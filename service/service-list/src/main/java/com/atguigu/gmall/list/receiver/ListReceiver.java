package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Liyanbao
 * @create 2020-05-08 19:51
 * 监听service-product发来的消息
 */
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;
    /*
    商品的上架调用upperGoods()方法.
    rabbitMq 监听消息可使用注解
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS, durable = "true", type = ExchangeTypes.DIRECT),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel) {
        //判断skuId不为空
        if (null != skuId) {
            //有skuId则调用商品的上架操作，将数据从mysql上传到es
            searchService.upperGoods(skuId);
        }
        //手动签收确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 监听商品的下架
     * @param skuId 商品id
     * @param message 消息
     * @param channel 管道
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS, durable = "true", type = ExchangeTypes.DIRECT),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lowerGoods(Long skuId, Message message, Channel channel) {
        //判断skuId不为空
        if (null != skuId) {
            //有skuId则调用商品的上架操作，将数据从mysql上传到es
            searchService.lowerGoods(skuId);
        }
        //手动签收确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
