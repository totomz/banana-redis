package org.bananarama.redis.entities;

import org.bananarama.annotation.Banana;
import org.bananarama.crud.redis.RedisAdapter;
import org.bananarama.crud.redis.annotations.KeyGenerator;

@Banana(adapter = RedisAdapter.class)
public class Host {
    
    // This field contains the id of the instance. No need to annotat ethis field,
    // in Redis we create the key using other methods
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

    // This annotation identify the method to use to convert the instance id to 
    // the string to use as key in redis
    @KeyGenerator
    public String getHostnameKey() {
        return "host:" + hostname;
    }
    
    // We have to annotate both the setter and the getter. 
    @KeyGenerator
    public void setHostnameKey(String key) {
        this.hostname = key.split(":")[1];            
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    
    
}
