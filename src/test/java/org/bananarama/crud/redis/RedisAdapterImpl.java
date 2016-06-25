package org.bananarama.crud.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Simple implementation that returns a connection from the default pool
 * @author Tommaso Doninelli
 */
public class RedisAdapterImpl extends RedisAdapter{

    private static JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");

    @Override
    protected Jedis getJedis() {
        return pool.getResource();
    }

}
