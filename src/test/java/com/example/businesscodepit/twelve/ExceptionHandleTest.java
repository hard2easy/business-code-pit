package com.example.businesscodepit.twelve;

import com.example.businesscodepit.bean.Order;
import jodd.util.concurrent.ThreadFactoryBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/11
 * 修改时间：
 *      不在业务代码层面考虑异常处理，仅在框架层面粗犷捕获和处理异常
 *      捕获了异常后直接生吞
 *      丢弃异常的原始信息
 *      抛出异常时不指定任何消息
 *          throw new RuntimeException();
 *
 *      对于自定义的业务异常，以 Warn 级别的日志记录异常以及当前 URL、执行方法等信息后，
 *          提取异常中的错误码和消息等信息，转换为合适的 API 包装体返回给 API 调用方；
 *      对于无法处理的系统异常，以 Error 级别的日志记录异常和上下文信息（比如 URL、参数、用户 ID）后，
 *          转换为普适的“服务器忙，请稍后再试”异常信息，同样以 API 包装体返回给调用方
 *
 *      查看 JDK JceSecurity 类 setupJurisdictionPolicies 方法源码，
 *          发现异常 e 没有记录，也没有作为新抛出异常的 cause，
 *          当时读取文件具体出现什么异常（权限问题又或是 IO 问题）可能永远都无法知道了，
 *          对问题定位造成了很大困扰
 *
 *      如果你捕获了异常打算处理的话，除了通过日志正确记录异常原始信息外，
 *      通常还有三种处理模式：
 *          转换，即转换新的异常抛出。对于新抛出的异常，最好具有特定的分类和明确的异常消息，
 *              而不是随便抛一个无关或没有任何信息的异常，并最好通过 cause 关联老异常。
 *          重试，即重试之前的操作。比如远程调用服务端过载超时的情况，
 *              盲目重试会让问题更严重，需要考虑当前情况是否适合重试。
 *          恢复，即尝试进行降级处理，或使用默认值来替代原始数据
 * @author yaoyong
 **/
public class ExceptionHandleTest {
    private static Logger log= LoggerFactory.getLogger(ExceptionHandleTest.class);

    /**
     * 吞掉错误信息
     */
    @Test
    public void wrong1(){
        try {
            readFile();
        } catch (IOException e) {

        }
    }

    /**
     * 丢失错误原始信息
     */
    @Test
    public void wrong3(){
        try {
            readFile();
        } catch (IOException e) {
            //原始异常信息丢失
            throw new RuntimeException("系统忙请稍后再试");
        }
    }

    private void readFile() throws IOException {
        Files.readAllLines(Paths.get("a_file"));
    }

    /**
     * 丢失错误原始信息
     */
    @Test
    public void wrong32(){
        try {
            readFile();
        } catch (IOException e) {
            //只保留了异常消息，栈没有记录
            log.error("文件读取错误, {}", e.getMessage());
            throw new RuntimeException("系统忙请稍后再试");
        }
    }
    @Test
    public void right(){
        try {
            readFile();
        } catch (IOException e) {
            log.error("文件读取错误", e);
            throw new RuntimeException("系统忙请稍后再试");
            //throw new RuntimeException("系统忙请稍后再试", e);
        }
    }

    /**
     * 小心 finally 中的异常
     * 虽然 try 中的逻辑出现了异常，但却被 finally 中的异常覆盖了。
     *      这是非常危险的，特别是 finally 中出现的异常是偶发的，
     *      就会在部分时候覆盖 try 中的异常，让问题更不明显
     * 原因也很简单，因为一个方法无法出现两个异常
     */
    @Test
    public void test4(){
        try {
            log.info("try");
            //异常丢失
            throw new RuntimeException("try");
        } finally {
            log.info("finally");
            throw new RuntimeException("finally");
        }
    }
    @Test
    public void test41(){
        try {
            log.info("try");
            //异常丢失
            throw new RuntimeException("try");
        } finally {
            log.info("finally");
            try {
                throw new RuntimeException("finally");
            } catch (Exception ex) {
                log.error("finally", ex);
            }
        }
    }

    /**
     * 把 try 中的异常作为主异常抛出，使用 addSuppressed 方法把 finally 中的异常附加到主异常上
     *
     * 其实这正是 try-with-resources 语句的做法：
     *      对于实现了 AutoCloseable 接口的资源，建议使用 try-with-resources 来释放资源，
     *      否则也可能会产生刚才提到的，释放资源时出现的异常覆盖主异常的问题
     */
    @Test
    public void test42() throws Exception{
        Exception e = null;
        try {
            log.info("try");
            throw new RuntimeException("try");
        } catch (Exception ex) {
            e = ex;
        } finally {
            log.info("finally");
            try {
                throw new RuntimeException("finally");
            } catch (Exception ex) {
                if (e!= null) {
                    e.addSuppressed(ex);
                } else {
                    e = ex;
                }
            }
        }
        throw e;
    }

    /**
     * 对于实现了 AutoCloseable 接口的资源，建议使用 try-with-resources
     * 否则也会出现异常覆盖的问题
     * @throws Exception
     */
    @Test
    public void useresourcewrong() throws Exception {
        TestResource testResource = new TestResource();
        try {
            testResource.read();
        } finally {
            testResource.close();
        }
    }

    /**
     * 使用try-with-resources 会发现其异常打印日志与test42()方法一样
     * @throws Exception
     */
    @Test
    public void useresourceright() throws Exception {
        try (TestResource testResource = new TestResource()){
            testResource.read();
        }
    }

    /**
     * 将异常定义为静态变量  进行统一管理
     *  如果设置为静态变量 会导致栈信息的错乱
     * 00:22:07.209 [main] ERROR com.example.businesscodepit.twelve.ExceptionHandleTest - createOrder got error
     *  com.example.businesscodepit.twelve.handle.BusinessException: 订单已存在
     * 	at com.example.businesscodepit.twelve.Exceptions.<clinit>(Exceptions.java:14)
     * 	at com.example.businesscodepit.twelve.ExceptionHandleTest.createOrderWrong(ExceptionHandleTest.java:207)
     * 	at com.example.businesscodepit.twelve.ExceptionHandleTest.wrongStaticException(ExceptionHandleTest.java:194)


     * 00:22:07.229 [main] ERROR com.example.businesscodepit.twelve.ExceptionHandleTest - cancelOrder got error
     * com.example.businesscodepit.twelve.handle.BusinessException: 订单已存在
     * 	at com.example.businesscodepit.twelve.Exceptions.<clinit>(Exceptions.java:14)
     * 	at com.example.businesscodepit.twelve.ExceptionHandleTest.createOrderWrong(ExceptionHandleTest.java:207)
     * 	at com.example.businesscodepit.twelve.ExceptionHandleTest.wrongStaticException
     *
     * 根据上面的报错日志可以看到  cancelOrderWrong抛出的异常实际是createOrderWrong的异常信息
     * */
    @Test
    public void wrongStaticException() {
        try {
            createOrderWrong();
        } catch (Exception ex) {
            log.error("createOrder got error", ex);
        }
        try {
            cancelOrderWrong();
        } catch (Exception ex) {
            log.error("cancelOrder got error", ex);
        }
    }

    private void createOrderWrong() {
        //这里有问题  替换成 Exceptions.getException();
        throw Exceptions.ORDEREXISTS;
    }

    private void cancelOrderWrong() {
        //这里有问题  替换成 Exceptions.getException();
        throw Exceptions.ORDEREXISTS;
    }

    /**
     * 使用execute 执行线程池中的任务. 如果该任务执行报异常,那么执行该任务的
     * 线程会被重新创建
     *    因此如果线程池中的任务可能频繁报错,需要定义异常处理机制,防止屏藩创建线程 耗费资源
     *
     *    因为没有手动捕获异常进行处理，ThreadGroup 帮我们进行了未捕获异常的默认处理，
     *    向标准错误输出打印了出现异常的线程名称和异常信息。
     *    显然，这种没有以统一的错误日志格式记录错误信息打印出来的形式，
     *    对生产级代码是不合适的
     * @throws InterruptedException
     */
    @Test
    public void testThreadPoolExecute() throws InterruptedException {
        String prefix = "test";
        ExecutorService threadPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat(prefix+"%d").get());
        //提交10个任务到线程池处理，第5个任务会抛出运行时异常
        IntStream.rangeClosed(1, 10).forEach(i -> threadPool.execute(() -> {
            if (i == 5) throw new RuntimeException("error");
            log.info("I'm done : {}", i);
        }));
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);

        // 定义ExecutorService 设置异常处理方法
        ExecutorService threadPoolRight = Executors.newFixedThreadPool(1,new ThreadFactoryBuilder()
                .setNameFormat(prefix+"%d")
                .setUncaughtExceptionHandler((thread, throwable)-> log.error("ThreadPool {} got exception", thread, throwable))
                .get());
    }

    /**
     * 线程不会被重新创建  但是异常信息会被吞掉
     * 查看 FutureTask 源码可以发现，在执行任务出现异常之后，
     *      异常存到了一个 outcome 字段中，
     *      只有在调用 get 方法获取 FutureTask 结果的时候，
     *      才会以 ExecutionException 的形式重新抛出异常：
     * @throws InterruptedException
     */
    @Test
    public void testThreadPoolSubmit() throws InterruptedException {
        String prefix = "test";
        ExecutorService threadPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat(prefix+"%d").get());
        //提交10个任务到线程池处理，第5个任务会抛出运行时异常
        IntStream.rangeClosed(1, 10).forEach(i -> threadPool.submit(() -> {
            if (i == 5) throw new RuntimeException("error");
            log.info("I'm done : {}", i);
        }));
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);


        //正确使用方法  通过List<Future>记录执行结果  遍历执行get方法
        List<Future> tasks = IntStream.rangeClosed(1, 10).mapToObj(i -> threadPool.submit(() -> {
            if (i == 5) throw new RuntimeException("error");
            log.info("I'm done : {}", i);
        })).collect(Collectors.toList());

        tasks.forEach(task-> {
            try {
                task.get();
            } catch (Exception e) {
                log.error("Got exception", e);
            }
        });
    }

    /**
     * 测试finally代码块的执行
     *    finally中的代码块其实是复制到try和catch中的return和throw之前的方式来处理的
     */
    @Test
    public void testFinally(){
        System.out.println(testFinally1());
        System.out.println(testFinally2());
        System.out.println(testFinally3());
        System.out.println(testFinally4());

    }

    /**
     * 1  int基本数据类型  不是引用类型,因此在finally修改了i的值 也不会发生变化
     * @return
     */
    public static int testFinally1 () {
        int i = 1;
        try {
            return i;
        }finally {
            ++i;
        }
    }

    /**
     * 2  return 覆盖
     * @return
     */
    public static int testFinally2 () {
        int i = 1;
        try {
            return i;
        }finally {
            return ++i;
        }
    }

    /**
     * 引用类型,返回的就是order
     * 1 zhangsan
     * @return
     */
    public static Order testFinally3 () {
        Order order = new Order();
        order.setId(1l);
        try {
            return order;
        }finally {
            order.setCustomerName("zhangsan");
        }
    }

    /**
     * finally重新给order = new Order() 重新只想一片内存地址
     * 但是实际上在try里面返回的是当时order指向的内存地址所对应的数据,因此重新设置order引用并不会对
     * 返回值有影响
     * 返回的是new Order()
     * @return
     */
    public static Order testFinally4 () {
        Order order = new Order();
        try {
            return order;
        } finally {
            order = new Order();
            order.setId(8l);
        }
    }
}
