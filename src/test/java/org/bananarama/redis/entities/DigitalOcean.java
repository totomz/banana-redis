package org.bananarama.redis.entities;

import org.bananarama.annotation.Banana;
import org.bananarama.crud.redis.RedisAdapter;

/**
 * 
 * @author Tommaso Doninelli
 */
@Banana(adapter = RedisAdapter.class)
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
