package com.example.businesscodepit.addsnack;

import com.example.businesscodepit.bean.Customer;
import com.example.businesscodepit.bean.Order;
import com.example.businesscodepit.bean.OrderItem;
import com.example.businesscodepit.bean.Product;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
/**
 * 描述：
 * <p>
 * 创建时间：2020/03/24
 * 修改时间：
 *
 * @author yaoyong
 **/
public class DifficultStream {
    private static Random random = new Random();
    private List<Order> orders;

    @Before
    public void data() {
        orders = Order.getData();

        orders.forEach(System.out::println);
        System.out.println("==========================================");
    }
    //通过stream方法把List或数组转换为流
    @Test
    public void stream()
    {
        Arrays.asList("a1", "a2", "a3").stream().forEach(System.out::println);
        Arrays.stream(new int[]{1, 2, 3}).forEach(System.out::println);
    }

    //通过Stream.of方法直接传入多个元素构成一个流
    @Test
    public void of()
    {
        String[] arr = {"a", "b", "c"};
        Stream.of(arr).forEach(System.out::println);
        Stream.of("a", "b", "c").forEach(System.out::println);
        Stream.of(1, 2, "a").map(item -> item.getClass().getName()).forEach(System.out::println);
    }

    //通过Stream.iterate方法使用迭代的方式构造一个无限流，然后使用limit限制流元素个数
    @Test
    public void iterate()
    {
        Stream.iterate(2, item -> item * 2).limit(10).forEach(System.out::println);
        Stream.iterate(BigInteger.ZERO, n -> n.add(BigInteger.TEN)).limit(10).forEach(System.out::println);
    }

    //通过Stream.generate方法从外部传入一个提供元素的Supplier来构造无限流，然后使用limit限制流元素个数
    @Test
    public void generate()
    {
        Stream.generate(() -> "test").limit(3).forEach(System.out::println);
        Stream.generate(Math::random).limit(10).forEach(System.out::println);
    }

    //通过IntStream或DoubleStream构造基本类型的流
    @Test
    public void primitive()
    {
        //演示IntStream和DoubleStream
        IntStream.range(1, 3).forEach(System.out::println);
        IntStream.range(0, 3).mapToObj(i -> "x").forEach(System.out::println);

        IntStream.rangeClosed(1, 3).forEach(System.out::println);
        DoubleStream.of(1.1, 2.2, 3.3).forEach(System.out::println);

        //各种转换，后面注释代表了输出结果
        System.out.println(IntStream.of(1, 2).toArray().getClass()); //class [I
        System.out.println(Stream.of(1, 2).mapToInt(Integer::intValue).toArray().getClass()); //class [I
        System.out.println(IntStream.of(1, 2).boxed().toArray().getClass()); //class [Ljava.lang.Object;
        System.out.println(IntStream.of(1, 2).asDoubleStream().toArray().getClass()); //class [D
        System.out.println(IntStream.of(1, 2).asLongStream().toArray().getClass()); //class [J

        //注意基本类型流和装箱后的流的区别
        Arrays.asList("a", "b", "c").stream()   // Stream<String>
                .mapToInt(String::length)       // IntStream
                .asLongStream()                 // LongStream
                .mapToDouble(x -> x / 10.0)     // DoubleStream
                .boxed()                        // Stream<Double>
                .mapToLong(x -> 1L)             // LongStream
                .mapToObj(x -> "")              // Stream<String>
                .collect(toList());
    }

    /**
     * filter 方法可以实现过滤操作，类似 SQL 中的 where。
     * 我们可以使用一行代码，通过 filter 方法实现查询所有订单中最近半年金额大于 40 的订单，
     * 通过连续叠加 filter 方法进行多次条件过滤：
     */
    public void testFilter(){
        //最近半年的金额大于40的订单
        orders.stream()
                .filter(Objects::nonNull) //过滤null值
                .filter(order -> order.getPlacedAt().isAfter(LocalDateTime.now().minusMonths(6))) //最近半年的订单
                .filter(order -> order.getTotalPrice() > 40) //金额大于40的订单
                .forEach(System.out::println);
    }

    /**
     * map 操作可以做转换（或者说投影），类似 SQL 中的 select。为了对比，
     * 我用两种方式统计订单中所有商品的数量，前一种是通过两次遍历实现，
     * 后一种是通过两次 mapToLong+sum 方法实现
     */
    @Test
    public void map() {
        //计算所有订单商品数量
        //通过两次遍历实现
        LongAdder longAdder = new LongAdder();
        orders.stream().forEach(order ->
                order.getOrderItemList().forEach(orderItem -> longAdder.add(orderItem.getProductQuantity())));
        //使用两次mapToLong+sum方法实现
        assertThat(longAdder.longValue(), is(orders.stream().mapToLong(order ->
                order.getOrderItemList().stream()
                        .mapToLong(OrderItem::getProductQuantity).sum()).sum()));
        //map与mapToLong使用区别  map返回Stream<T>  mapToLong返回LongStream   LongStream有自己的特殊方法可以直接调用
        orders.stream().map(i ->  new Product((long) i.getId(), "product" + i, i.getId() * 100.0));
        orders.stream().mapToLong(i -> i.getId());
        //把IntStream通过转换Stream<Project>
        System.out.println(IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Product((long) i, "product" + i, i * 100.0))
                .collect(toList()));
        //把IntStream通过转换Stream<Project>
        System.out.println(IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Product((long) i, "product" + i, i * 100.0))
                .collect(toList()));
    }

    /**
     * sorted 操作可以用于行内排序的场景，类似 SQL 中的 order by。
     * 比如，要实现大于 50 元订单的按价格倒序取前 5，可以通过 Order::getTotalPrice 方法引用直接指定需要排序的依据字段，
     * 通过 reversed() 实现倒序
     */
    @Test
    public void sorted() {
        System.out.println("//大于50的订单,按照订单价格倒序前5");
        orders.stream().filter(order -> order.getTotalPrice() > 50)
                .sorted(comparing(Order::getTotalPrice).reversed())
                .limit(5)
                .forEach(System.out::println);
    }

    /**
     * 比如，我们要统计所有订单的总价格，可以有两种方式：
     *      直接通过原始商品列表的商品个数 * 商品单价统计的话，
     *      可以先把订单通过 flatMap 展开成商品清单，也就是把 Order 替换为 Stream，
     *      然后对每一个 OrderItem 用 mapToDouble 转换获得商品总价，最后进行一次 sum 求和；
     *      利用 flatMapToDouble 方法把列表中每一项展开替换为一个 DoubleStream，
     *      也就是直接把每一个订单转换为每一个商品的总价，然后求和。
     */
    @Test
    public void flatMap() {
        //不依赖订单上的总价格字段
        System.out.println(orders.stream().mapToDouble(order -> order.getTotalPrice()).sum());

        //如果不依赖订单上的总价格,可以直接展开订单商品进行价格统计
        //直接将orders的所有orderItemList拼成一个Stream
        System.out.println(orders.stream()
                .flatMap(order -> order.getOrderItemList().stream())
                .mapToDouble(item -> item.getProductQuantity() * item.getProductPrice()).sum());

        //另一种方式flatMap+mapToDouble=flatMapToDouble
        System.out.println(orders.stream()
                .flatMapToDouble(order ->
                        order.getOrderItemList()
                                .stream().mapToDouble(item -> item.getProductQuantity() * item.getProductPrice()))
                .sum());
    }

    /**
     * groupingBy  第一个参数分组条件key   第二个条件value值(终结操作)->针对的就是每个分组条件抽出来的Stream<Order>
     */
    @Test
    public void groupBy() {

        System.out.println("//按照用户名分组，统计下单数量");
        System.out.println(orders.stream().collect(groupingBy(Order::getCustomerName, counting()))
                .entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).collect(toList()));

        System.out.println("//按照用户名分组,统计订单总金额");
        System.out.println(orders.stream().collect(groupingBy(Order::getCustomerName, summingDouble(Order::getTotalPrice)))
                .entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).collect(toList()));

        System.out.println("//按照用户名分组,统计商品采购数量");
        System.out.println(orders.stream().collect(groupingBy(Order::getCustomerName,
                summingInt(order -> order.getOrderItemList().stream()
                        .collect(summingInt(OrderItem::getProductQuantity)))))
                .entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).collect(toList()));

        System.out.println("//统计最受欢迎的商品，倒序后取第一个");
        orders.stream()
                .flatMap(order -> order.getOrderItemList().stream())
                .collect(groupingBy(OrderItem::getProductName, summingInt(OrderItem::getProductQuantity)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(System.out::println);

        System.out.println("//统计最受欢迎的商品的另一种方式,直接利用maxBy");
        orders.stream()
                .flatMap(order -> order.getOrderItemList().stream())
                .collect(groupingBy(OrderItem::getProductName, summingInt(OrderItem::getProductQuantity)))
                .entrySet().stream()
                .collect(maxBy(Map.Entry.comparingByValue()))
                .map(Map.Entry::getKey)
                .ifPresent(System.out::println);

        //collectingAndThen  先执行终结操作  再把最终结果放入Function<R,RR> finisher作为参数执行
        System.out.println("//按照用户名分组，选用户下的总金额最大的订单");
        orders.stream().collect(groupingBy(Order::getCustomerName, collectingAndThen(maxBy(comparingDouble(Order::getTotalPrice)), Optional::get)))
                .forEach((k, v) -> System.out.println(k + "#" + v.getTotalPrice() + "@" + v.getPlacedAt()));
        //mapping  第一个参数对流中的元素进行变化  第二个参数进行收集
        System.out.println("//根据下单年月分组统计订单ID列表");
        System.out.println(orders.stream().collect
                (groupingBy(order -> order.getPlacedAt().format(DateTimeFormatter.ofPattern("yyyyMM")),
                        mapping(order -> order.getId(), toList()))));
        //groupingBy 嵌套分组  先通过下单年月进行分组 再通过用户名进行分组
        System.out.println("//根据下单年月+用户名两次分组，统计订单ID列表");
        System.out.println(orders.stream().collect
                (groupingBy(order -> order.getPlacedAt().format(DateTimeFormatter.ofPattern("yyyyMM")),
                        groupingBy(order -> order.getCustomerName(),
                                mapping(order -> order.getId(), toList())))));
    }

    /**
     * 统计orders价格最高的以及最低的
     */
    @Test
    public void maxMin() {
        orders.stream().max(comparing(Order::getTotalPrice)).ifPresent(System.out::println);
        orders.stream().min(comparing(Order::getTotalPrice)).ifPresent(System.out::println);
    }

    /**
     * 统计花钱最多的人
     *      先统计每个人的花费  在通过maxBy取出value为最大值的key
     */
    @Test
    public void reduce() {
        System.out.println("//统计花钱最多的人");
        System.out.println(orders.stream().collect(groupingBy(Order::getCustomerName, summingDouble(Order::getTotalPrice)))
                .entrySet().stream()
                .reduce(BinaryOperator.maxBy(Map.Entry.comparingByValue()))
                .map(Map.Entry::getKey).orElse("N/A"));
    }

    /**
     * 查询去重后的下单用户。使用 map 从订单提取出购买用户，
     * 然后使用 distinct 去重。查询购买过的商品名。使用 flatMap+map 提取出订单中所有的商品名，
     * 然后使用 distinct 去重。
     */
    @Test
    public void distinct() {
        System.out.println("//不去重的下单用户");
        System.out.println(orders.stream().map(order -> order.getCustomerName()).collect(joining(",")));

        System.out.println("//去重的下单用户");
        System.out.println(orders.stream().map(order -> order.getCustomerName()).distinct().collect(joining(",")));

        System.out.println("//所有购买过的商品");
        System.out.println(orders.stream()
                .flatMap(order -> order.getOrderItemList().stream())
                .map(OrderItem::getProductName)
                .distinct().collect(joining(",")));
    }

    /**
     * collect 是收集操作，对流进行终结（终止）操作，把流导出为我们需要的数据结构。
     * “终结”是指，导出后，无法再串联使用其他中间操作，比如 filter、map、flatmap、sorted、distinct、limit、skip。
     * 在 Stream 操作中，collect 是最复杂的终结操作，
     * 比较简单的终结操作还有 forEach、toArray、min、max、count、anyMatch 等，我就不再展开了，
     * 你可以查询JDK 文档，搜索 terminal operation 或 intermediate operation
     */
    @Test
    public void collect() {
        System.out.println("//生成一定位数的随机字符串");
        System.out.println(random.ints(48, 122)
                .filter(i -> (i < 57 || i > 65) && (i < 90 || i > 97))
                .mapToObj(i -> (char) i)
                .limit(20)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString());

        System.out.println("//所有下单的用户,使用toSet去重了");
        System.out.println(orders.stream()
                .map(order -> order.getCustomerName()).collect(toSet())
                .stream().collect(joining(",", "[", "]")));

        System.out.println("//用toCollection收集器指定集合类型");
        System.out.println(orders.stream().limit(2).collect(toCollection(LinkedList::new)).getClass());

        System.out.println("//使用toMap获取订单ID+下单用户名的Map");
        orders.stream()
                .collect(toMap(Order::getId, Order::getCustomerName))
                .entrySet().forEach(System.out::println);

        System.out.println("//使用toMap获取下单用户名+最近一次下单时间的Map");
        // (x, y) -> x.isAfter(y) ? x : y)  key已存在的情况下  value取值
        orders.stream()
                .collect(toMap(Order::getCustomerName, Order::getPlacedAt, (x, y) -> x.isAfter(y) ? x : y))
                .entrySet().forEach(System.out::println);

        System.out.println("//订单平均购买的商品数量");
        System.out.println(orders.stream().collect(averagingInt(order ->
                order.getOrderItemList().stream()
                        .collect(summingInt(OrderItem::getProductQuantity)))));
        //获取Stream中流的个数
        System.out.println(orders.stream().collect(counting()));
        System.out.println(orders.stream().max(comparing(Order::getTotalPrice)));
        System.out.println(orders.stream().collect(maxBy(comparing(Order::getTotalPrice))));
    }

    /**
     * partitioningBy 用于分区，分区是特殊的分组，只有 true 和 false 两组。
     * 比如，我们把用户按照是否下单进行分区，给 partitioningBy 方法传入一个 Predicate 作为数据分区的区分，
     * 输出是 Map<Boolean, List>
     */
    @Test
    public void partition() {
        //先来看一下所有下单的用户
        orders.stream().map(order -> order.getCustomerName()).collect(toSet()).forEach(System.out::println);
        //根据是否有下单记录进行分区
        System.out.println(Customer.getData().stream().collect(
                partitioningBy(customer -> orders.stream().mapToLong(Order::getCustomerId)
                        .anyMatch(id -> id == customer.getId()))));
    }

    /**
     * skip 和 limit 操作用于分页，类似 MySQL 中的 limit。
     * 其中，skip 实现跳过一定的项，limit 用于限制项总数。
     * 比如下面的两段代码：按照下单时间排序，查询前 2 个订单的顾客姓名和下单时间；
     * 按照下单时间排序，查询第 3 和第 4 个订单的顾客姓名和下单时间
     */
    @Test
    public void skipLimit() {
        orders.stream()
                .sorted(comparing(Order::getPlacedAt))
                .map(order -> order.getCustomerName() + "@" + order.getPlacedAt())
                .limit(2).forEach(System.out::println);

        orders.stream()
                .sorted(comparing(Order::getPlacedAt))
                .map(order -> order.getCustomerName() + "@" + order.getPlacedAt())
                .skip(2).limit(2).forEach(System.out::println);
    }

    /**
     * map与peek区别  主要是看入参
     *  peek  Consumer<? super T> action  无返回值
     */
    @Test
    public void peek() {
        orders.stream()
                .filter(order -> order.getTotalPrice() > 40)
                .peek(order -> System.out.println(order.toString()))
                .map(Order::getCustomerName)
                .collect(toList());
    }

    @Test
    public void customCollector() //自定义收集器
    {
        //最受欢迎收集器
        assertThat(Stream.of(1, 1, 2, 2, 2, 3, 4, 5, 5).collect(new MostPopularCollector<>()).get(), is(2));
        assertThat(Stream.of('a', 'b', 'c', 'c', 'c', 'd').collect(new MostPopularCollector<>()).get(), is('c'));
    }
}
