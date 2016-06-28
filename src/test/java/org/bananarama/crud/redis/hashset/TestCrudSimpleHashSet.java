package org.bananarama.crud.redis.hashset;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bananarama.BananaRama;
import org.bananarama.crud.redis.Utils;
import org.bananarama.crud.redis.entities.DigitalOcean;
import org.bananarama.crud.redis.entities.GoogleHost;
import org.bananarama.crud.redis.entities.Host;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;

/**
 *
 * @author Tommaso Doninelli
 */
public class TestCrudSimpleHashSet {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestCrudSimpleHashSet.class);

    private final GoogleHost google = Utils.generateGoogleHost("google-01");
    private final DigitalOcean ocean = Utils.generateDigitalOceanHost("ocean-820");
    
    private Jedis jedis;
    private BananaRama banana;
    
    @Before
    public void startup() {
        jedis = new Jedis();
        banana = new BananaRama();
    }
    
    @After
    public void shutdown() {
        jedis.close();
        banana = new BananaRama();
    }
    
//    @Test
    public void testCreate() throws Exception {
        
        // CREATE
        banana.create(GoogleHost.class).from(Stream.of(google));
        banana.create(DigitalOcean.class).from(Stream.of(ocean));

        // The key namespace is defined in the parent class (Host)
        Assert.assertEquals(2, jedis.scan("0", new ScanParams().match("host:*")).getResult().size());

        jedis.del("host:google-01", "host:ocean-820");
        Assert.assertEquals(0, jedis.scan("0", new ScanParams().match("host:*")).getResult().size());

        banana.create(Host.class).from(Stream.of(google, ocean));
        Assert.assertEquals(2, jedis.scan("0", new ScanParams().match("host:*")).getResult().size());

        jedis.del("host:google-01", "host:ocean-820");
        Assert.assertEquals(0, jedis.scan("0", new ScanParams().match("host:*")).getResult().size());

    }
    
    @Test
    public void testDelete() {
        
        jedis.hset("host:google-01", "aFieldThatDoesNotExists", "nonono");
        jedis.hset("host:ocean-820", "anotherFieldThatDoesNotExists", "nonono");
        Assert.assertEquals(2, jedis.scan("0", new ScanParams().match("host:*")).getResult().size());

        // DELETE
        banana.delete(GoogleHost.class).from(Stream.of(google));
        banana.delete(DigitalOcean.class).from(Stream.of(ocean));            
        Assert.assertEquals(0, jedis.scan("0", new ScanParams().match("host:*")).getResult().size());

        banana.create(Host.class).from(Stream.of(google, ocean));
        Assert.assertEquals(2, jedis.scan("0", new ScanParams().match("host:*")).getResult().size());

        banana.delete(Host.class).from(Stream.of(google, ocean));
        Assert.assertEquals(0, jedis.scan("0", new ScanParams().match("host:*")).getResult().size());     
    }
    
    @Test    
    public void testRead() {
        
        jedis.scan("0", new ScanParams().match("host:*")).getResult().forEach(jedis::del);
        
        Stream.of("www.pippo.com", "google-01", "uno.due.tre.jack:8080")
                .forEach(hostname -> {
                    String redisKey = "host:" + hostname;
                    log.info("Testing " + hostname);

                    jedis.hset(redisKey, "aFieldThatDoesNotExists", "nonono");
                    jedis.hset(redisKey, "class", GoogleHost.class.getCanonicalName());
                    jedis.hset(redisKey, "commonProperty", "lorem impsum");
                    jedis.hset(redisKey, "sparse", "4230.423");
                    jedis.hset(redisKey, "credentialFile", "8923y7 9ryfh9 dshfvp9asdh vpz \\xc3\\xa8");
                    jedis.hset(redisKey, "ttl", "742389.7589234");

                    Assert.assertEquals(0, banana.read(GoogleHost.class).fromKeys(Arrays.asList(hostname + "_wrong")).count());

                    List<GoogleHost> hosts = banana.read(GoogleHost.class).fromKeys(Arrays.asList(hostname)).collect(Collectors.toList());
 
                    Assert.assertNotNull(hosts);
                    Assert.assertEquals(1, hosts.size());

                    GoogleHost host = hosts.get(0);
                    Assert.assertEquals("lorem impsum", host.getCommonProperty());
                    Assert.assertEquals(4230.423, host.getSparse(), 0.0001);
                    Assert.assertEquals("lorem impsum", host.getCommonProperty());
                    Assert.assertEquals("8923y7 9ryfh9 dshfvp9asdh vpz \\xc3\\xa8", host.getCredentialFile());
                    Assert.assertEquals(hostname, host.getHostname());

                    jedis.del(redisKey);
                });
            
    }
    
    @Test    
    public void testReadInheritance() {
        
        jedis.scan("0", new ScanParams().match("host:*")).getResult().forEach(jedis::del);
        
        // If we put 2 object with the same parent class
        List<String> all = Arrays.asList(GoogleHost.class.getCanonicalName(), DigitalOcean.class.getCanonicalName());
        
        all.stream()
                .forEach(hostname -> {
                    String redisKey = "host:" + hostname;

                    jedis.hset(redisKey, "aFieldThatDoesNotExists", "nonono");
                    jedis.hset(redisKey, "class", hostname);
                    jedis.hset(redisKey, "commonProperty", "lorem impsum");
                    jedis.hset(redisKey, "sparse", "4230.423");
                    jedis.hset(redisKey, "credentialFile", "8923y7 9ryfh9 dshfvp9asdh vpz \\xc3\\xa8");
                    jedis.hset(redisKey, "ttl", "742389.7589234");
                });
        
        // If we request the base class, we expect to have all the instances
        Assert.assertEquals(2, banana.read(Host.class).fromKeys(all).count());
        
        // But when we request a specific type, we must filter out instance that does not match
        Assert.assertEquals(1, banana.read(GoogleHost.class).fromKeys(all).count());
        Assert.assertEquals(1, banana.read(DigitalOcean.class).fromKeys(all).count());
        
        // Cleanup
        all.stream().map(s -> "host:" + s).forEach(jedis::del);
    }

}
