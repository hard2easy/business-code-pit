package com.example.businesscodepit.four;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 描述：
 * <p>
 * 创建时间：2020/03/15
 * 修改时间：
 *
 * @author yaoyong
 **/
public class TestRedis {
    public static void main(String[] args) {
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(32);
        JedisPool jsJedisPool=new JedisPool(jedisPoolConfig,"121.36.221.72", 6379,5000,"yy07093010");
        Jedis jedis=jsJedisPool.getResource();
        System.out.println(jedis.get("firstword"));
    }
}
