package com.example.businesscodepit.nineteen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/18
 * 修改时间：
 *      spring ioc aop
 * @author yaoyong
 **/
@RestController
@Slf4j
@RequestMapping("spring")
public class SpringTestController {
    /**
     * 批量注入  将符合类型的service全部注入 放到List中
     *    存在一个问题:一个SayBye 一个syeHello都会放到List中,那么到底谁先
     *    注入 谁后面注入？
     */
    @Autowired
    List<SayService> sayServiceList;
    @Autowired
    private UserService userService;

    /**
     * SayService是有状态的 将List<SayService>进行注入,会发现每次调用其sayBye都是一样的
     * sayService也是一样的,如果需要每次调用注入的service是不同的需要使用如下语句注解service
     *   仅使用@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE) 是不够的
     *   因为controller是单例的,也就意味着成员变量就会实例化一次,因此不管service是否单例
     *   因为只调用一次,因此每次请求打印的对象都是一样的
     *   @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
     */
    @GetMapping("test")
    public void test() {
        log.info("====================");
        sayServiceList.forEach(SayService::say);
    }

    /**
     * 自定义切面导致事务失效  无法回滚
     *
     *      在介绍数据库事务时，我们分析了 Spring 通过 TransactionAspectSupport 类
     *          实现事务。在 invokeWithinTransaction 方法中设置断点可以发现，
     *          在执行 Service 的 createUser 方法时，TransactionAspectSupport
     *          并没有捕获到异常，所以自然无法回滚事务。原因就是:
     *          异常被 MetricsAspect 吃掉了
     *
     *       因为 Spring 的事务管理也是基于 AOP 的，默认情况下
     *          优先级最低也就是会先执行出操作，但是自定义切面 MetricsAspect
     *          也同样是最低优先级，这个时候就可能出现问题：
     *              如果出操作先执行捕获了异常，那么 Spring 的事务处理就会
     *              因为无法捕获到异常导致无法回滚事务。
     *       出现原因(
     *              1、实现Controller 的自动打点，不要自动记录入参和出参日志，否则日志量太大
     *              2、对于 Service 中的方法，最好可以自动捕获异常):
     *       原因方案实现:
     *             为 SpringTestController 手动加上了 @Metrics 注解，
     *             设置 logParameters 和 logReturn 为 false；
     *             然后为 Service 中的 createUser 方法的 @Metrics 注解，
     *            设置了 ignoreException 属性为 true(由于为true可以查看自定义的aspect
     *            如果为true的话 该切面不会抛出异常)
     *            由于自定义切面与spring aop事务切面的优先级都是最低级(优先级越低 around after after两个执行顺序也高),
     *            因此如果切面先执行(切面的around after中 如果ignoreException=true直接返回默认值不会抛出异常
     *            导致spring事务aop接收不到异常信息 导致事务无法回滚)
     *       解决方式是，明确 MetricsAspect 的优先级，可以设置为最高优先级，
     *          也就是最先执行入操作最后执行出操作
     * @return
     */
    @GetMapping("testAOP")
    public int  testAOP() {
        try {
            userService.createUser();
        } catch (Exception ex) {
            log.error("create user failed because {}", ex.getMessage());
        }
        return userService.getUserCount("1");
    }
}


