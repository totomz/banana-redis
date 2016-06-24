package org.bananarama.crud.redis.entities;

import org.bananarama.annotation.Banana;
import org.bananarama.crud.redis.RedisAdapterImpl;
import org.bananarama.crud.redis.annotations.RedisKey;

@Banana(adapter = RedisAdapterImpl.class)
public class Host {
    
    // This field contains the id of the instance. No need to annotat ethis field,
    // in Redis we create the key using other methods
	@RedisKey("host:$")
    private String hostname;
            
    private String commonProperty;

    public Host() {
    }

    public Host(String hostname) {
        this.hostname = hostname;
    }

    public String getCommonProperty() {
        return commonProperty;
    }

    public void setCommonProperty(String commonProperty) {
        this.commonProperty = commonProperty;
    }

//    // This annotation identify the method to use to convert the instance id to 
//    // the string to use as key in redis
//    @KeyGenerator
//    public String getHostnameKey() {
//        return "host:" + hostname;
//    }
//    
//    // We have to annotate both the setter and the getter. 
//    @KeyGenerator
//    public void setHostnameKey(String key) {
//        this.hostname = key.split(":")[1];            
//    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
//    
//    public static List<String> keys(String...name) {
//    	return Arrays.stream(name)
//    			.map(s -> "host:" + s)
//    			.collect(Collectors.toList());
//	}
}
