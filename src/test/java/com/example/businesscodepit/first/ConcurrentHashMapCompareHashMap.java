package com.example.businesscodepit.first;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * 极客-业务代码01
 *      ThreadLocal
 *          绑定线程,用于某个线程之间的业务传输
 *          适用于相同线程的不同逻辑需要共享数据（但又无法通过传值来共享数据），
 *          或为了避免相同线程重复创建对象希望重用数据，可以考虑使用ThreadLocal
 *          但是在特定场景下ThreadLocal并不是线程安全的,例如部署在tomcat服务器上的项目
 *          此时ThreadLocal绑定的是tomcat的线程,tomcat的线程是通过线程池重现的,存在线程复用
 *      ThreadLocalRandom
 *          不能设置为全局变量用于各线程共享,因为ThreadLocalRandom同样需要获取Seed,此时只有主线程
 *          与seed进行了绑定,其他线程的seed取值会有问题  查看ThreadLocalRandom.current()
 *          除了初始化 ThreadLocalRandom 的主线程获取的随机值是无模式的（调用者不可预测下个返回值，满足我们对伪随机的要求）之外，
 *          其他线程获得随机值都不是相互独立的（本质上来说，是因为他们用于生成随机数的种子 seed 的值可预测的，为 i*gamma，
 *          其中 i 是当前线程调用随机数生成方法次数，而 gamma 是 ThreadLocalRandom 类的一个 long 静态字段值）。
 *          例如，一个有趣的现象是，所有非初始化 ThreadLocalRandom 实例的线程如果调用相同次数的 nextInt() 方法，
 *          他们得到的随机数串是完全相同的。造成这样现象的原因在于，ThreadLocalRandom 类维护了一个类单例字段，
 *          线程通过调用 ThreadLocalRandom#current() 方法来获取 ThreadLocalRandom 单例，
 *          然后以线程维护的实例字段 threadLocalRandomSeed 为种子生成下一个随机数和下一个种子值。
 *          那么既然是单例模式，为什么多线程共用主线程初始化的实例就会出问题呢。
 *          问题就在于 current 方法，线程在调用 current() 方法的时候，会根据用每个线程的 thread 的一个实例字段
 *          threadLocalRandomProbe 是否为 0 来判断是否当前线程实例是否为第一次调用随机数生成方法，
 *          从而决定是否要给当前线程初始化一个随机的 threadLocalRandomSeed 种子值。
 *          因此，如果其他线程绕过 current 方法直接调用随机数方法，
 *          那么它的种子值就是 0, 1*gamma, 2*gamma... 因此也就是可预测的了
 */
public class ConcurrentHashMapCompareHashMap {
    private ThreadLocal<Integer> id = ThreadLocal.withInitial(() -> null);
    private ThreadLocalRandom random = ThreadLocalRandom.current();
    private static Logger log = LoggerFactory.getLogger(ConcurrentHashMapCompareHashMap.class);
    //循环次数
    private static int LOOP_COUNT = 10000000;
    //线程数量
    private static int THREAD_COUNT = 10;
    //元素数量
    private static int ITEM_COUNT = 1000;

    //帮助方法，用来获得一个指定元素数量模拟数据的ConcurrentHashMap
    private ConcurrentHashMap<String, Long> getData(int count) {
        return LongStream.rangeClosed(1, count)
                .boxed()
                .collect(Collectors.toConcurrentMap(i -> UUID.randomUUID().toString(), Function.identity(),
                        (o1, o2) -> o1, ConcurrentHashMap::new));
    }

    /**
     * 1  使用了 ConcurrentHashMap，不代表对它的多个操作之间的状态是一致的，是没有其他线程在操作它的，如果需要确保需要手动加锁。
     * 2  诸如 size、isEmpty 和 containsValue 等聚合方法，在并发情况下可能会反映 ConcurrentHashMap 的中间状态。
     * 因此在并发情况下，这些方法的返回值只能用作参考，而不能用于流程控制。显然，利用 size 方法计算差异值，是一个流程控制。
     * 3  诸如 putAll 这样的聚合方法也不能确保原子性，在 putAll 的过程中去获取数据可能会获取到部分数据。
     * @return
     * @throws InterruptedException
     */
    @Test
    public void wrong() throws InterruptedException {
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);
        //初始900个元素
        log.info("init size:{}", concurrentHashMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        //使用线程池并发处理逻辑
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, 10).parallel().forEach(i -> {
            //查询还需要补充多少个元素
            int gap = ITEM_COUNT - concurrentHashMap.size();
            log.info("gap size:{}", gap);
            //补充元素
            concurrentHashMap.putAll(getData(gap));
        }));
        //等待所有任务完成
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        //最后元素个数会是1000吗？
        log.info("finish size:{}", concurrentHashMap.size());
    }

    /**
     * normaluse与gooduse
     *     熟悉ConcurrentHashMap特性可以提升性能
     *        computeIfAbsent(接受Function)  putIfAbsent(接受某一个value)
     *              相同点：两者均是指定的key不存在其对应的value时，进行操作，指定的key存在对应的value时，直接返回value。
     *              不同点：
     *              线程安全性：putIfAbsent线程非安全，computeIfAbsent线程安全；
     *              返回值：指定key对应的value不存在时，putIfAbsent进行设置并返回null
     *                      computeIfAbsent进行计算(接受Function 在Function中进行计算)并返回新值；
     * @return
     * @throws InterruptedException
     */
    private static Map<String, Long> normaluse() throws InterruptedException {
        ConcurrentHashMap<String, Long> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(i -> {
                    //获得一个随机的Key
                    String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                    synchronized (freqs) {
                        if (freqs.containsKey(key)) {
                            //Key存在则+1
                            freqs.put(key, freqs.get(key) + 1);
                        } else {
                            //Key不存在则初始化为1
                            freqs.put(key, 1L);
                        }
                    }
                }
        ));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        return freqs;
    }

    private static Map<String, Long> gooduse() throws InterruptedException {
        ConcurrentHashMap<String, LongAdder> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(i -> {
                    String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                    //利用computeIfAbsent()方法来实例化LongAdder，然后利用LongAdder来进行线程安全计数
                    freqs.computeIfAbsent(key, k -> new LongAdder()).increment();
                }
        ));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        //因为我们的Value是LongAdder而不是Long，所以需要做一次转换才能返回
        return freqs.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().longValue())
                );
    }

    /**
     * testWrite testRead
     *  copyOnWriteArrayList  synchronizedList
     *  由于copyOnWriteArrayList的实现问题,如果对copyOnWriteArrayList进行大批量写操作,此时copyOnWriteArrayList
     *  会比synchronizedList性能还低 因为每次 add 时，都会用 Arrays.copyOf 创建一个新数组
     */
    //测试并发写的性能
    @Test
    public void testWrite() {
        List<Integer> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        List<Integer> synchronizedList = Collections.synchronizedList(new ArrayList<>());
        StopWatch stopWatch = new StopWatch();
        int loopCount = 100000;
        stopWatch.start("Write:copyOnWriteArrayList");
        //循环100000次并发往CopyOnWriteArrayList写入随机元素
        IntStream.rangeClosed(1, loopCount).parallel().forEach(__ -> copyOnWriteArrayList.add(ThreadLocalRandom.current().nextInt(loopCount)));
        stopWatch.stop();
        stopWatch.start("Write:synchronizedList");
        //循环100000次并发往加锁的ArrayList写入随机元素
        IntStream.rangeClosed(1, loopCount).parallel().forEach(__ -> synchronizedList.add(ThreadLocalRandom.current().nextInt(loopCount)));
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
        Map result = new HashMap();
        result.put("copyOnWriteArrayList", copyOnWriteArrayList.size());
        result.put("synchronizedList", synchronizedList.size());
    }

    //帮助方法用来填充List
    public static void addAll(List<Integer> list) {
        list.addAll(IntStream.rangeClosed(1, 1000000).boxed().collect(Collectors.toList()));
    }

    //测试并发读的性能
    @Test
    public void testRead() {
        //创建两个测试对象
        List<Integer> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        List<Integer> synchronizedList = Collections.synchronizedList(new ArrayList<>());
        //填充数据
        addAll(copyOnWriteArrayList);
        addAll(synchronizedList);
        StopWatch stopWatch = new StopWatch();
        int loopCount = 1000000;
        int count = copyOnWriteArrayList.size();
        stopWatch.start("Read:copyOnWriteArrayList");
        //循环1000000次并发从CopyOnWriteArrayList随机查询元素
        IntStream.rangeClosed(1, loopCount).parallel().forEach(__ -> copyOnWriteArrayList.get(ThreadLocalRandom.current().nextInt(count)));
        stopWatch.stop();
        stopWatch.start("Read:synchronizedList");
        //循环1000000次并发从加锁的ArrayList随机查询元素
        IntStream.range(0, loopCount).parallel().forEach(__ -> synchronizedList.get(ThreadLocalRandom.current().nextInt(count)));
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
        Map result = new HashMap();
        result.put("copyOnWriteArrayList", copyOnWriteArrayList.size());
        result.put("synchronizedList", synchronizedList.size());
    }

    public static void main(String[] args) throws Exception{
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("normaluse");
        Map<String, Long> normaluse = normaluse();
        stopWatch.stop();
        //校验元素数量
        Assert.isTrue(normaluse.size() == ITEM_COUNT, "normaluse size error");
        //校验累计总数
        Assert.isTrue(normaluse.entrySet().stream()
                        .mapToLong(item -> item.getValue()).reduce(0, Long::sum) == LOOP_COUNT
                , "normaluse count error");
        stopWatch.start("gooduse");
        Map<String, Long> gooduse = gooduse();
        stopWatch.stop();
        Assert.isTrue(gooduse.size() == ITEM_COUNT, "gooduse size error");
        Assert.isTrue(gooduse.entrySet().stream()
                        .mapToLong(item -> item.getValue())
                        .reduce(0, Long::sum) == LOOP_COUNT
                , "gooduse count error");
        log.info(stopWatch.prettyPrint());
    }
}
