package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Liyanbao
 * @create 2020-05-12 0:21
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    //查询所有秒杀商品
    @Override
    public List<SeckillGoods> findAll() {
        //每天夜晚扫描，然后发送消息，消费消息将数据放入缓存
        return redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
    }

    //根据skuId查询商品详情
    @Override
    public SeckillGoods getSeckillGoodsById(Long skuId) {
        return (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
    }

    //预下单处理
    @Override
    public void seckillOrder(String userId, Long skuId) {
        /**
         * 1.监听用户，同一个用户不能抢两次
         * 2.判断状态位
         * 3.监听库存的数量 ，在缓存中redis-list
         * 4.将用户秒杀的记录放入缓存中
         */
        //判断状态位
        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)) {
            //说明没有商品了
            return;
        }
        //用户不能抢两次
        //如果用户第一次抢到了，那么就会将抢到的信息存储在缓存中，利用redis的setnx()判断这个key是否存在
        //得到key = seckill:user:userId
        String userseckillKey = RedisConst.SECKILL_USER+userId;
        //如果执行成功返回true，说明第一次添加key，false执行失败，可能用户不是第一次添加key
        Boolean isExist = redisTemplate.opsForValue().setIfAbsent(userseckillKey, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        //判断存在不存在
        if (!isExist) {
            return;
        }
        //走到这说明用户可以下单了,减少库存  key=seckill:stock:skuId
        //redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(goodsId)) {
            //如果没有吐出来 说明没有拿到数据,就是已经售完货了
            //通知其他的兄弟节点，当前的商品没有了，更新内存的状态位
            redisTemplate.convertAndSend("seckillpush", skuId + ":0");
            return;
        }
        //记录订单,做一个秒杀的订单类
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userId);
        //根据skuId，查询秒杀的那个商品
        orderRecode.setSeckillGoods(getSeckillGoodsById(skuId));
        orderRecode.setNum(1);
        orderRecode.setOrderStr(MD5.encrypt(userId));
        //将用户秒杀的订单放入缓存 key=seckill:orders
        String orderSeckillKey = RedisConst.SECKILL_ORDERS;
        redisTemplate.boundHashOps(orderSeckillKey).put(orderRecode.getUserId(), orderRecode);
        //更新商品的数量
        updateStockCount(orderRecode.getSeckillGoods().getSkuId());
    }

    //更新商品的数量
    private void updateStockCount(Long skuId) {
        //秒杀商品的库存，在缓存中和数据库中各有一份
        Long count = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //主要目的为了不想频繁更新数据库
        if (count % 2 == 0) {
            //获取缓存中当前的秒杀商品
            SeckillGoods seckillGoods = getSeckillGoodsById(skuId);
            seckillGoods.setStockCount(count.intValue());
            //更新数据库
            seckillGoodsMapper.updateById(seckillGoods);
            //更新缓存
            //key=seckill:goods
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(skuId.toString(), seckillGoods);
        }
    }

    //检查订单
    @Override
    public Result checkOrder(Long skuId, String userId) {
        //判断用户是否存在，不能购买两次
        //用户是否能够抢单
        Boolean isExist = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if (isExist) {
           //返回true说明第一次购买,存储用户  seckill:user:userId
            //判断用户的订单是否存在
            Boolean isHasKey = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if (isHasKey) {
                //抢单成功,获取用户订单对象
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                //返回数据,抢单成功
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        //判断用户是否下过订单
        Boolean isExistOrder = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        //如果返回true 执行成功
        if (isExistOrder) {
            //获取订单id
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            //第一次下单成功
            return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        //判断状态位
        String status = (String) CacheHelper.get(skuId.toString());
        //status = 1 表示可以抢单，0售完
        if ("0".equals(status)) {
            return Result.build(null,ResultCodeEnum.SECKILL_FINISH);
        }

        //默认情况下
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }

}
