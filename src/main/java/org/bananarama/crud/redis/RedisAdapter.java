package org.bananarama.crud.redis;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.bananarama.crud.Adapter;
import org.bananarama.crud.CreateOperation;
import org.bananarama.crud.DeleteOperation;
import org.bananarama.crud.ReadOperation;
import org.bananarama.crud.UpdateOperation;
import org.bananarama.crud.redis.annotations.RedisKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.cqengine.query.option.QueryOptions;

import javaslang.control.Either;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * Simple adapter for CRUD operations of objects saved as HashSet in a redis db.
 *
 * Each instance must have an unique identifier, and getter/setter methods
 * annotated with {@link KeyGenerator} that are used to convert the instance id
 * in a custom redis key; eg the class Host has a unique id the string
 * "www.web.com", and the equivalent redis key is in the form
 * "host:www.web.com:key"
 *
 * This class is abstract; the implementations must provide the necessary
 * connection string to the redis instance
 *
 * @author Tommaso Doninelli
 */
public abstract class RedisAdapter implements Adapter<Object> {

    private static final Logger log = LoggerFactory.getLogger(RedisAdapter.class);

    // toValue = castmap(toClass).apply(fromValue)
    private static final HashMap<String, Function<String, Object>> castStringMap = new HashMap<>();
    private static final ConcurrentHashMap<Class<?>, InstanceKey> keyGeneratorsMap = new ConcurrentHashMap<>();

    static {
        castStringMap.put("java.lang.Double", (s) -> {
            return Double.parseDouble(s);
        });
        castStringMap.put("double", (s) -> {
            return Double.parseDouble(s);
        });
        castStringMap.put("java.lang.String", (s) -> {
            return s;
        });
        castStringMap.put("java.time.ZonedDateTime", (s) -> {
            return ZonedDateTime.parse(s);
        });
    }

    /**
     * Convert a {@link Map<String, String>) to an object. The instance class
     * must be specified using the fqdn class name as value for the key class.
     *
     * @param <T>
     * @param map
     * @return
     */
    @SuppressWarnings("unchecked")
    protected static <T> Either<Exception, T> mapToObject(Map<String, String> map) {

        // XXX A functional way could be better; a future improvement
//        Object obj = 
//                Try.of(() -> {return Class.forName(map.get("class"));})
//                .mapTry( (clazz) -> {return clazz.newInstance();}).onFailure(x -> {throw new Exception("Probably an empty default construtor is missing?", x);})
//                .get();
        
        try {
            Class<?> clazz = Class.forName(map.get("class"));
            InstanceKey key = getKey(clazz);        // The field to use to parse the redis key
            Object obj = clazz.newInstance();

            BeanInfo info = Introspector.getBeanInfo(clazz);
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {

                Method setter = pd.getWriteMethod();

                if (setter != null) {
                    Type t = setter.getGenericParameterTypes()[0];

                    if (!castStringMap.containsKey(t.getTypeName())) {
                        throw new RuntimeException("Missing mapping for objectType " + t.getTypeName());
                    }

                    Object value = castStringMap.get(t.getTypeName()).apply(map.get(pd.getName()));
                    if (value != null) {
                        setter.invoke(obj, value);
                    }
                }
            }
            
            // Set the id **bypassing the setter** (easier, and currently it seems what we need....)
            String value = extractKey(map.get("@key"), key.regex);
            boolean access = key.field.isAccessible();
            key.field.setAccessible(true);
            key.field.set(obj, value);
            key.field.setAccessible(access);

            // Set the id
            return Either.right((T) obj);
        }
        catch (InstantiationException e) {
            log.error("Can not instantiate " + map.get("class") + "; does it have a public noargs constructor?", e); // In this case we have to provide usefull hints to the user
            return Either.left(e);
        }
        catch (ClassNotFoundException | IllegalAccessException | IntrospectionException | RuntimeException | InvocationTargetException e) {
//            log.error(e.getMessage(), e); // No need to lo gerrors, since we can return them; the caller will ignore, or do whatever he wants
            return Either.left(e);
        }
    }
    

    /**
     * Convert an instance of T in a {@link Map<String, String>}. The instance
     * FQDN class is saved as value for the key "class". This method expects
     * that the class T has a field marked with {@link RedisKey}; the value of
     * this field, converted using the patterns specified by the annotation, is
     * stored using at the key "@id"
     *
     * @param <T>
     * @param t
     * @return
     */
    protected static <T> Either<Exception, Map<String, String>> objToMap(T t) {

        InstanceKey key = getKey(t.getClass());

        try {
            // We can't cache the BeanInfo becase the stream could be of different class with the same parent class
            Map<String, String> objectAsMap = new HashMap<>();
            BeanInfo info = Introspector.getBeanInfo(t.getClass());
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {

                Method reader = pd.getReadMethod();
                if (reader != null) {
                    objectAsMap.put(pd.getName(), reader.invoke(t).toString());
                }

            }

            // Remove the field that is the id from the map; 
            // this field is mapped as @key to be serialized as key. 
            // Intstead of doing n checks in the for-cycle, we just remove it from the hash once.            
            objectAsMap.remove(key.fieldName);

            // Set the "special" @id value as defined by the annotation
            // toString() is not the best but for now it is enough
            boolean access = key.field.isAccessible();
            key.field.setAccessible(true);
            objectAsMap.put("@key", key.regex.replace("$", key.field.get(t).toString()));
            key.field.setAccessible(access);

            return Either.right(objectAsMap);
        } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.warn("Error mapping an object to a map", e);
            return Either.left(e);
        }
    }

    protected static String extractKey(String fromString, String pattern) {

        int patternPos = pattern.indexOf('$');
        String leftPart = pattern.substring(0, patternPos);
        String rightPart = pattern.substring(patternPos + 1, pattern.length());

        String result = fromString.replaceFirst(leftPart, "").replaceFirst(rightPart, "");
        
//        System.out.println(String.format("[%s] - [%s] --> %s", pattern, fromString, result));

        return result;
    }
    
    private static InstanceKey getKey(Class<?> clazz) {
        if (!keyGeneratorsMap.containsKey(clazz)) {
            InstanceKey key = Stream.concat(
                    Arrays.stream(clazz.getDeclaredFields()), // Get the fields fot this class
                    Stream.of(clazz.getSuperclass()).filter(c -> c != null).map(c -> c.getDeclaredFields()).flatMap(Arrays::stream)) // And from the parents
                    
                .filter(f -> f.isAnnotationPresent(RedisKey.class))
                .map(field -> {return new InstanceKey(field, field.getName(), field.getAnnotation(RedisKey.class).value());})
                .findFirst().get();

            keyGeneratorsMap.put(clazz, key);
        }

        return keyGeneratorsMap.get(clazz);
    }
    
    /**
     * The provider for a connection. Implementation shall use this method to
     * return a pooled connection, using a custom connection string.
     *
     * @return a {@link Jedis} connection
     */
    protected abstract Jedis getJedis();

    @Override
    public <T> CreateOperation<T> create(Class<T> clazz) {

        return new CreateOperation<T>() {

            @Override
            public CreateOperation<T> from(Stream<T> data) {

                return from(data, null);
            }

            @Override
            public CreateOperation<T> from(Stream<T> data, QueryOptions options) {

                try (Jedis jedis = getJedis();
                        Pipeline pipeline = jedis.pipelined()) {

                    data.map(RedisAdapter::objToMap) // Convert to hashmap
                            .filter(Either::isRight) // throw away wrong stuff                         
                            .map(Either::get)
                            .forEach(mapObj -> {
                                String key = mapObj.get("@key");
                                mapObj.entrySet().stream()
                                        .filter(entry -> !entry.getKey().equals("@key"))
                                        .forEach(entry -> pipeline.hset(key, entry.getKey(), entry.getValue()));
                            });
                    pipeline.sync();
                } catch (Exception e) {
                    log.warn("Error persisting an element", e);
                }

                return this;
            }

            @Override
            public void close() throws IOException {
            }
        };

    }

    @Override
    public <T> ReadOperation<T> read(Class<T> clazz) {
        
        Class<T> type = clazz;
        
        return new ReadOperation<T>() {
            @Override
            public Stream<T> all() {
                return all(null);
            }

            @Override
            public Stream<T> all(QueryOptions options) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public <Q> Stream<T> where(Q whereClause) {
                return where(whereClause, null);
            }

            @Override
            public <Q> Stream<T> where(Q whereClause, QueryOptions options) {
                throw new UnsupportedOperationException("Select with where is not supported"); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Stream<T> fromKeys(List<?> keys) {
                return fromKeys(keys, null);
            }

            @Override
            @SuppressWarnings("unchecked")
            public Stream<T> fromKeys(List<?> keys, QueryOptions options) {
                
                try (Jedis jedis = getJedis()) {

                    return (Stream<T>) keys.stream()
                            .map(Object::toString)
//                            .map(s ->{
//                                System.out.println(":::"+s);
//                                return s;
//                            })
                            .map(s -> {return getKey(type).regex.replace("$", s);})
                            .map(key -> {
                                Map<String, String> map = jedis.hgetAll(key);
                                map.put("@key", key);
                                return map;
                            })
                            .filter(map -> {return !map.isEmpty();})                            
                            .map(RedisAdapter::mapToObject)
                            .filter(Either::isRight)                    // Returning only valid results, but where are we handling the errors?                            
                            .map(Either::get)
                            .filter(type::isInstance)                   // Return only types that match the required class
                            ;

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

                return Stream.empty();
            }

            @Override
            public void close() throws IOException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    @Override
    public <T> UpdateOperation<T> update(Class<T> clazz) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> DeleteOperation<T> delete(Class<T> clazz) {
        return new DeleteOperation<T>() {
            @Override
            public <Q> DeleteOperation<T> where(Q whereClaus) {
                return where(whereClaus, null);
            }

            @Override
            public <Q> DeleteOperation<T> where(Q whereClaus, QueryOptions options) {
                throw new UnsupportedOperationException("Delte with where clause is not supported.");
            }

            @Override
            public DeleteOperation<T> from(Stream<T> data) {
                return from(data, null);
            }

            @Override
            public DeleteOperation<T> from(Stream<T> data, QueryOptions options) {
                try (Jedis jedis = getJedis();
                        Pipeline pipeline = jedis.pipelined()) {

                    data.map(RedisAdapter::objToMap) // Convert to hashmap
                            .filter(Either::isRight) // throw away wrong stuff                         
                            .map(Either::get)
                            .map(map -> map.get("@key"))
                            .forEach(pipeline::del);

                    pipeline.sync();
                } catch (Exception e) {
                    log.warn("Error deleting an element", e);
                }
                return this;
            }

            @Override
            public void close() throws IOException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

}

class InstanceKey {

    protected Field field;
    protected String fieldName;
    protected String regex;

    public InstanceKey(Field field, String fieldName, String regex) {
        this.field = field;
        this.fieldName = fieldName;
        this.regex = regex;
    }

}
