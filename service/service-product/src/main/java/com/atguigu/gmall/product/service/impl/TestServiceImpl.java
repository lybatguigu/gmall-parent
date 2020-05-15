package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Liyanbao
 * @create 2020-04-24 0:39
 */
@Service
public class TestServiceImpl implements TestService {


    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public  void testLock() {

        //创建锁
        String skuId = "25";
        String lockKey = "lock" + skuId;
        //锁的每个商品
        RLock lock = redissonClient.getLock("lockKey");
        //加锁
        lock.lock(10,TimeUnit.SECONDS);
        //最多等待时间
//        boolean flag = false;
//        try {
//            flag = lock.tryLock(100, 10, TimeUnit.SECONDS);
//            if (flag) {
//                //业务逻辑代码
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }finally {
//            lock.unlock();
//        }
        //业务逻辑代码
        //获取数据
        String value = redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(value)) {
            return;
        }
        //将value变为int
        int num = Integer.parseInt(value);
        //将num+1放入缓存
        redisTemplate.opsForValue().set("num", String.valueOf(++num));
        //解锁
        lock.unlock();
    }

    //读锁
    @Override
    public String readLock() {
        //获取读写锁对象
        RReadWriteLock readwriteLock = redissonClient.getReadWriteLock("readwriteLock");
        //获取读锁
        RLock rLock = readwriteLock.readLock();
        //加锁
        rLock.lock(10, TimeUnit.SECONDS);
        //读取缓存的数据
        String msg = redisTemplate.opsForValue().get("msg");
        return msg;
    }

    //写锁
    @Override
    public String writeLock() {
        //获取读写锁对象
        RReadWriteLock readwriteLock = redissonClient.getReadWriteLock("readwriteLock");
        RLock rLock = readwriteLock.writeLock();
        rLock.lock(10, TimeUnit.SECONDS);
        //写入数据
        redisTemplate.opsForValue().set("msg",UUID.randomUUID().toString());
        return "写入数据完成....";
    }

//    //synchronized 就不用了
//    @Override
//    public  void testLock() {
//
//        //声明一个uuid,将uuid作为一个value放入key所对应的值中
//        String uuid = UUID.randomUUID().toString();
//        String skuId = "25";
//        String lockKey = "lock" + skuId;
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid,3,TimeUnit.SECONDS);
//        //redisTemplate.expire("lock",10, TimeUnit.SECONDS);//设置过期的时间
//        if (lock) {
//            //执行的业务逻辑开始
//
//            // 查询redis中的num值
//            String value = redisTemplate.opsForValue().get("num");
//            // 没有该值return
//            if (StringUtils.isBlank(value)) {
//                return;
//            }
//            // 有值就转成成int
//            int num = Integer.parseInt(value);
//            // 把redis中的num值+1
//            redisTemplate.opsForValue().set("num", String.valueOf(++num));
//            //判断当前线程key所对应的value是不是自己的值
////            if (uuid.equals(redisTemplate.opsForValue().get("lock"))) {
////                //执行的业务逻辑完成
////                //删除锁
////                redisTemplate.delete("lock");
////            }
//            //使用Lua脚本锁
//            //定义Lua脚本
//            String script="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//            //使用redis执行lua
//            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//            redisScript.setScriptText(script);
//            //设置返回值类型
//            redisScript.setResultType(Long.class);
//            //第一个是script脚本，第二个：需要判断的key，第三个是key所对应的值
//            redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
//
//
//        } else {
//            //其他线程等待
//            try {
//                Thread.sleep(1000);
//                //睡醒之后再次调用这个方法
//                testLock();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//
//    }
}
