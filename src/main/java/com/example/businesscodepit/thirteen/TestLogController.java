package com.example.businesscodepit.thirteen;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/12
 * 修改时间：
 *
 * @author yaoyong
 **/

@Log4j2
@RequestMapping("logging")
@RestController
public class TestLogController {
    @GetMapping("log")
    public void log() {
        log.debug("debug");
        log.info("info");
        log.warn("warn");
        log.error("error");
    }

    /**
     * 记录日志文件性能测试
     * @param count
     */
    @GetMapping("performance")
    public void performance(@RequestParam(name = "count", defaultValue = "1000") int count) {
        long begin = System.currentTimeMillis();
        String payload = IntStream.rangeClosed(1, 1000000)
                .mapToObj(__ -> "a")
                .collect(Collectors.joining("")) + UUID.randomUUID().toString();
        IntStream.rangeClosed(1, count).forEach(i -> log.info("{} {}", i, payload));
        Marker timeMarker = MarkerFactory.getMarker("time");
//        SLF4J注解才支持 log.info(timeMarker, "took {} ms", System.currentTimeMillis() - begin);
    }

    @GetMapping("manylog")
    public void manylog(@RequestParam(name = "count", defaultValue = "1000") int count) {
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, count).forEach(i -> log.info("log-{}", i));
        System.out.println("took " + (System.currentTimeMillis() - begin) + " ms");
    }

    /**
     * 使用日志占位符就不需要进行日志级别判断了？
     *     拼接字符串方式记录 slowString；
     *     使用占位符方式记录 slowString；
     *     先判断日志级别是否启用 DEBUG
     *     lambda 表达式进行延迟参数内容
     * 使用占位符方式记录 slowString 的方式，同样需要耗时 1 秒，
     *      是因为这种方式虽然允许我们传入 Object，不用拼接字符串，
     *      但也只是延迟（如果日志不记录那么就是省去）了日志参数对象.toString()
     *      和字符串拼接的耗时
     * @param count
     */
    @GetMapping("testZhanWeifu")
    public void testZhanWeifu(@RequestParam(name = "count", defaultValue = "1000") int count) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("debug1");
        //字符拼接 会执行slowString()方法 尽管日志级别是info
        log.debug("debug1:" + slowString("debug1"));
        stopWatch.stop();
        stopWatch.start("debug2");
        //占位符  会执行slowString()方法 尽管日志级别是info
        log.debug("debug2:{}", slowString("debug2"));
        stopWatch.stop();
        stopWatch.start("debug3");
        //判断日志级别
        if (log.isDebugEnabled())
            log.debug("debug3:{}", slowString("debug3"));
        stopWatch.stop();
        stopWatch.start("debug4");
        log.debug("debug4:{}", ()->slowString("debug4"));
        stopWatch.stop();
    }

    private String slowString(String s) {
        System.out.println("slowString called via " + s);
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
        }
        return "OK";
    }
}
