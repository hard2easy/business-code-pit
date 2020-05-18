package com.example.businesscodepit.eighteen.annotation;

import java.lang.annotation.*;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/18
 * 修改时间：
 *
 * @author yaoyong
 **/
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited  //标注注解可以被继承
public @interface MyAnnotation {
    String value();
}
