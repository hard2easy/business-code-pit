package com.example.businesscodepit.three;

import jodd.util.concurrent.ThreadFactoryBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 描述：
 * <p>
 * 创建时间：2020/03/14
 * 修改时间：
 *     1  我们需要根据自己的场景、并发情况来评估线程池的几个核心参数，
 *        包括核心线程数、最大线程数、线程回收策略、工作队列的类型，以及拒绝策略，
 *        确保线程池的工作行为符合需求，一般都需要设置有界的工作队列和可控的线程数。
 *     2  任何时候，都应该为自定义线程池指定有意义的名称，以方便排查问题。
 *        当出现线程数量暴增、线程死锁、线程占用大量 CPU、线程执行出现异常等问题时，
 *        我们往往会抓取线程栈。此时，有意义的线程名称，就可以方便我们定位问题
 * @author yaoyong
 **/
public class ThreadPoolTest {
    private Logger log = LoggerFactory.getLogger(ThreadPoolTest.class);
    /**
     * newFixedThreadPool   只有一个参数:执行线程数  队列默认采取的是无限制的LinkedBlockingQueue
     * newCachedThreadPool  来一个请求就创建一个线程执行任务
     * 以上两种方式都可能会导致oom
     * @throws InterruptedException
     */
    public void oom1() throws InterruptedException {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        //打印线程池的信息，稍后我会解释这段代码
        printStats(threadPool);
        for (int i = 0; i < 100000000; i++) {
            threadPool.execute(() -> {
                String payload = IntStream.rangeClosed(1, 1000000)
                        .mapToObj(__ -> "a")
                        .collect(Collectors.joining("")) + UUID.randomUUID().toString();
                try {
                    TimeUnit.HOURS.sleep(1);
                } catch (InterruptedException e) {
                }
                log.info(payload);
            });
        }

        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);
    }

    private void printStats(ThreadPoolExecutor threadPool) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            log.info("=========================");
            log.info("Pool Size: {}", threadPool.getPoolSize());
            log.info("Active Threads: {}", threadPool.getActiveCount());
            log.info("Number of Tasks Completed: {}", threadPool.getCompletedTaskCount());
            log.info("Number of Tasks in Queue: {}", threadPool.getQueue().size());
            log.info("=========================");
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 正确创建线程池的方式
     * 不会初始化corePoolSize个线程，有任务来了才创建工作线程；
     * 当核心线程满了之后不会立即扩容线程池，而是把任务堆积到工作队列中；
     * 当工作队列满了后扩容线程池，一直到线程个数达到maximumPoolSize为止；
     * 如果队列已满且达到了最大线程后还有任务进来，按照拒绝策略处理；
     * 当线程数大于核心线程数时，线程等待 keepAliveTime 后还是没有任务需要处理的话，
     * 收缩线程到核心线程数。
     *     声明线程池后立即调用 prestartAllCoreThreads 方法，来启动所有核心线程；
     *     传入 true 给 allowCoreThreadTimeOut 方法，来让线程池在空闲的时候同样回收核心线程。
     * 我们有没有办法让线程池更激进一点，优先开启更多的线程，而把队列当成一个后备方案呢？
     * @return
     * @throws InterruptedException
     */
    @Test
    public void right() throws InterruptedException {
        //使用一个计数器跟踪完成的任务数
        AtomicInteger atomicInteger = new AtomicInteger();
        //创建一个具有2个核心线程、5个最大线程，使用容量为10的ArrayBlockingQueue阻塞队列作为工作队列的线程池，使用默认的AbortPolicy拒绝策略
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                2, 5,
                5, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadFactoryBuilder().setNameFormat("demo-threadpool-%d").get(),
                new ThreadPoolExecutor.AbortPolicy());
        printStats(threadPool);
        threadPool.prestartAllCoreThreads();
        threadPool.allowCoreThreadTimeOut(true);
        submitTasks(atomicInteger, threadPool);
        TimeUnit.SECONDS.sleep(60);
        System.out.println(atomicInteger.intValue());
    }

    private void submitTasks(AtomicInteger atomicInteger, ThreadPoolExecutor threadPool) {
        //每隔1秒提交一次，一共提交20次任务
        IntStream.rangeClosed(1, 20).forEach(i -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int id = atomicInteger.incrementAndGet();
            try {
                threadPool.submit(() -> {
                    log.info("{} started", id);
                    //每个任务耗时10秒
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                    }
                    log.info("{} finished", id);
                });
            } catch (Exception ex) {
                //提交出现异常的话，打印出错信息并为计数器减一
                log.error("error submitting task {}", id, ex);
                atomicInteger.decrementAndGet();
            }
        });
    }

    /**
     * 激进的线程: 先创建更多的线程再将任务放到队列中
     * 实现:
     *     原来是创建线程达到核心线程数就往队列中放,然后再创建到最大线程,如果
     *     还有就按照配置的拒绝策略进行执行
     * @throws InterruptedException
     */
    @Test
    public void test2() throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger();
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10) {
            @Override
            public boolean offer(Runnable e) {
                //营造队列已满假象
                return false;
            }
        };
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                2, 5,
                5, TimeUnit.SECONDS,
                queue, new ThreadFactoryBuilder().setNameFormat("elastic-pool").get(), (r, executor) -> {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        threadPool.allowCoreThreadTimeOut(true);
        printStats(threadPool);
        submitTasks(atomicInteger, threadPool);
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);
    }
}
