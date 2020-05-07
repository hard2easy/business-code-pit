package com.example.businesscodepit.four;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


/**
 * 描述：
 * <p>
 * 创建时间：2020/03/14
 * 修改时间：
 *     查看redis.png类图
 *     BinaryClient 封装了各种 Redis 命令，其最终会调用基类 Connection 的方法，
 *     使用 Protocol 类发送命令。看一下 Protocol 类的 sendCommand 方法的源码，
 *     可以发现其发送命令时是直接操作 RedisOutputStream 写入字节。
 *     我们在多线程环境下复用 Jedis 对象，其实就是在复用 RedisOutputStream。
 *     如果多个线程在执行操作，那么既无法确保整条命令以一个原子操作写入 Socket，
 *     也无法确保写入后、读取前没有其他数据写到远端：
 *     如果将Jedis对象复用
 *          比如，写操作互相干扰，多条命令相互穿插的话，必然不是合法的 Redis 命令，
 *              那么 Redis 会关闭客户端连接，导致连接断开；
 *          又比如，线程 1 和 2 先后写入了 get a 和 get b 操作的请求，
 *              Redis 也返回了值 1 和 2，但是线程 2 先读取了数据 1 就会出现数据错乱的问题。
 *
 * @author yaoyong
 **/
public class RedisConnectPoolTest {
    private static Logger log = LoggerFactory.getLogger(RedisConnectPoolTest.class);
    private static JedisPool jedisPool = new JedisPool(new GenericObjectPoolConfig(),"121.36.221.72", 6379,5,"yy07093010");

    /**
     * 看一下JedisPool.getResource 与  Jedis关闭逻辑
     * JedisPool是连接池和连接分离
     * redisTemplate的实现是内置连接池
     */
    public static void main(){
        new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                for (int i = 0; i < 1000; i++) {
                    String result = jedis.get("a");
                    if (result.equals("1")) {
                        log.warn("Expect a to be 1 but found {}", result);
                        return;
                    }
                }
            }
        }).start();
        new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                for (int i = 0; i < 1000; i++) {
                    String result = jedis.get("b");
                    if (result.equals("2")) {
                        log.warn("Expect b to be 2 but found {}", result);
                        return;
                    }
                }
            }
        }).start();
    }
    @Test
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            jedisPool.close();
        }));
    }
}
