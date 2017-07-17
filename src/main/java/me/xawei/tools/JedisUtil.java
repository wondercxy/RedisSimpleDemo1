package me.xawei.tools;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by wei on 2017/7/17.
 */
public class JedisUtil {
    private static String JEDIS_IP;
    private static int JEDIS_PORT;
    private static String JEDIS_PASSWORD;
    //private static String JEDIS_SLAVE;

    private static JedisPool jedisPool;

    static {
        Configuration conf = Configuration.getInstance();
//        JEDIS_IP = conf.getString("jedis.ip", "127.0.0.1");
//        JEDIS_PORT = conf.getInt("jedis.port", 6379);

        JEDIS_IP = "127.0.0.1";
        JEDIS_PORT = 6379;
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxIdle(256);//20
        config.setMaxWaitMillis(5000L);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setMinEvictableIdleTimeMillis(60000l);
        config.setTimeBetweenEvictionRunsMillis(3000l);
        config.setNumTestsPerEvictionRun(-1);
        jedisPool = new JedisPool(config, JEDIS_IP, JEDIS_PORT, 60000);
    }

    public static long hsetnx(String key, String field, String value) {
        Jedis jedis = null;
        long res = -1L;
        try {
            jedis = jedisPool.getResource();
            res = jedis.hsetnx(key, field, value);
        } catch (Exception e) {
            //释放redis对象
            jedis.close();
            e.printStackTrace();
        } finally {
            //返还到连接池
            jedis.close();
        }
        return res;
    }
}
