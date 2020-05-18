package com.example.businesscodepit.eighteen;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 反射、注解和泛型遇到重载和继承时可能会产生的坑
 *
 *      getMethods 和 getDeclaredMethods 是有区别的，前者可以查询到父类方法，后者只能查询到当前类。
 *      反射进行方法调用要注意过滤桥接方法
 */
@Slf4j
public class OOPTest {
    private void age(int age) {
        log.info("int age = {}", age);
    }
    private void age(Integer age) {
        log.info("Integer age = {}", age);
    }

    /**
     * 反射调用方法不是以传参决定重载
     * 要通过反射进行方法调用:
     *  第一步就是通过方法签名来确定方法。具体到这个案例，getDeclaredMethod 传入的参数类型 Integer.TYPE 代表的是 int，
     *      所以实际执行方法时无论传的是包装类型还是基本类型，都会调用 int 入参的 age 方法
     */
    @Test
    public void OOPTest() throws Exception {
        OOPTest application = new OOPTest();
        application.age(36);
        application.age(Integer.valueOf("36"));
        //走的是int方法
        OOPTest.class.getDeclaredMethod("age", Integer.TYPE).invoke(this, Integer.valueOf("36"));
        //走的是Integer方法
        OOPTest.class.getDeclaredMethod("age", Integer.class).invoke(this, Integer.valueOf("36"));
        OOPTest.class.getDeclaredMethod("age", Integer.class).invoke(this, 36);
    }

    /**
     * 泛型经过类型擦除多出桥接方法的坑
     *      运行结果；Child1.setValue called
     *              Parent.setValue called
     *              Parent.setValue called
     *              value: test updateCount: 2
     *
     *      这个案例中，子类方法重写父类方法失败的原因，包括两方面：
     *              一是，子类没有指定 String 泛型参数，父类的泛型方法 setValue(T value) 在泛型擦除后是 setValue(Object value)，
     *                  子类中入参是 String 的 setValue 方法被当作了新方法；
     *              二是，子类的 setValue 方法没有增加 @Override 注解，因此编译器没能检测到重写失败的问题。
     *                      这就说明，重写子类方法时，标记 @Override 是一个好习惯
     *       解决方案示例
     *               1.将getMethods替换成getDeclaredMethods   这样就只会获取当前类的public、protected、package 和 private 方法
     *                      getMethods是获取当前类以及父类的public、protected、package 和 private 方法
     *                 但是这只是治标不治本,因为对于Child1而言 还是有两个setValue方法,重写父方法失败
     */
    @Test
    public void test2(){
        Child1 child1 = new Child1();
        Arrays.stream(child1.getClass().getMethods())
                .filter(method -> method.getName().equals("setValue"))
                .forEach(method -> {
                    try {
                        method.invoke(child1, "test");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        System.out.println(child1.toString());
        log.info(">>>>>>>>getDeclaredMethods");
        Arrays.stream(child1.getClass().getDeclaredMethods())
                .filter(method -> method.getName().equals("setValue"))
                .forEach(method -> {
                    try {
                        method.invoke(child1, "test");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        System.out.println(child1.toString());
    }

    /**
     * 尽管Child2定义了泛型、@Override
     * 但是还没无法达到预期的效果,因为Java 的泛型类型在编译后擦除为 Object。
     *      虽然子类指定了父类泛型 T 类型是 String，但编译后 T 会被擦除成为 Object，
     *      所以父类 setValue 方法的入参是 Object，value 也是 Object。
     *      如果子类 Child2 的 setValue 方法要覆盖父类的 setValue 方法，那入参也必须是 Object。
     *      所以，编译器会为我们生成一个所谓的 bridge 桥接方法，
     *      使用 javap 命令来反编译编译后的 Child2 类的 class 字节码  可以使用method 的 isBridge 方法
     *      获得结果 该方法是否桥接方法
     */
    @Test
    public void test3(){
        log.info(">>>>>>>>Child2");
        Child2 child2 = new Child2();
        Arrays.stream(child2.getClass().getDeclaredMethods())
                .filter(method -> method.getName().equals("setValue"))
                .forEach(method -> {
                    try {
                        method.invoke(child2, "test");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        System.out.println(child2.toString());
        //method.isBridge() 判断该方法是否桥接方法
        log.info(">>>>>>>>exclude isBridge");
        Arrays.stream(child2.getClass().getDeclaredMethods())
                .filter(method -> method.getName().equals("setValue") && !method.isBridge())
                .findFirst().ifPresent(method -> {
            try {
                method.invoke(child2, "test");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 注解可以继承吗？
     */
    @Test
    public void test4(){

    }
}


class Parent<T> {
    //用于记录value更新的次数，模拟日志记录的逻辑
    AtomicInteger updateCount = new AtomicInteger();
    private T value;
    //重写toString，输出值和值更新次数
    @Override
    public String toString() {
        return String.format("value: %s updateCount: %d", value, updateCount.get());
    }
    //设置值
    public void setValue(T value) {
        System.out.println("Parent.setValue called");
        this.value = value;
        updateCount.incrementAndGet();
    }
}


class Child1 extends Parent {
    public void setValue(String value) {
        System.out.println("Child1.setValue called");
        super.setValue(value);
    }
}


class Child2 extends Parent<String> {
    @Override
    public void setValue(String value) {
        System.out.println("Child2.setValue called");
        super.setValue(value);
    }
}


