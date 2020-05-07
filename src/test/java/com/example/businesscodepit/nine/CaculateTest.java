package com.example.businesscodepit.nine;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/07
 * 修改时间：
 *    在MySQL中，整数和浮点数的定义都是有多种类型，整数根据实际范围定义，浮点数语言指定整体长度和小数长度。
 *    浮点数类型包括单精度浮点数（float型）和双精度浮点数（double型）。定点数类型就是decimal型。
 *    定点数以字符串形式存储，因此，其精度比浮点数要高，而且浮点数会出现误差，这是浮点数一直存在的缺陷。
 *    如果要对数据的精度要求比较高，还是选择定点数decimal比较安全
 * @author yaoyong
 **/
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class CaculateTest {
    private static Logger log = LoggerFactory.getLogger(CaculateTest.class);
    @Test
    public void test1(){
        System.out.println(0.1+0.2);
        System.out.println(1.0-0.8);
        System.out.println(4.015*100);
        System.out.println(123.3/100);

        double amount1 = 2.15;
        double amount2 = 1.10;
        if (amount1 - amount2 == 1.05)
            System.out.println("OK");
    }

    /**
     * 使用字符串的构造方法进行BigDecimal构造才可以避免精度问题
     */
    @Test
    public void test2(){
        System.out.println(new BigDecimal(0.1).add(new BigDecimal(0.2)));
        System.out.println(new BigDecimal(1.0).subtract(new BigDecimal(0.8)));
        System.out.println(new BigDecimal(4.015).multiply(new BigDecimal(100)));
        System.out.println(new BigDecimal(123.3).divide(new BigDecimal(100)));

        System.out.println(new BigDecimal("0.1").add(new BigDecimal("0.2")));
        System.out.println(new BigDecimal("1.0").subtract(new BigDecimal("0.8")));
        System.out.println(new BigDecimal("4.015").multiply(new BigDecimal("100")));
        System.out.println(new BigDecimal("123.3").divide(new BigDecimal("100")));
    }

    /**
     * 小数位与精度
     *      scale   precision
     */
    @Test
    public void test3(){
        //通过该构造方法获得的BigDecimal的scale=1 precision=4
        BigDecimal a = new BigDecimal(Double.toString(100));
        //new BigDecimal("4.015") 显然scale是3   再进行乘法时  结果的scale是取两个数的scale之和   因此输出的结果是401.5000
        System.out.println(new BigDecimal("4.015").multiply(new BigDecimal(Double.toString(100))));
    }

    /**
     * 通过double创建的BigDecimal  scale是一,如果还double不带小数点的话
     */
    @Test
    public void testScale(){
        BigDecimal bigDecimal1 = new BigDecimal("100");
        System.out.println(String.valueOf(100d));//输出100.0
        System.out.println(String.valueOf(100.20d));//输出100.2
        System.out.println(String.valueOf(100.20));//输出100.2
        System.out.println(String.valueOf(100.21));//输出100.21     相应的BigDecimal的scale也会随之变化
        BigDecimal bigDecimal2 = new BigDecimal(String.valueOf(100d));
        BigDecimal bigDecimal3 = new BigDecimal(String.valueOf(100));
        BigDecimal bigDecimal4 = BigDecimal.valueOf(100.34d);
        BigDecimal bigDecimal5 = new BigDecimal(Double.toString(100.34));
        print(bigDecimal1); //scale 0 precision 3 result 401.500
        print(bigDecimal2); //scale 1 precision 4 result 401.5000
        print(bigDecimal3); //scale 0 precision 3 result 401.500
        print(bigDecimal4); //scale 1 precision 4 result 401.5000
        print(bigDecimal5); //scale 1 precision 4 result 401.5000
    }

    private static void print(BigDecimal bigDecimal) {
        log.info("scale {} precision {} result {}", bigDecimal.scale(), bigDecimal.precision(), bigDecimal.multiply(new BigDecimal("4.015")));
    }

    /**
     * 精度
     */
    @Test
    public void testJindu(){
        double num1 = 3.35;
        float num2 = 3.35f;
        System.out.println(num1 == 3.35);
        System.out.println(num2 == 3.35);
        System.out.println(String.format("%.1f", num1));//四舍五入  3.4
        System.out.println(String.format("%.1f", num2));//   3.3  num2在获取的时候实际上3.349xxx 四舍五入时取4直接被舍弃


        double num3 = 3.35;
        float num4 = 3.35f;
        DecimalFormat format = new DecimalFormat("#.##");
        format.setRoundingMode(RoundingMode.DOWN);
        System.out.println(format.format(num3));
        format.setRoundingMode(RoundingMode.DOWN);
        System.out.println(format.format(num4));

        /**
         * 需要格式化成BigDecimal进行格式化取值
         */
        BigDecimal num5 = new BigDecimal("3.35");
        BigDecimal num6 = num5.setScale(1, BigDecimal.ROUND_DOWN);
        System.out.println(num6);
        BigDecimal num7 = num5.setScale(1, BigDecimal.ROUND_HALF_UP);
        System.out.println(num7);
        System.out.println(String.format("%.1f", num5));//四舍五入  3.4
    }

    /**
     * BigDecimal的equals方法还要求scale相同  如果只希望比数值   使用compareTo
     * 如果set中存储的是BigDecimal的话,那么推荐使用TreeSet 而不是  HashSet
     *      因为TreeSet使用compareTo进行验重,如果必须使用HashSet的话,如下:先通过stripTrailingZeros将尾部的0抹除,使用最终的scale是一致的
     */
    @Test
    public void testEquals(){
        System.out.println(new BigDecimal("1.0").equals(new BigDecimal("1")));


        Set<BigDecimal> hashSet2 = new HashSet<>();
        hashSet2.add(new BigDecimal("1.0").stripTrailingZeros());
        System.out.println(hashSet2.contains(new BigDecimal("1.000").stripTrailingZeros()));//返回true
    }

    /**
     * 溢出问题
     */
    @Test
    public void testYiChu(){
        //得到的结果为 Long的最小值   -9223372036854775808
        long l = Long.MAX_VALUE;
        System.out.println(l + 1);
        System.out.println(l + 1 == Long.MIN_VALUE);

        //抛出 ArithmeticException: long overflow
        try {
            long l1 = Long.MAX_VALUE;
            System.out.println(Math.addExact(l1, 1));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //可以通过add进行正常计算  只是当使用longValueExact等进行转换的时候会报错 溢出
        BigInteger i = new BigInteger(String.valueOf(Long.MAX_VALUE));
        System.out.println(i);
        System.out.println(i.add(BigInteger.ONE).toString());
        //ArithmeticException: BigInteger out of long range
        try {
            long l3 = i.add(BigInteger.ONE).longValueExact();
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }
}
