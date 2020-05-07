package com.example.businesscodepit.four;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 描述：HTTP 连接池
 * <p>
 * 创建时间：2020/03/14
 * 修改时间：
 *
 * @author yaoyong
 **/
public class HttpConnectPoolTest {
    /**
     * 在使用三方客户端进行网络通信时，我们首先要确定客户端SDK是否是基于连接池技术实现的。
     * 我们知道，TCP是面向连接的基于字节流的协议：
     *   面向连接: 意味着连接需要先创建再使用，创建连接的三次握手有一定开销；
     *   基于字节流: 意味着字节是发送数据的最小单元，TCP 协议本身无法区分哪几个字节是完整的消息体，
     *      也无法感知是否有多个客户端在使用同一个 TCP 连接，TCP 只是一个读写数据的管道。
     *      如果客户端 SDK 没有使用连接池，而直接是 TCP 连接，那么就需要考虑每次建立 TCP 连接的开销，
     *      并且因为 TCP 基于字节流，在多线程的情况下对同一连接进行复用，可能会产生线程安全问题。
     *      我们先看一下涉及 TCP 连接的客户端 SDK，对外提供 API 的三种方式。
     *      在面对各种三方客户端的时候，只有先识别出其属于哪一种，才能理清楚使用方式。
     * 连接池和连接分离的 API：
     *    有一个 XXXPool 类负责连接池实现，先从其获得连接 XXXConnection，
     *    然后用获得的连接进行服务端请求，完成后使用者需要归还连接。
     *    通常，XXXPool 是线程安全的，可以并发获取和归还连接，
     *    而 XXXConnection 是非线程安全的。对应到连接池的结构示意图中，
     *    XXXPool 就是右边连接池那个框，左边的客户端是我们自己的代码。
     * 内部带有连接池的 API：
     *    对外提供一个 XXXClient 类，通过这个类可以直接进行服务端请求；
     *    这个类内部维护了连接池，SDK 使用者无需考虑连接的获取和归还问题。
     *    一般而言，XXXClient 是线程安全的。对应到连接池的结构示意图中，
     *    整个 API 就是蓝色框包裹的部分。
     * 非连接池的 API：
     *    一般命名为 XXXConnection，以区分其是基于连接池还是单连接的，
     *    而不建议命名为 XXXClient 或直接是 XXX。直接连接方式的API基于单一连接，
     *    每次使用都需要创建和断开连接，性能一般，且通常不是线程安全的。
     *    对应到连接池的结构示意图中，这种形式相当于没有右边连接池那个框，
     *    客户端直接连接服务端创建连接。虽然上面提到了 SDK 一般的命名习惯，
     *    但不排除有一些客户端特立独行，因此在使用三方 SDK 时，一定要先查看官方文档了解其最佳实践，
     *    或是在类似 Stackoverflow 的网站搜索 XXX threadsafe/singleton 字样看看大家的回复，也可以一层一层往下看
     * 明确了 SDK 连接池的实现方式后，我们就大概知道了使用 SDK 的最佳实践：
     *     1 如果是分离方式，那么连接池本身一般是线程安全的，可以复用。
     *       每次使用需要从连接池获取连接，使用后归还，归还的工作由使用者负责。
     *     2 如果是内置连接时池，SDK 会负责连接的获取和归还，使用的候直接复用客户端。
     *     3 如果 SDK 没有实现连接池（大多数中间件、数据库的客户端 SDK 都会支持连接池），
     *       那通常不是线程安全的，而且短连接的方式性能不会很高，使用的时候需要考虑是否自己封装一个连接池。
     *
     *  连接池参数根据实际情况配置,例如:最大连接数
     *      在真实情况下，只要数据库可以承受，你可以选择在遇到连接超限的时候先设置一个足够大的连接数，
     *      然后观察最终应用的并发，再按照实际并发数留出一半的余量来设置最终的最大连接。
     *  对类似数据库连接池的重要资源进行持续检测(必须)，并设置一半的使用量作为报警阈值，出现预警后及时扩容
     *
     */

    /**
     * HttpClient是内部带有连接池
     * @return
     */

    @Test
    public String wrong1() {
        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .evictIdleConnections(60, TimeUnit.SECONDS).build();
        try (CloseableHttpResponse response = client.execute(new HttpGet("http://127.0.0.1:45678/httpclientnotreuse/test"))) {
            return EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static CloseableHttpClient httpClient = null;

    /**
     * Runtime.getRuntime().addShutdownHook关闭线程池
     */
    static {
        //当然，也可以把CloseableHttpClient定义为Bean，然后在@PreDestroy标记的方法内close这个HttpClient
        httpClient = HttpClients.custom().setMaxConnPerRoute(1).setMaxConnTotal(1).evictIdleConnections(60, TimeUnit.SECONDS).build();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                httpClient.close();
            } catch (IOException ignored) {
            }
        }));
    }

    /**
     * 将client定义为静态变量进行复用
     * @return
     */
    public String right() {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet("http://127.0.0.1:45678/httpclientnotreuse/test"))) {
            return EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * 执行的时候每次都创建新的client
     * @return
     */
    public String wrong2() {
        try (CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .evictIdleConnections(60, TimeUnit.SECONDS).build();
             CloseableHttpResponse response = client.execute(new HttpGet("http://127.0.0.1:45678/httpclientnotreuse/test"))) {
            return EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
