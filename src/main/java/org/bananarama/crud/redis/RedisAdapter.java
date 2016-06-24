package org.bananarama.crud.redis;

import com.googlecode.cqengine.query.option.QueryOptions;

import javaslang.Tuple2;

import java.beans.BeanInfo;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.bananarama.BananaRama;
import org.bananarama.crud.Adapter;
import org.bananarama.crud.CreateOperation;
import org.bananarama.crud.DeleteOperation;
import org.bananarama.crud.ReadOperation;
import org.bananarama.crud.UpdateOperation;
import org.bananarama.crud.redis.annotations.KeyGenerator;
import org.bananarama.crud.redis.annotations.RedisKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * Simple adapter for CRUD operations of objects saved as HashSet in a redis db. 
 * 
 * Each instance must have an unique identifier, and getter/setter methods annotated with {@link KeyGenerator} that are used to convert the
 * instance id in a custom redis key; eg the class Host has a unique id the string "www.web.com", and the equivalent redis key is in the form "host:www.web.com:key"
 * 
 * This class is abstract; the implementations must provide the necessary connection string to the redis instance
 * @author Tommaso Doninelli
 */
public abstract class RedisAdapter implements Adapter<Object> {

    private static final Logger log = LoggerFactory.getLogger(RedisAdapter.class);

    
    // toValue = castmap(toClass).apply(fromValue)
    private static final HashMap<String, Function<String, Object>> castStringMap = new HashMap<>();

    static {
        castStringMap.put("java.lang.Double", (s) -> {return Double.parseDouble(s);});
        castStringMap.put("double", (s) -> {return Double.parseDouble(s);});
        castStringMap.put("java.lang.String", (s) -> {return s;});
        castStringMap.put("java.time.ZonedDateTime", (s) -> {return ZonedDateTime.parse(s);});
    }

    @SuppressWarnings("unchecked")
    protected static <T> Optional<T> mapToObject(Map<String, String> map) {
    	
        try {
            Class clazz = Class.forName(map.get("class"));
            Object obj = clazz.newInstance();
          
            Tuple2<String, Field> key =
	            Arrays.stream(clazz.getDeclaredFields())
	            .filter(f -> {return f.isAnnotationPresent(RedisKey.class);})
	            .map(field -> {return new Tuple2<String, Field>(field.getDeclaredAnnotation(RedisKey.class).value(), field);})
	            .findFirst()
	            .get();
            
            
            BeanInfo info = Introspector.getBeanInfo(clazz);
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {

                Method setter = pd.getWriteMethod();
                String property = pd.getName();         // This is the name of the property represented by the getter/setter! not the field, not the method name!

                if (setter != null) {

                    if ( map.containsKey("@key") && setter.isAnnotationPresent(KeyGenerator.class)) {
                    	
                    	0.
                    	
                    	key._2.set(setter, keyValue);
                    	
                        setter.invoke(obj, map.get("@key"));
                    } 
                    else {
                        Type t = setter.getGenericParameterTypes()[0];
                        
                        if(!castStringMap.containsKey(t.getTypeName())) {
                        	throw new RuntimeException("Missing mapping for objectType " + t.getTypeName());
                        }
                        
                        Object value = castStringMap.get(t.getTypeName()).apply(map.get(pd.getName()));
                        if(value != null) {
                            setter.invoke(obj, value);
                        }
                    }

                }
            }

            // Set the id
            return Optional.of((T)obj);
        } 
        catch (Exception e) {
            log.warn(e.getMessage(), e);
            return Optional.empty();
        }
    }

    protected static <T> Optional<HashMap<String, String>> objToMap(T t) {

        try {
            HashMap<String, String> objectAsMap = new HashMap<>();
            BeanInfo info = Introspector.getBeanInfo(t.getClass());
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {

                Method reader = pd.getReadMethod();
                if (reader != null) {
                    String pname = reader.isAnnotationPresent(KeyGenerator.class) ? "@key" : pd.getName();
                    objectAsMap.put(pname, reader.invoke(t).toString());
                }
                
            }
            return Optional.of(objectAsMap);
        } 
        catch (Exception e) {
            log.warn("Error mapping an object to a map", e);
            return Optional.empty();
        }
    }

    /**
     * The provider for a connection. 
     * Implementation shall use this method to return a pooled connection, using a custom connection string.
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
                
                try(Jedis jedis = getJedis();
                        Pipeline pipeline = jedis.pipelined()){
                    
                    data.map(RedisAdapter::objToMap) // Convert to hashmap
                        .filter(Optional::isPresent) // throw away wrong stuff 
                        .map(Optional::get)
                        .forEach(mapObj -> {                            
                            String key = mapObj.get("@key");
                            mapObj.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equals("@key") )
                                    .forEach( entry -> pipeline.hset(key, entry.getKey(), entry.getValue()) );                            
                        });
                    pipeline.sync();
                }
                catch(Exception e) {
                    log.warn("Error persisting an element", e);
                }
                
                return this;
            }

            @Override
            public void close() throws IOException {}
        };

    }

    @Override
    public <T> ReadOperation<T> read(Class<T> clazz) {
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
                try(Jedis jedis = getJedis()){
                      	
                    return (Stream<T>) keys.stream()
                    .map(Object::toString)
            		.map(jedis::hgetAll)
                    .filter(map -> {return !map.isEmpty();})
                    .map(RedisAdapter::mapToObject)
                    .filter(Optional::isPresent)
                    .map(Optional::get);
                    
                }
                catch(Exception e) {
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
                try(Jedis jedis = getJedis();
                        Pipeline pipeline = jedis.pipelined()){
                    
                    data.map(RedisAdapter::objToMap) // Convert to hashmap
                        .filter(Optional::isPresent) // throw away wrong stuff 
                        .map(Optional::get)
                        .map(map -> map.get("@key"))
                        .forEach(pipeline::del);
                    
                    pipeline.sync();
                }
                catch(Exception e) {
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

