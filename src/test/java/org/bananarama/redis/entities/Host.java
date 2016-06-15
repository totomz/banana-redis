package org.bananarama.redis.entities;

import org.bananarama.annotation.Banana;
import org.bananarama.crud.redis.RedisAdapter;
import org.bananarama.crud.redis.annotations.KeyGenerator;

/**
 * 
 * @author Tommaso Doninelli
 */
@Banana(adapter = RedisAdapter.class)
public class Host {
    
    // This will be the id
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

    @KeyGenerator
    public String getHostnameKey() {
        return "host:" + hostname;
    }
    
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
