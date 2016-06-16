package org.bananarama.redis;

import java.util.HashMap;
import org.bananarama.crud.redis.RedisAdapter;
import static org.bananarama.redis.Utils.GOOGLE_TTL;
import org.bananarama.redis.entities.DigitalOcean;
import org.bananarama.redis.entities.GoogleHost;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.Jedis;

/**
 * 
 * @author Tommaso Doninelli
 */
public class TestObjectToMap {
    
    @Test
    public void fromObjectToMap() throws Exception {
        
        HashMap<String, String> map = RedisAdapter
                .objToMap(Utils.generateGoogleHost("ciaone"))
                .getOrElseThrow(t -> t);
        
//        map.forEach((k, v) -> {System.out.println(k + "--> " + v);});
        
        Assert.assertEquals("host:ciaone", map.get("@key"));        // This is the key in redis
        Assert.assertEquals(Utils.GOOGLE_COMMONPROPVALUE, map.get("commonProperty"));
        Assert.assertEquals(Utils.GOOGLE_CREFILEVALUE, map.get("credentialFile"));
        Assert.assertEquals(Double.toString(Utils.GOOGLE_TTL), map.get("ttl"));
        
        // Test the id agains a different object
        HashMap<String, String> map2 = RedisAdapter
                .objToMap(Utils.generateDigitalOceanHost("digiciccio"))
                .getOrElseThrow(t -> t);
        
        Assert.assertEquals("host:digiciccio", map2.get("@key"));
    }

    @Test
    public void fromMapToObject() throws Exception {
        
        HashMap<String, String> map = new HashMap<>();
        map.put("@key", "host:antonello");
        map.put("commonProperty", "this is sparta!");
        map.put("ttl", "156.8792398437");
        map.put("credentialFile", "this is the content of a file: )");
        map.put("sparse", "27.85");
        map.put("class", "org.bananarama.redis.entities.GoogleHost");
        
        GoogleHost host = (GoogleHost)RedisAdapter.mapToObject(map).getOrElseThrow(t -> t);
        
        Assert.assertNotNull(host);
        Assert.assertEquals("antonello", host.getHostname());
        Assert.assertEquals("this is sparta!", host.getCommonProperty());
        Assert.assertEquals(156.8792398437, host.getTtl(),0.001);
        Assert.assertEquals("this is the content of a file: )", host.getCredentialFile());
        Assert.assertEquals(27.85, host.getSparse(),0.001);        
        
    }
}