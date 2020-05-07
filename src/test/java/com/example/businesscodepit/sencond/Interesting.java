package com.example.businesscodepit.sencond;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * volatile保证了可见性。改完后会强制让工作内存失效。去主存拿
 */
public class Interesting {
    private Logger log = LoggerFactory.getLogger(Interesting.class);
    volatile int a = 1;
    volatile int b = 1;

    public void add() {
        log.info("add start");
        for (int i = 0; i < 10000; i++) {
            a++;
            b++;
        }
        log.info("add done");
    }

    public void compare() {
        log.info("compare start");
        for (int i = 0; i < 10000; i++) {
            //a始终等于b吗？
            if (a < b) {
                log.info("a:{},b:{},{}", a, b, a > b);
                //最后的a>b应该始终是false吗？
            }
        }
        log.info("compare done");
    }

    public static void main(String[] args) {
        /**
         * 希望的效果是a永远等于b 但是由于无法保证add()中的a++  b++一起的原子性
         * 此时就算给add方法加上synchronized也是无用的,因为成员变量synchronized是成员对象级的锁
         * 此时如果希望a永远等于b必须要add  compare都加上synchronized 保证同一成员变量的add compare方法无法同时执行
         */
        Interesting interesting = new Interesting();
        new Thread(() -> interesting.add()).start();
        new Thread(() -> interesting.compare()).start();
    }
}