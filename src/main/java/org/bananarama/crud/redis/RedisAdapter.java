package org.bananarama.crud.redis;

import com.googlecode.cqengine.query.option.QueryOptions;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javaslang.control.Either;
import javaslang.control.Try;
import org.bananarama.crud.Adapter;
import org.bananarama.crud.CreateOperation;
import org.bananarama.crud.DeleteOperation;
import org.bananarama.crud.ReadOperation;
import org.bananarama.crud.UpdateOperation;
import org.bananarama.crud.redis.annotations.KeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 *
 * @author Tommaso Doninelli
 */
public class RedisAdapter implements Adapter<Object> {

    private static final Logger log = LoggerFactory.getLogger(RedisAdapter.class);

    // toValue = castmap(toClass).apply(fromValue)
    private static final HashMap<String, Function<String, Object>> castStringMap = new HashMap<>();

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
    }

    public static Either<Exception, Object> mapToObject(HashMap<String, String> map) {

        try {
            Class clazz = Class.forName(map.get("class"));
            Object obj = clazz.newInstance();

            BeanInfo info = Introspector.getBeanInfo(clazz);
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {

                Method setter = pd.getWriteMethod();
                String property = pd.getName();         // This is the name of the property represented by the getter/setter! not the field, not the method name!

                if (setter != null) {

                    if (setter.isAnnotationPresent(KeyGenerator.class)) {
                        setter.invoke(obj, map.get("@key"));
                    } 
                    else {
                        Type t = setter.getGenericParameterTypes()[0];
                        Object value = castStringMap.get(t.getTypeName()).apply(map.get(pd.getName()));
                        if(value != null) {
                            setter.invoke(obj, value);
                        }
                    }
                }
            }

            // Set the id
            return Either.right(obj);
        } 
        catch (Exception e) {
            log.warn("Can not deserialize map to object", e);
            return Either.left(e);
        }
    }

    public static Either<Exception, HashMap<String, String>> objToMap(Object t) {

        try {
            HashMap<String, String> objectAsMap = new HashMap<>();
            BeanInfo info = Introspector.getBeanInfo(t.getClass());
//            System.out.println("::::::::::::::::::::::::::::::::::::");
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {

                Method reader = pd.getReadMethod();
                if (reader != null) {
                    String pname = reader.isAnnotationPresent(KeyGenerator.class) ? "@key" : pd.getName();
//                    System.out.println(pname);

                    objectAsMap.put(pname, reader.invoke(t).toString());
                }
            }
//            System.out.println("::::::::::::::::::::::::::::::::::::");
            return Either.right(objectAsMap);
        } 
        catch (Exception e) {
            log.warn("Error mapping an object to a map", e);
            return Either.left(e);
        }
    }

    private Jedis getJedis(){
        return new Jedis();
    }
    
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
                        .filter(Either::isRight) // throw away wrong stuff 
                        .map(Either::get)
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
            public Stream<T> fromKeys(List<?> keys, QueryOptions options) {
                try(Jedis jedis = getJedis();
                        Pipeline pipe = jedis.pipelined()) {
                    
                    keys.stream().map(k -> {
                        Arrays.stream(k.getClass().getMethods())
                                .filter((m) -> {return m.isAnnotationPresent(KeyGenerator.class);})
                                .findFirst()
                                .orElseThrow(new RuntimeException("Annotation " + KeyGenerator.class + " not found for class " + k.toString() ));
                                
                        return null;
                    });
                    
                }
                catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
                        .filter(Either::isRight) // throw away wrong stuff 
                        .map(Either::get)
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
