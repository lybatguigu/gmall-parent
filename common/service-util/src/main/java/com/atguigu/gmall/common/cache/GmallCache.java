package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Liyanbao
 * @create 2020-04-25 18:35
 */
@Target(ElementType.METHOD)//表示该注解使用在方法上
@Retention(RetentionPolicy.RUNTIME)//注解的生命周期，这个是最大的
public @interface GmallCache {

    //缓存的前缀
    //定义一个字段，作为缓存的key来使用的  以后做缓存时，缓存的key可以由sku:组成
    //sku:是缓存key的一部分。
    String prefix() default "cache";
}
