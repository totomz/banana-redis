package org.bananarama.crud.redis.entities;

import org.bananarama.annotation.Banana;
import org.bananarama.crud.redis.RedisAdapterImpl;

/**
 * 
 * @author Tommaso Doninelli
 */
@Banana(adapter = RedisAdapterImpl.class)
public class DigitalOcean extends Host{

    private String token;

    public DigitalOcean(String id) {
        super(id);
    }

    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }

}
