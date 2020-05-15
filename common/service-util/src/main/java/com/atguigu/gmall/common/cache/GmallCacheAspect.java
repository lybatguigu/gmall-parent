package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Liyanbao
 * @create 2020-04-25 18:48
 */
//利用aop来实现缓存
@Component
@Aspect
public class GmallCacheAspect {
    //引入redis，redissonClient
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    //编写方法，利用环绕通知来获取对应的数据
    //模拟@Transactional注解
    //返回值类型Object 为啥是Object？因为我们在切方法的时候不能确定方法的返回值到底是什么。
    //例如：SkuInfo getSkuInfo(Long skuId) 返回的是SkuInfo，以后还要做其他的。
    //例如：BigDecimal getSkuPrice(Long skuId)这个方法返回的就是 BigDecimal
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point) {
        //声明一个Object
        Object result = null;
        //http://item.gmall.com/16.html 前缀定义的:prefix = RedisConst.SKUKEY_PREFIX = sku:
        //以后缓存key的形式 sku:[16] sku:[skuId]
        //获取传递过来的参数
        Object[] args = point.getArgs();
        //获取方法上的签名，我们如何知道方法上是否有注解,通过方法上签名判断方法上是否有注解
        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        //得到注解
        GmallCache gmallCache = methodSignature.getMethod().getAnnotation(GmallCache.class);
        //获取缓存的前缀
        String prefix = gmallCache.prefix();
        //组成缓存的key
        String key = prefix + Arrays.asList(args).toString();
        //从缓存中获取数据
        result = cacheHit(key,methodSignature);
        //判断,不为空缓存中有数据
        if (result != null) {
            return result;
        }
        //缓存没有数据
        //加分布式锁
        RLock lock = redissonClient.getLock(key + ":lock");
        try {
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
            if (res) {
                //获取到分布式锁,从数据库中获取数据
                //如果访问的getSkuInfoDB方法，那么它相当于调用skuInfoDB
                result = point.proceed(point.getArgs());
                //判断result是否为空,第一次访问如果数据库里边没有
                //防止缓存穿透
                if (null == result) {
                    Object o = new Object();
                    redisTemplate.opsForValue().set(key, JSONObject.toJSONString(o), RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                    return o;
                }
                redisTemplate.opsForValue().set(key, JSONObject.toJSONString(result), RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                return result;
            } else {
                Thread.sleep(1000);
                //自己调用自己，自旋
                return cacheAroundAdvice(point);
                //直接获取缓存数据
                //return cacheHit(key, methodSignature);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }finally {
            //因为我们放入数据的时候，放的是Object，我们不能确保Object中是否有id属性，
            //如果缓存的是getSkuInfoDB()这个方法的返回值是SkuInfo，对于这个方法就有id，可以判断
            //但是我们缓存其他的方法，getPrice()返回的是BigDecimal，对于这个方法就没有id，不可判断
            //解锁
            lock.unlock();
        }
        return result;
    }

    //从缓存获取数据
    private Object cacheHit(String key, MethodSignature methodSignature) {
        //必须有key,存入缓存时数据时一个Object类型，但是我们可以看做是一个字符串
        String cache = (String) redisTemplate.opsForValue().get(key);
        //判断当前的字符串是否有值
        if (StringUtils.isNotBlank(cache)) {
            //不为空,字符串是项目中需要的哪种数据类型,需要确定,判断方法的返回值类型是什么,那么缓存存储的就是什么类型
            Class returnType = methodSignature.getReturnType();
            //需要将cache字符串转化为方法的返回值类型
            return JSONObject.parseObject(cache, returnType);
        }
        return null;
    }
}
