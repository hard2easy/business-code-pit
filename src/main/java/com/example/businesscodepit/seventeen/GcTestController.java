package com.example.businesscodepit.seventeen;

import com.example.businesscodepit.seventeen.bean.User;
import com.example.businesscodepit.seventeen.bean.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/14
 * 修改时间：
 *      一是，我们的程序确实需要超出 JVM 配置的内存上限的内存。
 *          不管是程序实现的不合理，还是因为各种框架对数据的重复处理、加工和转换，
 *          相同的数据在内存中不一定只占用一份空间。针对内存量使用超大的业务逻辑，
 *          比如缓存逻辑、文件上传下载和导出逻辑，我们在做容量评估时，
 *          可能还需要实际做一下 Dump，而不是进行简单的假设。
 *      二是，出现内存泄露，其实就是我们认为没有用的对象最终会被 GC，但却没有。
 *          GC 并不会回收强引用对象，我们可能经常在程序中定义一些容器作为缓存，
 *          但如果容器中的数据无限增长，要特别小心最终会导致 OOM。
 *          使用 WeakHashMap 是解决这个问题的好办法，但值得注意的是，
 *          如果强引用的 Value 有引用 Key，也无法回收 Entry。
 *      三是，不合理的资源需求配置，在业务量小的时候可能不会出现问题，
 *          但业务量一大可能很快就会撑爆内存。
 *          比如，随意配置 Tomcat 的 max-http-header-size 参数(申请不必要的内存空间)，
 *          会导致一个请求使用过多的内存，请求量大的时候出现 OOM。
 *          在进行参数配置的时候，我们要认识到，很多限制类参数限制的是背后资源的使用，
 *          资源始终是有限的，需要根据实际需求来合理设置参数。
 *      最后我想说的是，在出现 OOM 之后，也不用过于紧张。我们可以根据错误日志中的异常信息，
 *          再结合 jstat 等命令行工具观察内存使用情况，以及程序的 GC 日志，
 *          来大致定位出现 OOM 的内存区块和类型。其实，我们遇到的 90% 的 OOM 都是堆 OOM，
 *          对 JVM 进程进行堆内存 Dump，或使用 jmap 命令分析对象内存占用排行，
 *          一般都可以很容易定位到问题。
 *      这里，我建议你为生产系统的程序配置 JVM 参数启用详细的 GC 日志，方便观察垃圾收集器的行为，
 *      并开启 HeapDumpOnOutOfMemoryError，以便在出现 OOM 时能自动 Dump 留下第一问题现场。对于 JDK8，你可以这么设置：
 *          XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=. -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Xloggc:gc.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M
 * @author yaoyong
 **/
@Slf4j
@RestController
@RequestMapping("/oom")
public class GcTestController {
    private Map<User, UserProfile> cache = new WeakHashMap<>();
    /**
     * 场景:
     *     系统缓存所有用户信息到内存中,现新加一个功能 实现用户名搜索时自动补全
     *     实现方法:针对每一个用户,将用户名进行分割,分割结果作为key 用户信息作为value
     *     但是使用不当 导致同一个用户其value有很多个,为共用一个 导致大量内存被消耗
     *
     *      举例:
     *          用户名abc 用户为user,在map中存储为
     *           a    new User(user的信息)
     *           ab   new User(user的信息)
     *           abc  new User(user的信息)
     */
    public void test1(){

    }

    /**
     *
     * 垃圾回收器不会回收有强引用的对象；
     * 在内存充足时，垃圾回收器不会回收具有软引用的对象；
     * 垃圾回收器只要扫描到了具有弱引用的对象就会回收(WeakHashMap 就是利用了这个特点)
     *
     * 使用 WeakHashMap 不等于不会 OOM
     *      WeakHashMap 的特点是 Key 在哈希表内部是弱引用的，
     *      当没有强引用指向这个 Key 之后，Entry 会被 GC，
     *      即使我们无限往 WeakHashMap 加入数据，只要 Key 不再使用，也就不会 OOM。
     *  一下案例中value->UserProfile 持有key-user的引用 导致无法被回收
     *      1.new WeakReference(new UserProfile(user, "location" + i)))
     *      2.cache.put(user, new UserProfile(new User(user.getName()), "location" + i)); });}
     */
    @GetMapping("/testWeakHashMap")
    public void testWeakHashMap(){
        String userName = "zhuye";
        //间隔1秒定时输出缓存中的条目数
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> log.info("cache size:{}", cache.size()), 1, 1, TimeUnit.SECONDS);
        LongStream.rangeClosed(1, 2000000).forEach(i -> {
            User user = new User(userName + i);
            cache.put(user, new UserProfile(user, "location" + i));
        });
    }

    /**
     * tomcat配置导致的oom
     */
    public void test3(){

    }

}
