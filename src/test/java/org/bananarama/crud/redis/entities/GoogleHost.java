package org.bananarama.crud.redis.entities;

import java.util.List;

import org.bananarama.annotation.Banana;
import org.bananarama.crud.redis.RedisAdapterImpl;
import org.bananarama.crud.redis.annotations.KeyGenerator;

/**
 * 
 * @author Tommaso Doninelli
 */
@Banana(adapter = RedisAdapterImpl.class)
public class GoogleHost  extends Host {

    private String contents;
    private Double ttl = 983.84;
    private double sparse = 4230.423;
    
    public GoogleHost() {}
    
    public GoogleHost(String id) {
        super(id);
    }

    public String getCredentialFile() {
        return contents;
    }
        
    public void setCredentialFile(String contents) {
        this.contents = contents;
    }

    public double getTtl() {
        return ttl;
    }

    public void setTtl(double ttl) {
        this.ttl = ttl;
    }

    public double getSparse() {
        return sparse;
    }

    public void setSparse(double sparse) {
        this.sparse = sparse;
    }

    
}