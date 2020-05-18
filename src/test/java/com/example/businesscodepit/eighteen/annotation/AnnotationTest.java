package com.example.businesscodepit.eighteen.annotation;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/18
 * 修改时间：
 *
 * @author yaoyong
 **/
@Slf4j
public class AnnotationTest {

    private String getAnnotationValue(MyAnnotation annotation) {
        if (annotation == null) return "";
        return annotation.value();
    }

    /**
     * 默认情况下注解不可以被继承,如果需要被继承 可以使用@Inherited注解
     *   但是@Inherited 只能实现类上的注解  因此ChildMethod输出的是空,无法拿到方法上的注解
     *   如果需要拿到父方法的注解,你可以通过反射在继承链上找到方法上的注解。但，这样实现起来很繁琐，而且需要考虑桥接方法
     *      可以直接使用spring提供的AnnotatedElementUtils.findMergedAnnotation
     * @throws NoSuchMethodException
     */
    @Test
    public  void wrong() throws NoSuchMethodException {
        //获取父类的类和方法上的注解
        Parent parent = new Parent();
        log.info("ParentClass:{}", getAnnotationValue(parent.getClass().getAnnotation(MyAnnotation.class)));
        log.info("ParentMethod:{}", getAnnotationValue(parent.getClass().getMethod("foo").getAnnotation(MyAnnotation.class)));

        //获取子类的类和方法上的注解
        Child child = new Child();
        log.info("ChildClass:{}", getAnnotationValue(child.getClass().getAnnotation(MyAnnotation.class)));
        log.info("ChildMethod:{}", getAnnotationValue(child.getClass().getMethod("foo").getAnnotation(MyAnnotation.class)));

        Child child1 = new Child();
        log.info("ChildClass:{}", getAnnotationValue(AnnotatedElementUtils.findMergedAnnotation(child1.getClass(), MyAnnotation.class)));
        log.info("ChildMethod:{}", getAnnotationValue(AnnotatedElementUtils.findMergedAnnotation(child1.getClass().getMethod("foo"), MyAnnotation.class)));
    }

    @MyAnnotation(value = "Class")
    @Slf4j
    static class Parent {
        @MyAnnotation(value = "Method")
        public void foo() {
        }
    }

    @Slf4j
    static class Child extends Parent {
        @Override
        public void foo() {
        }
    }
}
