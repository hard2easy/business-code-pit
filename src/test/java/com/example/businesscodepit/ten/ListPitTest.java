package com.example.businesscodepit.ten;

import com.example.businesscodepit.bean.Order;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 *  不能直接使用 Arrays.asList 来转换基本类型数组
 *  Arrays.asList 返回的 List 不支持增删操作
 *  对原始数组的修改会影响到我们获得的那个 List
 *  使用 List.subList 进行切片操作居然会导致 OOM(引用了原List对象 导致原List对象无法释放,并且如果修改原历史对象会对subList返回对象有影响)
 *  要对大 List 进行单值搜索的话，可以考虑使用 HashMap，其中 Key 是要搜索的值，Value 是原始对象，会比使用 ArrayList 有非常明显的性能优势
 */
public class ListPitTest {
    private  static Logger log = LoggerFactory.getLogger(ListPitTest.class);
    private static List<List<Integer>> data = new ArrayList<>();

    /**
     * asList(T... a)
     * int自动装箱成Integer 但是无法将int[] 转化为 Integer[]
     * ArrayList(E[] array) {
     *     a = Objects.requireNonNull(array);
     * }
     */
    @Test
    public void test1(){
        int[] arr = {1, 2, 3};
        List list = Arrays.asList(arr);
        log.info("list:{} size:{} class:{}", list, list.size(), list.get(0).getClass());

        //以下两种方式可以实现效果  Arrays.stream
        int[] arr1 = {1, 2, 3};
        List list1 = Arrays.stream(arr1).boxed().collect(Collectors.toList());
        log.info("list:{} size:{} class:{}", list1, list1.size(), list1.get(0).getClass());

        //直接使用Integer[] 数组
        Integer[] arr2 = {1, 2, 3};
        List list2 = Arrays.asList(arr2);
        log.info("list:{} size:{} class:{}", list2, list2.size(), list2.get(0).getClass());
    }

    /**
     * Arrays.asList返回的是Arrays的内部类ArrayList,其没有实现add方法,默认使用的是父类的方法,父类的add当法就是抛出异常UnsupportedOperationException
     */
    @Test
    public void test2(){
        String[] arr = {"1", "2", "3"};
        List list = Arrays.asList(arr);
        arr[1] = "4";
        try {
            list.add("5");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log.info("arr:{} list:{}", Arrays.toString(arr), list);
    }

    /**
     * private final E[] a;
     *
     * ArrayList(E[] array) {
     *     a = Objects.requireNonNull(array);
     * }
     * Arrays.asList返回的Arrays的内部类ArrayList,其实就是把array引用给成员变量a
     */
    @Test
    public void test3(){
        String[] arr = {"1", "2", "3"};
        List list = Arrays.asList(arr);
        arr[1] = "4";
        log.info("arr:{} list:{}", Arrays.toString(arr), list);

        //只有通过new ArrayList()再次封装  才不会受arr2数组的影响
        String[] arr2 = {"1", "2", "3"};
        List listFix = new ArrayList(Arrays.asList(arr2));
        arr[1] = "4";
        try {
            list.add("5");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log.info("arr:{} list:{}", Arrays.toString(arr), list);
    }

    /**
     *  定义一个名为 data 的静态 List 来存放 Integer 的 List，也就是说 data 的成员本身是包含了多个数字的 List。循环 1000 次，
     *  每次都从一个具有 10 万个 Integer 的 List 中，使用 subList 方法获得一个只包含一个数字的子 List，并把这个子 List 加入 data 变量
     *
     *  以下测试会出现oom(具体看各自运行环境大小)；
     *      循环中的 1000 个具有 10 万个元素的 List 始终得不到回收，因为它始终被 subList 方法返回的 List 强引用
     *
     *  原始 List 中数字 3 被删除了，说明删除子 List 中的元素影响到了原始 List；
     *  尝试为原始 List 增加数字 0 之后再遍历子 List，会出现 ConcurrentModificationException
     *
     *      第一，ArrayList 维护了一个叫作 modCount 的字段，表示集合结构性修改的次数。所谓结构性修改，指的是影响 List 大小的修改，
     *          所以 add 操作必然会改变 modCount 的值。
     *      第二，分析第 21 到 24 行的 subList 方法可以看到，获得的 List 其实是内部类 SubList，并不是普通的 ArrayList，在初始化的时候传入了 this。
     *      第三，分析第 26 到 39 行代码可以发现，这个 SubList 中的 parent 字段就是原始的 List。
     *          SubList 初始化的时候，并没有把原始 List 中的元素复制到独立的变量中保存。
     *          我们可以认为 SubList 是原始 List 的视图，并不是独立的 List。双方对元素的修改会相互影响，而且 SubList 强引用了原始的 List，
     *          所以大量保存这样的 SubList 会导致 OOM。
     *       第四，分析第 47 到 55 行代码可以发现，遍历 SubList 的时候会先获得迭代器，比较原始 ArrayList modCount 的值和 SubList 当前 modCount 的值。
     *          获得了 SubList 后，我们为原始 List 新增了一个元素修改了其 modCount，所以判等失败抛出 ConcurrentModificationException 异常
     */
    @Test
    public void test4(){
        List<Integer> list = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
        List<Integer> subList = list.subList(1, 4);
        System.out.println(subList);
        subList.remove(1);
        System.out.println(list);
        list.add(0);
        try {
            subList.forEach(System.out::println);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //方法一:不直接使用subList返回的集合
        List<Integer> subListFix = new ArrayList<>(list.subList(1, 4));
        //方法二:使用java8的语法
        List<Integer> subListFixTwo = list.stream().skip(1).limit(3).collect(Collectors.toList());
//        for (int i = 0; i < 1000; i++) {
//            List<Integer> rawList = IntStream.rangeClosed(1, 100000).boxed().collect(Collectors.toList());
//            data.add(rawList.subList(0, 1));
//        }
    }

    /**
     * 要对大 List 进行单值搜索的话，可以考虑使用 HashMap，其中 Key 是要搜索的值，Value 是原始对象，会比使用 ArrayList 有非常明显的性能优势
     * 需要比较HashMap与List
     *      HashMap占用更多的控空间  信性能会慢一些
     *      List性能插  所需空间更少
     *           时间     空间
     */
    @Test
    public void test5(){
        int elementCount = 1000000;
        int loopCount = 1000;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("listSearch");
        Object list = listSearch(elementCount, loopCount);
        System.out.println(ObjectSizeCalculator.getObjectSize(list));
        stopWatch.stop();
        stopWatch.start("mapSearch");
        Object map = mapSearch(elementCount, loopCount);
        stopWatch.stop();
        System.out.println(ObjectSizeCalculator.getObjectSize(map));
        System.out.println(stopWatch.prettyPrint());
    }
    private static Object listSearch(int elementCount, int loopCount) {
        List<Order> list = LongStream.rangeClosed(1, elementCount).mapToObj(i -> new Order(i)).collect(Collectors.toList());
        IntStream.rangeClosed(1, loopCount).forEach(i -> {
            long search = ThreadLocalRandom.current().nextLong(elementCount);
            Order result = list.stream().filter(order -> order.getId() == search).findFirst().orElse(null);
            Assert.assertTrue(result != null && result.getId() == search);
        });
        return list;
    }


    private static Object mapSearch(int elementCount, int loopCount) {
        Map<Long, Order> map = LongStream.rangeClosed(1, elementCount).boxed().collect(Collectors.toMap(Function.identity(), i -> new Order(i)));
        IntStream.rangeClosed(1, loopCount).forEach(i -> {
            long search = ThreadLocalRandom.current().nextLong(elementCount);
            Order result = map.get(search);
            Assert.assertTrue(result != null && result.getId() == search);
        });
        return map;
    }

    /**
     * LinkedList  插入时间复杂度为O(1) 但是那指的是:你已经有了那个要插入节点的指针
     * 实际使用过程中  我们需要先通过循环获取到那个节点的 Node，然后再执行插入操作 所以，对于插入操作，LinkedList 的时间复杂度其实也是 O(n)
     */
    @Test
    public void test6(){

        int elementCount = 100000;
        int loopCount = 100000;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("linkedListGet");
        linkedListGet(elementCount, loopCount);
        stopWatch.stop();
        stopWatch.start("arrayListGet");
        arrayListGet(elementCount, loopCount);
        stopWatch.stop();
        System.out.println(stopWatch.prettyPrint());


        StopWatch stopWatch2 = new StopWatch();
        stopWatch2.start("linkedListAdd");
        linkedListAdd(elementCount, loopCount);
        stopWatch2.stop();
        stopWatch2.start("arrayListAdd");
        arrayListAdd(elementCount, loopCount);
        stopWatch2.stop();
        System.out.println(stopWatch2.prettyPrint());
    }

    //LinkedList访问
    private static void linkedListGet(int elementCount, int loopCount) {
        List<Integer> list = IntStream.rangeClosed(1, elementCount).boxed().collect(Collectors.toCollection(LinkedList::new));
        IntStream.rangeClosed(1, loopCount).forEach(i -> list.get(ThreadLocalRandom.current().nextInt(elementCount)));
    }

    //ArrayList访问
    private static void arrayListGet(int elementCount, int loopCount) {
        List<Integer> list = IntStream.rangeClosed(1, elementCount).boxed().collect(Collectors.toCollection(ArrayList::new));
        IntStream.rangeClosed(1, loopCount).forEach(i -> list.get(ThreadLocalRandom.current().nextInt(elementCount)));
    }

    //LinkedList插入
    private static void linkedListAdd(int elementCount, int loopCount) {
        List<Integer> list = IntStream.rangeClosed(1, elementCount).boxed().collect(Collectors.toCollection(LinkedList::new));
        IntStream.rangeClosed(1, loopCount).forEach(i -> list.add(ThreadLocalRandom.current().nextInt(elementCount),1));
    }

    //ArrayList插入
    private static void arrayListAdd(int elementCount, int loopCount) {
        List<Integer> list = IntStream.rangeClosed(1, elementCount).boxed().collect(Collectors.toCollection(ArrayList::new));
        IntStream.rangeClosed(1, loopCount).forEach(i -> list.add(ThreadLocalRandom.current().nextInt(elementCount),1));
    }
}
