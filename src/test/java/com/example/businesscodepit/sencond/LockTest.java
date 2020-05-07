package com.example.businesscodepit.sencond;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 描述：
 * <p>
 * 创建时间：2020/03/14
 * 修改时间：
 *      如果精细化考虑了锁应用范围后，性能还无法满足需求的话，我们就要考虑另一个维度的粒度问题了，
 *      即：区分读写场景以及资源的访问冲突，考虑使用悲观方式的锁还是乐观方式的锁。
 *          1 对于读写比例差异明显的场景，考虑使用 ReentrantReadWriteLock 细化区分读写锁，来提高性能
 *          2 如果你的 JDK 版本高于 1.8、共享资源的冲突概率也没那么大的话，考虑使用 StampedLock 的乐观读的特性，
 *          进一步提高性能
 *              ReadWriteLock 与  StampedLock
 *                  同样是实现多线程同时读，但只有一个线程能写的问题。
 *                  ReadWriteLock:
 *                      是悲观锁,如果有线程正在读，写线程需要等待读线程释放锁后才能获取写锁，即读的过程中不允许写，这是一种悲观的读锁。
 *                  StampedLock:
 *                      读的过程中也允许获取写锁后写入！这样一来，我们读的数据就可能不一致，
 *                      所以，需要一点额外的代码来判断读的过程中是否有写入，这种读锁是一种乐观锁。
 *                      // 获得一个乐观读锁
 *                      long stamp = stampedLock.tryOptimisticRead();
 *                      //进行业务操作
 *                      // 检查乐观读锁后是否有其他写锁发生
 *                      if (!stampedLock.validate(stamp)) {
 *                          // 获取一个悲观读锁，如果乐观读锁失效那么就获取悲观读锁
 *                          stamp = stampedLock.readLock();
 *                          //重新读取
 *                      }
 *          3 JDK 里 ReentrantLock 和 ReentrantReadWriteLock 都提供了公平锁的版本，
 *                 ReentrantLock synchronized ReentrantReadWriteLock ReadWriteLock
 *                 synchronized:锁在对象上,自动加锁以及解锁 不可响应中断
 *                 ReentrantLock:需要手动进行加锁以及解锁 可以响应中断
 *                 ReentrantReadWriteLock:基于ReentrantLock做的读写互斥而不是独享锁
 *          在没有明确需求的情况下不要轻易开启公平锁特性，在任务很轻的情况下开启公平锁可能会让性能下降上百倍。
 *                 公平锁特性:排队时间长的线程可以先获取到锁
 *                 响应中断: 响应中断就是一个线程获取不到锁，不会傻傻的一直等下去
 *                 可重入(ReentrantLock(显式锁、轻量级锁)和Synchronized (内置锁、重量级锁)): 某一线程已经获取到锁之后,再次获取同一个锁,此时就不需要进行竞争可以直接获取
 *                      意义:两个synchronized方法a  b,如果不具备可重入那么a b方法内部是不允许互相调用的,否则必死锁
 *                          哪怕只有一个线程执行
 * @author yaoyong
 **/
public class LockTest {
    private Logger log = LoggerFactory.getLogger(LockTest.class);
    private List<Integer> data = new ArrayList<>();

    /**
     * 加锁的时候需要判断好锁的级别以及锁的粒度  没有必要加锁的代码不要加锁
     */
    @Test
    public void test1(){
        wrong();
        right();
    }

    //不涉及共享资源的慢方法
    private void slow() {
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
        }
    }

    //错误的加锁方法
    public int wrong() {
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {
            //加锁粒度太粗了
            synchronized (this) {
                slow();
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
    }

    //正确的加锁方法
    public int right() {
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {
            slow();
            //只对List加锁
            synchronized (data) {
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
    }
}
