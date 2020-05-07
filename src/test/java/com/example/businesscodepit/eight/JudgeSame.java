package com.example.businesscodepit.eight;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/06
 * 修改时间：
 *      判等第一个案例中：
 *          编译器会把 Integer a = 127 转换为 Integer.valueOf(127)。
 *          查看源码可以发现，这个转换在内部其实做了缓存，使得两个 Integer 指向同一个对象
 *      第二个案例中:
 *          之所以同样的代码 128 就返回 false 的原因是，
 *          默认情况下会缓存[-128, 127]的数值，而 128 处于这个区间之外。
 *          设置 JVM 参数加上 -XX:AutoBoxCacheMax=1000 再试试，是不是就返回 true 了呢？
 *      第三和第四个案例中：
 *          New 出来的 Integer 始终是不走缓存的新对象。比较两个新对象，或者比较一个新对象和一个来自缓存的对象，结果肯定不是相同的对象，因此返回 false
 *      第五个案例中
 *          我们把装箱的 Integer 和基本类型 int 比较，
 *          前者会先拆箱再比较，比较的肯定是数值而不是引用，因此返回 true
 * @author yaoyong
 *
 * 重写equals方法
 *      考虑到性能，可以先进行指针判等，如果对象是同一个那么直接返回 true；
 *      需要对另一方进行判空，空对象和自身进行比较，结果一定是 fasle；
 *      需要判断两个对象的类型，如果类型都不同，那么直接返回 false；
 *      确保类型相同的情况下再进行类型强制转换，然后逐一判断所有字段
 *          instanceof进行类型检查规则是:你是该类或者是该类的子类；
 *          getClass获得类型信息采用==来进行检查是否相等的操作是严格的判断。不会存在继承方面的考虑；
 * hashCode 和 equals 要配对实现
 *
 * compareTo 和 equals 的逻辑一致性
 *
 *  HashSet本质上就是HashMap的key组成的不重复的元素集合，
 *      contains方法其实就是根据hashcode和equals去判断相等的
 *      e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))
 *  TreeSet本质TreeMap的key组成的，数据结构是红黑树，是自带排序功能的，
 *      可以在放入元素的时候指定comparator（比较器），
 *      或者是放入的元素要实现Comparable接口后元素自己实现compareTo方法，
 *      contains方法是根据比较器或者compareTo去判断相等
 **/
public class JudgeSame {
    private static Logger log = LoggerFactory.getLogger(JudgeSame.class);

    public static void main(String[] args) {

        Integer a = 127; //Integer.valueOf(127)
        Integer b = 127; //Integer.valueOf(127)
        log.info("\nInteger a = 127;\n" +
                "Integer b = 127;\n" +
                "a == b ? {}",a == b);    // true

        Integer c = 128; //Integer.valueOf(128)
        Integer d = 128; //Integer.valueOf(128)
        log.info("\nInteger c = 128;\n" +
                "Integer d = 128;\n" +
                "c == d ? {}", c == d);   //false

        Integer e = 127; //Integer.valueOf(127)
        Integer f = new Integer(127); //new instance
        log.info("\nInteger e = 127;\n" +
                "Integer f = new Integer(127);\n" +
                "e == f ? {}", e == f);   //false

        Integer g = new Integer(127); //new instance
        Integer h = new Integer(127); //new instance
        log.info("\nInteger g = new Integer(127);\n" +
                "Integer h = new Integer(127);\n" +
                "g == h ? {}", g == h);  //false

        Integer i = 128; //unbox
        int j = 128;
        log.info("\nInteger i = 128;\n" +
                "int j = 128;\n" +
                "i == j ? {}", i == j); //true
    }

    /**
     * 字符串常量池机制:
     *      首先要明确的是其设计初衷是节省内存。当代码中出现双引号形式创建字符串对象时，
     *      JVM 会先对这个字符串进行检查，如果字符串常量池中存在相同内容的字符串对象的引用，
     *      则将这个引用返回；否则，创建新的字符串对象，然后将这个引用放入字符串常量池，
     *      并返回该引用。这种机制，就是字符串驻留或池化
     *  第一个案例返回 true
     *      因为 Java 的字符串驻留机制，直接使用双引号声明出来的两个 String 对象指向常量池中的相同字符串。
     *  第二个案例
     *      new 出来的两个 String 是不同对象，引用当然不同，所以得到 false 的结果。
     *  第三个案例
     *      使用 String 提供的 intern 方法也会走常量池机制，所以同样能得到 true。
     *      虽然使用 new 声明的字符串调用 intern 方法，也可以让字符串进行驻留，
     *      但在业务代码中滥用 intern，可能会产生性能问题
     *
     *  第四个案例，通过 equals 对值内容判等，是正确的处理方式，当然会得到 true
     */
    @Test
    public void testString(){
        String a = "1";
        String b = "1";
        log.info("\nString a = \"1\";\n" +
                "String b = \"1\";\n" +
                "a == b ? {}", a == b); //true

        String c = new String("2");
        String d = new String("2");
        log.info("\nString c = new String(\"2\");\n" +
                "String d = new String(\"2\");" +
                "c == d ? {}", c == d); //false

        String e = new String("3").intern();
        String f = new String("3").intern();
        log.info("\nString e = new String(\"3\").intern();\n" +
                "String f = new String(\"3\").intern();\n" +
                "e == f ? {}", e == f); //true

        String g = new String("4");
        String h = new String("4");
        log.info("\nString g = new String(\"4\");\n" +
                "String h = new String(\"4\");\n" +
                "g == h ? {}", g.equals(h)); //true
    }
}
