package com.example.businesscodepit.addsnack;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * 描述：
 * <p>
 * 创建时间：2020/03/23
 * 修改时间：
 *
 * @author yaoyong
 **/
public class TestParallel {
    /**
     * 一键把 Stream 转换为并行操作提交到线程池处理
     * rangeClosed
     */
    @Test
    public void test1(){
        IntStream.rangeClosed(1,3).parallel().forEach(i->{
            System.out.println(LocalDateTime.now() + " : " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
        });
        IntStream.range(1,3).parallel().forEach(i->{
            System.out.println(LocalDateTime.now() + " : " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
        });
    }

    /**
     * 手动分割任务  等任务量分割到不同的线程
     * 第一种方式是使用线程。直接把任务按照线程数均匀分割，分配到不同的线程执行，
     * 使用 CountDownLatch 来阻塞主线程，直到所有线程都完成操作。
     * 这种方式，需要我们自己分割任务
     * @param taskCount
     * @param threadCount
     * @return
     * @throws InterruptedException
     */
    private int thread(int taskCount, int threadCount) throws InterruptedException {
        //总操作次数计数器
        AtomicInteger atomicInteger = new AtomicInteger();
        //使用CountDownLatch来等待所有线程执行完成  threadCount线程数
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        //使用IntStream把数字直接转为Thread
        IntStream.rangeClosed(1, threadCount).mapToObj(i -> new Thread(() -> {
            //手动把taskCount分成taskCount份，每一份有一个线程执行
            IntStream.rangeClosed(1, taskCount / threadCount).forEach(j -> increment(atomicInteger));
            //每一个线程处理完成自己那部分数据之后，countDown一次
            countDownLatch.countDown();  //CountDownLatch(count)  count-1
        })).forEach(Thread::start);
        //等到所有线程执行完成
        countDownLatch.await(); //等待CountDownLatch(count) count为0
        //查询计数器当前值
        return atomicInteger.get();
    }

    /**
     * 使用 Executors.newFixedThreadPool 来获得固定线程数的线程池，使用 execute 提交所有任务到线程池执行，
     * 最后关闭线程池等待所有任务执行完成
     * @param taskCount
     * @param threadCount
     * @return
     * @throws InterruptedException
     */
    private int threadpool(int taskCount, int threadCount) throws InterruptedException {
        //总操作次数计数器
        AtomicInteger atomicInteger = new AtomicInteger();
        //初始化一个线程数量=threadCount的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        //所有任务直接提交到线程池处理
        IntStream.rangeClosed(1, taskCount).forEach(i -> executorService.execute(() -> increment(atomicInteger)));
        //提交关闭线程池申请，等待之前所有任务执行完成
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        //查询计数器当前值
        return atomicInteger.get();
    }
    /**
     * ForkJoinPool 和传统的 ThreadPoolExecutor 区别在于:
     *      前者对于 n 并行度有 n 个独立队列，后者是共享队列。
     *      如果有大量执行耗时比较短的任务，ThreadPoolExecutor 的单队列就可能会成为瓶颈。
     *      这时，使用 ForkJoinPool 性能会更好。因此，ForkJoinPool 更适合大任务分割成许多小任务并行执行的场景，
     *      而 ThreadPoolExecutor 适合许多独立任务并发执行的场景
     *      IntStream.rangeClosed(1, taskCount).parallel()默认是使用通用的ForkJoinPool.commonPool()
     *      但是如果IntStream.rangeClosed(1, taskCount).parallel()操作本身就放在
     *      forkJoinPool.execute()那么使用的就是forkJoinPool而不是通用的
     *      可以查看ForkJoinTask.fork()方法
     * @param taskCount
     * @param threadCount
     * @return
     * @throws InterruptedException
     */
    private int forkjoin(int taskCount, int threadCount) throws InterruptedException {
        //总操作次数计数器
        AtomicInteger atomicInteger = new AtomicInteger();
        //自定义一个并行度=threadCount的ForkJoinPool
        ForkJoinPool forkJoinPool = new ForkJoinPool(threadCount);
        //所有任务直接提交到线程池处理
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, taskCount).parallel().forEach(i -> {
            increment(atomicInteger);
            System.out.println("notOrder-----" + i);
        }));
        //提交关闭线程池申请，等待之前所有任务执行完成
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        //查询计数器当前值
        return atomicInteger.get();
    }
    /**
     * 测试forEach与forEachOrder
     *      forEachOrder失去parallel并行操作特性 会使得IntStream.rangeClosed(1, taskCount)
     *      按照顺序串行
     * @param taskCount
     * @param threadCount
     * @return
     * @throws InterruptedException
     */
    private int forkjoinOrder(int taskCount, int threadCount) throws InterruptedException {
        //总操作次数计数器
        AtomicInteger atomicInteger = new AtomicInteger();
        //自定义一个并行度=threadCount的ForkJoinPool
        ForkJoinPool forkJoinPool = new ForkJoinPool(threadCount);
        //所有任务直接提交到线程池处理
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, taskCount).parallel().forEachOrdered(i -> {
            increment(atomicInteger);
            System.out.println("order-----" + i);
        }));
        //提交关闭线程池申请，等待之前所有任务执行完成
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        //查询计数器当前值
        return atomicInteger.get();
    }
    /**
     * 直接使用并行流，并行流使用公共的 ForkJoinPool，也就是 ForkJoinPool.commonPool()。
     * 公共的 ForkJoinPool 默认的并行度是 CPU 核心数 -1，原因是对于 CPU 绑定的任务分配超过 CPU 个数的线程没有意义。
     * 由于并行流还会使用主线程执行任务，也会占用一个 CPU 核心，所以公共 ForkJoinPool 的并行度即使 -1 也能用满所有 CPU 核心。
     * 这里，我们通过配置强制指定（增大）了并行数，但因为使用的是公共 ForkJoinPool，所以可能会存在干扰，
     * 你可以回顾下第 3 讲有关线程池混用产生的问题
     * @param taskCount
     * @param threadCount
     * @return
     */
    private int stream(int taskCount, int threadCount) {
        //设置公共ForkJoinPool的并行度
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(threadCount));
        //总操作次数计数器
        AtomicInteger atomicInteger = new AtomicInteger();
        //由于我们设置了公共ForkJoinPool的并行度，直接使用parallel提交任务即可
        IntStream.rangeClosed(1, taskCount).parallel().forEach(i -> increment(atomicInteger));
        //查询计数器当前值
        return atomicInteger.get();
    }

    /**
     * 第五种方式是，使用 CompletableFuture 来实现。CompletableFuture.runAsync
     * 方法可以指定一个线程池，一般会在使用 CompletableFuture 的时候用到
     * @param taskCount
     * @param threadCount
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private int completableFuture(int taskCount, int threadCount) throws InterruptedException, ExecutionException {
        //总操作次数计数器
        AtomicInteger atomicInteger = new AtomicInteger();
        //自定义一个并行度=threadCount的ForkJoinPool
        ForkJoinPool forkJoinPool = new ForkJoinPool(threadCount);
        //使用CompletableFuture.runAsync通过指定线程池异步执行任务
        CompletableFuture.runAsync(() -> IntStream.rangeClosed(1, taskCount).parallel().forEach(i -> increment(atomicInteger)), forkJoinPool).get();
        //查询计数器当前值
        return atomicInteger.get();
    }

    private void increment(AtomicInteger atomicInteger) {
        atomicInteger.incrementAndGet();
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 如果你的程序对性能要求特别敏感，建议通过性能测试根据场景决定适合的模式。
     * 一般而言，使用线程池（第二种）和直接使用并行流（第四种）的方式在业务代码中比较常用。
     * 但需要注意的是，我们通常会重用线程池，而不会像 Demo 中那样在业务逻辑中直接声明新的线程池，
     * 等操作完成后再关闭
     *
     * 另外需要注意的是，在上面的例子中我们一定是先运行 stream 方法再运行 forkjoin 方法，
     * 对公共 ForkJoinPool 默认并行度的修改才能生效。
     * 这是因为 ForkJoinPool 类初始化公共线程池是在静态代码块里，加载类时就会进行的，
     * 如果 forkjoin 方法中先使用了 ForkJoinPool，即便 stream 方法中设置了系统属性也不会起作用。
     * 因此我的建议是，设置 ForkJoinPool 公共线程池默认并行度的操作，应该放在应用启动时设置
     * @throws Exception
     */
    @Test
    public void testMethod() throws Exception {
        System.out.println(thread(20,5));
        System.out.println(threadpool(20,5));
        System.out.println(forkjoin(20,5));
        System.out.println(forkjoinOrder(20,5));
        System.out.println(stream(20,5));
        System.out.println(completableFuture(20,5));
    }
}
