package com.example.businesscodepit.httpinvoke;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/05
 * 修改时间：
 *    与执行本地方法不同，进行 HTTP 调用本质上是通过 HTTP 协议进行一次网络请求。
 *    网络请求必然有超时的可能性，因此我们必须考虑到这三点：
 *      首先，框架设置的默认超时是否合理；
 *      其次，考虑到网络的不稳定，超时后的请求重试是一个不错的选择，
 *          但需要考虑服务端接口的幂等性设计是否允许我们重试；
 *     最后，需要考虑框架是否会像浏览器那样限制并发连接数，以免在服务并发很大的情况下，
 *          HTTP 调用的并发数限制成为瓶颈。
 *     对于 HTTP 调用，虽然应用层走的是 HTTP 协议，但网络层面始终是 TCP/IP 协议。
 *     TCP/IP 是面向连接的协议，在传输数据之前需要建立连接。
 *     几乎所有的网络框架都会提供这么两个超时参数：
 *          连接超时参数 ConnectTimeout，让用户配置建连阶段的最长等待时间； 理论上不操作三秒
 *          读取超时参数 ReadTimeout，用来控制从 Socket 上读取数据的最长等待时间
 *     为什么不存在写超时参数
 *          请求时,写操作相当于本地操作,只要可以连接上就不会出现超时的情况
 *     springcloud  feign、ribbon
 *          默认情况下 Feign 的读取超时是 1 秒，如此短的读取超时算是坑点一
 *          如果要配置 Feign 的读取超时，就必须同时配置连接超时，才能生效
 *          单独的超时可以覆盖全局超时，这符合预期，不算坑
 *          除了可以配置 Feign，也可以配置 Ribbon 组件的参数来修改两个超时时间。
 *              这里的坑点三是，参数首字母要大写，和 Feign 的配置不同
 *          同时配置 Feign 和 Ribbon 的超时，以 Feign 为准
 *              原来里面逻辑为 如果Feign的超时参数进行了设置那么直接忽略Ribbon配置的超时
 *          Ribbon会自动重试一次(MaxAutoRetriesNextServer)
 *              对于GET请求,如果访问到故障请求  ribbon会进行自动重试,因此接口设计时需要按照实际
 *              业务类型定义接口的请求类型
 *          PoolingHttpClientConnectionManager
 *              defaultMaxPerRoute    也就是同时访问同一个主机 / 域名的最大并发请求数
 *  *           maxTotal 所有主机整体最大并发为 20，这也是 HttpClient 整体的并发度
 *              HttpClients.custom().setConnectionManager(new PoolingHttpClientConnectionManager())
 *                  查看构造方法的源码可以发现 里面自动将两个值设置为 2 20
 *              HttpClients.custom().setMaxConnPerRoute(10).setMaxConnTotal(20).build();
 *                  自定义defaultMaxPerRoute以及maxTotal
 * * @author yaoyong
 **/
public class HttpInvokeTest {
}
