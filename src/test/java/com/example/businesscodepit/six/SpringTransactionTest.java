package com.example.businesscodepit.six;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/06
 * 修改时间：
 *     @Transactional
 *          生效原则 1，除非特殊配置（比如使用 AspectJ 静态织入实现 AOP），否则只有定义在 public 方法上的 @Transactional 才能生效
 *          生效原则 2，必须通过代理过的类从外部调用目标方法才能生效
 *          生效原则 3，只有异常传播出了标记了 @Transactional 注解的方法，事务才能回滚
 *                     默认只捕获RuntimeException（非受检异常）或 Error,可以通过设置参数
 *                     rollbackFor = Exception.class定义回滚捕捉异常
 *          一个用户注册的操作，会插入一个主用户到用户表，还会注册一个关联的子用户。
 *          我们希望将子用户注册的数据库操作作为一个独立事务来处理，
 *          即使失败也不会影响主流程，即不影响主用户的注册
 *              一个service(主方法 标注@Transactional)调用两个子方法  一个创建主用户 一个创建子用户:
 *              1.调用子用户方法时需要捕获子方法的异常,并且子用户方法的@Transactional设置事务传播级别
 *              2.调用子用户方法，需要注意通过注入的service进行调用,要求写在两个service里面
 *                要不然只能自己注入自己的方式   否则子用户方法的@Transactional不生效(原则2)
 * @author yaoyong
 **/
public class SpringTransactionTest {
}
