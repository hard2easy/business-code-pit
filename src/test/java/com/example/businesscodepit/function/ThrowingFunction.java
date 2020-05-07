package com.example.businesscodepit.function;

import java.util.function.Function;

/**
 * 描述：
 * <p>
 * 创建时间：2020/03/23
 * 修改时间：
 *
 * @author yaoyong
 **/

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Throwable> {
    static <T, R, E extends Throwable> Function<T, R> unchecked(ThrowingFunction<T, R, E> f) {
        return t -> {
            try {
                return f.apply(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
    R apply(T t) throws E;
}
