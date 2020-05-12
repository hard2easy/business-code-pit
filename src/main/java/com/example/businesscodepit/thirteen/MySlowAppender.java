package com.example.businesscodepit.thirteen;

import ch.qos.logback.core.ConsoleAppender;

import java.util.concurrent.TimeUnit;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/12
 * 修改时间：
 *
 * @author yaoyong
 **/

public class MySlowAppender extends ConsoleAppender {
    @Override
    protected void subAppend(Object event) {
        try {
            // 模拟慢日志
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.subAppend(event);
    }
}
