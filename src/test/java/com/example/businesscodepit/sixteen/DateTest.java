package com.example.businesscodepit.sixteen;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

/**
 * 描述：上海时间比纽约时间快13小时  参看testDate的运行结果
 * <p>
 * 创建时间：2020/05/14
 * 修改时间：
 *      1.初始化日期时间
 *      2.“恼人”的时区问题
 *
 *      在把 Date 转换为 LocalDateTime 的时候，
 *          需要通过 Date 的 toInstant 方法得到一个 UTC 时间戳进行转换，
 *          并需要提供当前的时区，这样才能把 UTC 时间转换为本地日期时间（的表示）。
 *          反过来，把 LocalDateTime 的时间表示转换为 Date 时，也需要提供时区，
 *          用于指定是哪个时区的时间表示，
 *          也就是先通过 atZone 方法把 LocalDateTime 转换为 ZonedDateTime，
 *          然后才能获得 UTC 时间戳
 *      3.多次强调 Date 是一个时间戳，是 UTC 时间、没有时区概念，
 *          为什么调用其 toString 方法会输出类似 CST 之类的时区字样呢？
 *
 *          Date#toString方法中，会将当前时间转化为BaseCalendar.Date类，
 *              这个类有一个Zone属性，在toString的时候会被追加到字符串中（默认是GMT）
 *
 *      4.MySQL 中有两种数据类型 datetime 和 timestamp 可以用来保存日期时间。
 *          你能说说它们的区别吗，它们是否包含时区信息呢
 *              datetime占用8字节，不受时区影响，表示范围'1000-01-01 00:00:00' to '9999-12-31 23:59:59'
 * timestamp占用4字节，受时区影响，表示范围'1970-01-01 00:00:01' to '2038-01-19 03:14:07'，若插入null会自动转化为当前时间
 * @author yaoyong
 **/
public class DateTest {
    private Logger logger = LoggerFactory.getLogger(DateTest.class);

    @Test
    public void test1() {
        //你会说这是新手才会犯的低级错误：年应该是和 1900 的差值，
        // 月应该是从 0 到 11 而不是从 1 到 12
        Date date = new Date(2019, 12, 31, 11, 12, 13);
        System.out.println(date);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2019, 11, 31, 11, 12, 13);
        System.out.println(calendar.getTime());
        Calendar calendar2 = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        //指定纽约时间对应的本地时间
        calendar2.set(2019, Calendar.DECEMBER, 31, 11, 12, 13);
        System.out.println(calendar2.getTime());
    }

    /**
     * 一是，Date 并无时区问题，世界上任何一台计算机使用 new Date()
     * 初始化得到的时间都一样。
     * 因为，Date 中保存的是 UTC 时间，UTC 是以原子钟为基础的统一时间，
     * 不以太阳参照计时，并无时区划分。
     * 二是，Date 中保存的是一个时间戳，代表的是从 1970 年 1 月 1 日 0 点
     * （Epoch 时间）到现在的毫秒数。尝试输出 Date(0)：
     */
    @Test
    public void testDate() {
        // Thu Jan 01 08:00:00 CST 1970
        System.out.println(new Date(0));
        // Asia/Shanghai:8 时区相差8小时
        System.out.println(TimeZone.getDefault().getID() + ":" + TimeZone.getDefault().getRawOffset() / 3600000);
        System.out.println(TimeZone.getTimeZone("America/New_York").getID() + ":" + TimeZone.getTimeZone("America/New_York").getRawOffset() / 3600000);
    }

    /**
     * 方式一，以 UTC 保存，保存的时间没有时区属性，
     *      是不涉及时区时间差问题的世界统一时间。
     *      我们通常说的时间戳，或 Java 中的 Date 类就是用的这种方式，
     *      这也是推荐的方式。
     * 方式二，以字面量保存，比如年 / 月 / 日 时: 分: 秒，
     *      一定要同时保存时区信息。只有有了时区信息，
     *      我们才能知道这个字面量时间真正的时间点，
     *      否则它只是一个给人看的时间表示，
     *      只在当前时区有意义。
     *      Calendar 是有时区概念的，
     *      所以我们通过不同的时区初始化 Calendar，得到了不同的时间
     */
    @Test
    public void testInternationalDate() throws ParseException {
        String stringDate = "2020-01-02 22:00:00";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //默认时区解析时间表示
        Date date1 = inputFormat.parse(stringDate);
        System.out.println(date1 + ":" + date1.getTime());
        //纽约时区解析时间表示
        inputFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Date date2 = inputFormat.parse(stringDate);
        System.out.println(date2 + ":" + date2.getTime());
    }
    @Test
    public void testInternationalDate2() throws ParseException {
        String stringDate = "2020-01-02 22:00:00";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //同一Date
        Date date = inputFormat.parse(stringDate);
        //默认时区格式化输出：
        System.out.println(new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss Z]").format(date));
        //纽约时区格式化输出
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        System.out.println(new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss Z]").format(date));
    }

    /**
     * Java 8 推出了新的时间日期类 ZoneId、ZoneOffset、LocalDateTime、ZonedDateTime 和 DateTimeFormatter，
     * 处理时区问题更简单清晰。我们再用这些类配合一个完整的例子，来理解一下时间的解析和展示：
     *      首先初始化上海、纽约和东京三个时区。我们可以使用 ZoneId.of 来初始化一个标准的时区，
     *          也可以使用 ZoneOffset.ofHours 通过一个 offset，来初始化一个具有指定时间差的自定义时区。
     *      对于日期时间表示，LocalDateTime 不带有时区属性，所以命名为本地时区的日期时间；
     *      而 ZonedDateTime=LocalDateTime+ZoneId，具有时区属性。
     *      因此，LocalDateTime 只能认为是一个时间表示，ZonedDateTime 才是一个有效的时间。
     *      在这里我们把 2020-01-02 22:00:00 这个时间表示，使用东京时区来解析得到一个 ZonedDateTime。
     *      使用 DateTimeFormatter 格式化时间的时候，可以直接通过 withZone 方法直接设置格式化使用的时区。最
     *      后，分别以上海、纽约和东京三个时区来格式化这个时间输出
     */
    @Test
    public void testJava8Date(){
        //一个时间表示
        String stringDate = "2020-01-02 22:00:00";
        //初始化三个时区
        ZoneId timeZoneSH = ZoneId.of("Asia/Shanghai");
        ZoneId timeZoneNY = ZoneId.of("America/New_York");
        ZoneId timeZoneJST = ZoneOffset.ofHours(9);
        //格式化器
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        //申明 stringDate 是建立在比UTC快9小时的基础上得到的基于时区的值
        ZonedDateTime date = ZonedDateTime.of(LocalDateTime.parse(stringDate, dateTimeFormatter), timeZoneJST);
        //使用DateTimeFormatter格式化时间，可以通过withZone方法直接设置格式化使用的时区
        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
        System.out.println(timeZoneSH.getId() + outputFormat.withZone(timeZoneSH).format(date));
        System.out.println(timeZoneNY.getId() + outputFormat.withZone(timeZoneNY).format(date));
        System.out.println(timeZoneJST.getId() + outputFormat.withZone(timeZoneJST).format(date));
    }
    @Test
    public void testSimpleDateFormat(){
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        System.out.println("defaultLocale:" + Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(2019, Calendar.DECEMBER, 29,0,0,0);
        //小写 y 是年，而大写 Y 是 week year，也就是所在的周属于哪一年
        //一周从周日开始，周六结束，(只要本周跨年，那么这周就算入下一年 12-29)
        SimpleDateFormat YYYY = new SimpleDateFormat("YYYY-MM-dd");
        System.out.println("格式化: " + YYYY.format(calendar.getTime()));
        System.out.println("weekYear:" + calendar.getWeekYear());
        System.out.println("firstDayOfWeek:" + calendar.getFirstDayOfWeek());
        System.out.println("minimalDaysInFirstWeek:" + calendar.getMinimalDaysInFirstWeek());
    }

    /**
     * simpleDateFormat 线程不安全,多个线程操作一个simpleDateFormat 会导致线程安全问题
     *    SimpleDateFormat 继承了 DateFormat，
     *    DateFormat 有一个字段 Calendar；
     *    SimpleDateFormat 的 parse 方法调用 CalendarBuilder 的 establish 方法，来构建 Calendar；
     *    establish 方法内部先清空 Calendar 再构建 Calendar，整个操作没有加锁
     * 如果非要使用同一个SimpleDateFormat 可以使用如下方法 绑定线程
     *  private static ThreadLocal<SimpleDateFormat> threadSafeSimpleDateFormat
     *      = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
     */
    @Test
    public void testThreadFormatDate() throws InterruptedException {
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ThreadLocal<SimpleDateFormat> threadSafeSimpleDateFormat
            = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        ExecutorService threadPool = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 20; i++) {
            //提交20个并发解析时间的任务到线程池，模拟并发环境
            threadPool.execute(() -> {
                for (int j = 0; j < 10; j++) {
                    try {
                        System.out.println(threadSafeSimpleDateFormat.get().parse("2020-01-01 11:12:13"));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);
    }
    @Test
    public void testDateCal(){
        //1. 错误示例,希望得到当前时间之后 30 天的时间 通过计算获取时间 但是最终算出来的结果是不对的  因为会发生int溢出
        //  修复将30写成30L 使用long进行计算
        Date today = new Date();
        Date nextMonth = new Date(today.getTime() + 30 * 1000 * 60 * 60 * 24);
        System.out.println(today);
        System.out.println(nextMonth);

        //2 java8之前的正确写法
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DAY_OF_MONTH, 30);
        System.out.println(c.getTime());

        //3.java8时间计算
        LocalDateTime localDateTime = LocalDateTime.now();
        System.out.println(localDateTime.plusDays(30));

        System.out.println("//测试操作日期");
        System.out.println(LocalDate.now()
                .minus(Period.ofDays(1))     //减一天
                .plus(1, ChronoUnit.DAYS)//加一天
                .minusMonths(1)  //减一个月
                .plus(Period.ofMonths(1)));  //加一个月


        System.out.println("//本月的第一天");
        System.out.println(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));

        System.out.println("//今年的程序员日");
        System.out.println(LocalDate.now().with(TemporalAdjusters.firstDayOfYear()).plusDays(255));

        System.out.println("//今天之前的一个周六");
        System.out.println(LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SATURDAY)));

        System.out.println("//本月最后一个工作日");
        System.out.println(LocalDate.now().with(TemporalAdjusters.lastInMonth(DayOfWeek.FRIDAY)));

        System.out.println(LocalDate.now().with(temporal -> temporal.plus(ThreadLocalRandom.current().nextInt(100), ChronoUnit.DAYS)));


        System.out.println("//查询是否是今天要举办生日");
        System.out.println(LocalDate.now().query(DateTest::isFamilyBirthday));
    }

    public static Boolean isFamilyBirthday(TemporalAccessor date) {
        int month = date.get(MONTH_OF_YEAR);
        int day = date.get(DAY_OF_MONTH);
        if (month == Month.FEBRUARY.getValue() && day == 17)
            return Boolean.TRUE;
        if (month == Month.SEPTEMBER.getValue() && day == 21)
            return Boolean.TRUE;
        if (month == Month.MAY.getValue() && day == 22)
            return Boolean.TRUE;
        return Boolean.FALSE;
    }
    /**
     * Java 8 中有一个专门的类 Period 定义了日期间隔，通过 Period.between 得到了两个 LocalDate 的差，
     *  返回的是两个日期差几年零几月零几天。
     *  如果希望得知两个日期之间差几天，直接调用 Period 的 getDays() 方法得到的只是最后的“零几天”，而不是算总的间隔天数
     */
    @Test
    public void testJava8DatePit(){
        System.out.println("//计算日期差");
        LocalDate today = LocalDate.of(2019, 12, 12);
        LocalDate specifyDate = LocalDate.of(2019, 10, 1);
        System.out.println(Period.between(specifyDate, today).getDays());
        System.out.println(Period.between(specifyDate, today));
        System.out.println(ChronoUnit.DAYS.between(specifyDate, today));
    }
}
