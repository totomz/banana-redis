package org.bananarama.redis.hashset;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bananarama.BananaRama;
import org.bananarama.redis.Utils;
import org.bananarama.redis.entities.GoogleHost;
import org.bananarama.redis.entities.DigitalOcean;
import org.bananarama.redis.entities.Host;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

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
    
//    @Test
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
        
        /*
        127.0.0.1:6379> HGETALL host:ocean-820
        1) "commonProperty"
        2) "ocean-common"
        3) "hostname"
        4) "ocean-820"
        5) "class"
        6) "class org.bananarama.redis.entities.DigitalOcean"
        7) "token"
        8) "08f97sa0f9yds908gb"

        */
        jedis.scan("0", new ScanParams().match("host:*")).getResult().forEach(jedis::del);
        
        jedis.hset("host:google-01", "aFieldThatDoesNotExists", "nonono");
        jedis.hset("host:google-01", "class", "org.bananarama.redis.entities.GoogleHost");        
        jedis.hset("host:google-01", "commonProperty", "lorem impsum");
        jedis.hset("host:google-01", "sparse", "4230.423");
        jedis.hset("host:google-01", "hostname", "google-01");
        jedis.hset("host:google-01", "credentialFile", "8923y7 9ryfh9 dshfvp9asdh vpz \\xc3\\xa8");
        jedis.hset("host:google-01", "ttl", "742389.7589234");
        
        List<GoogleHost> hosts = banana.read(GoogleHost.class).fromKeys(Collections.singletonList("google-01")).collect(Collectors.toList());
        
        Assert.assertNull(hosts);
        Assert.assertEquals(1, hosts.size());
        
        banana.read(GoogleHost.class).all().forEach(System.out::println);
            
    }

}
