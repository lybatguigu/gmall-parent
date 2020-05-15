package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-05-11 23:09
 */
//监听消息,获取那些商品是秒杀并将商品添加到缓存
@Component
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsService seckillGoodsService;

    //编写监听消息的方法,将商品添加到缓存

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importGoodsToRedis(Message message, Channel channel) {
        //获取秒杀商品?什么样的商品是秒杀商品：审核状态status=1的，startTime=new Date()当天。
        //根据上述定义查询所有的秒杀商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        //导入时间比较的工具类,时间比较只取年月日
        seckillGoodsQueryWrapper.eq("status", 1).eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        //说明有秒杀商品
        if (null != seckillGoodsList && seckillGoodsList.size() > 0) {
            //循环商品放入缓存
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                //在放入商品之前，先判断缓存是否已经存在，如果存在就不需要放了
                // hset(key,field,value)
                // 定义key=seckill:goods  field=skuId value=秒杀商品的对象
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                //当前的缓存中有秒杀商品
                if (flag) {
                    //不放如数据
                    continue;//进行下一次循环
                }
                //缓存中没有数据,秒杀的商品放入缓存
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(), seckillGoods);
                /**
                 * 商品的数量如何存储，如何防止库存超卖？
                 * 使用redis中List的数据类型，先进先出 而且redis是单线程，每次只能进来一个用户
                 */
                //剩余的库存数量getStockCount()
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    //key=seckill:stock:skuId    value= skuId
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }
                //将所有的商品的状态为都初始化为 1， 状态为只有为1 的时候这个商品才能秒杀，为0不能秒
                redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId() + ":1");
            }
        }
        //手动确认消息已被处理
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //监听用户发送过来的消息
    //rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillGoods(UserRecode userRecode, Message message, Channel channel) {
        //用户不为空
        if (null != userRecode) {
            //预下单处理
            seckillGoodsService.seckillOrder(userRecode.getUserId(), userRecode.getSkuId());
            //确认收到消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void delSeckillGoods( Message message, Channel channel) {
        //删除缓存
        //查询结束的秒杀商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        //结束时间
        seckillGoodsQueryWrapper.eq("status", 1).le("end_time", new Date());
        //获取到结束的秒杀商品
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        if (!CollectionUtils.isEmpty(seckillGoodsList)) {
            //删除缓存数据
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                //删除缓存中秒杀商品的数量
                redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId());
            }
        }

        //删除  seckill:goods 这个是存储所有秒杀商品的数据
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        //删除的 seckill:orders:users
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);

        //变更数据库的状态 status1变为2
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setStatus("2");
        seckillGoodsMapper.update(seckillGoods, seckillGoodsQueryWrapper);

        //手动确认消息被消费
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
