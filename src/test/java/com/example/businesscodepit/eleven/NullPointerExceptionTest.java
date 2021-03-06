package com.example.businesscodepit.eleven;

import lombok.Getter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参数值是 Integer 等包装类型，使用时因为自动拆箱出现了空指针异常；
 * 字符串比较出现空指针异常；
 * 诸如 ConcurrentHashMap 这样的容器不支持 Key 和 Value 为 null，强行 put null 的 Key 或 Value 会出现空指针异常；
 * A 对象包含了 B，在通过 A 对象的字段获得 B 之后，没有对字段判空就级联调用 B 的方法出现空指针异常；
 * 方法或远程服务返回的 List 不是空而是 null，没有进行判空就直接调用 List 的方法出现空指针异常
 *
 * 小心 MySQL 中有关 NULL 的三个坑
 *      MySQL 中 sum 函数没统计到任何记录时，会返回 null 而不是 0
 *      MySQL 中 count 字段不统计 null 值,count(score)  如果满足where条件后的数据中score有为null的 那么不会进行统计
 *      MySQL 中使用诸如 =、<> 这样的算数比较操作符比较 NULL 的结果总是 NULL
 *          字段名 = null  字段名 ！= null 都查不出数据
 * MyBatis实现是否需要更新字段为空的属性值
 *      @Column 注解的updateIfNull属性，可以控制，当对应的列value为null时，updateIfNull的true和false可以控制
 * ConcurrentHashMap 的 Key 和 Value 都不能为 null，而 HashMap 却可以，你知道这么设计的原因是什么吗
 *
 *      ConcurrentHashmap和Hashtable都是支持并发的，这样会有一个问题，
 *          当你通过get(k)获取对应的value时，如果获取到的是null时，你无法判断，
 *          它是put（k,v）的时候value为null，还是这个key从来没有做过映射。
 *
 *      HashMap是非并发的，可以通过contains(key)来做这个判断。
 *      而支持并发的Map在调用m.contains（key）和m.get(key),m可能已经不同了。
 * arthas 阿里诊断工具
 */
public class NullPointerExceptionTest {
    private Logger log = LoggerFactory.getLogger(NullPointerExceptionTest.class);

    private List<String> wrongMethod(FooService fooService, Integer i, String s, String t) {
        log.info("result {} {} {} {}", i + 1, s.equals("OK"), s.equals(t),
                new ConcurrentHashMap<String, String>().put(null, null));
        if (fooService.getBarService().bar().equals("OK"))
            log.info("OK");
        return null;
    }

    @Test
    public void wrong() {
        String test = "1111";
        System.out.println(wrongMethod(test.charAt(0) == '1' ? null : new FooService(),
                test.charAt(1) == '1' ? null : 1,
                test.charAt(2) == '1' ? null : "OK",
                test.charAt(3) == '1' ? null : "OK").size());
    }

    class FooService {
        @Getter
        private BarService barService;

    }

    class BarService {
        String bar() {
            return "OK";
        }
    }

    /**
     * 对于 Integer 的判空，可以使用 Optional.ofNullable 来构造一个 Optional，然后使用 orElse(0) 把 null 替换为默认值再进行 +1 操作。
     * 对于 String 和字面量的比较，可以把字面量放在前面，比如"OK".equals(s)，这样即使 s 是 null 也不会出现空指针异常；
     *      而对于两个可能为 null 的字符串变量的 equals 比较，可以使用 Objects.equals，它会做判空处理。
     * 对于 ConcurrentHashMap，既然其 Key 和 Value 都不支持 null，修复方式就是不要把 null 存进去。HashMap 的 Key 和 Value 可以存入 null，
     *      而 ConcurrentHashMap 看似是 HashMap 的线程安全版本，却不支持 null 值的 Key 和 Value，这是容易产生误区的一个地方。
     * 对于类似 fooService.getBarService().bar().equals(“OK”) 的级联调用，需要判空的地方有很多，
     *      包括 fooService、getBarService() 方法的返回值，以及 bar 方法返回的字符串。如果使用 if-else 来判空的话可能需要好几行代码，
     *      但使用 Optional 的话一行代码就够了。
     * 对于 rightMethod 返回的 List，由于不能确认其是否为 null，所以在调用 size 方法获得列表大小之前，
     *      同样可以使用 Optional.ofNullable 包装一下返回值，然后通过.orElse(Collections.emptyList())
     *      实现在 List 为 null 的时候获得一个空的 List，最后再调用 size 方法
     */
    @Test
    public void test2(){
        String test = "1111";
        System.out.println(Optional.ofNullable(rightMethod(test.charAt(0) == '1' ? null : new FooService(),
                test.charAt(1) == '1' ? null : 1,
                test.charAt(2) == '1' ? null : "OK",
                test.charAt(3) == '1' ? null : "OK"))
                .orElse(Collections.emptyList()).size());
    }


    private List<String> rightMethod(FooService fooService, Integer i, String s, String t) {
        log.info("result {} {} {} {}", Optional.ofNullable(i).orElse(0) + 1, "OK".equals(s), Objects.equals(s, t), new HashMap<String, String>().put(null, null));
        Optional.ofNullable(fooService)
                .map(FooService::getBarService)
                .filter(barService -> "OK".equals(barService.bar()))
                .ifPresent(result -> log.info("OK"));
        return new ArrayList<>();
    }
}
