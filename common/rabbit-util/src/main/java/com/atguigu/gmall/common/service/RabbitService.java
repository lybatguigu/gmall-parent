package com.atguigu.gmall.common.service;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Liyanbao
 * @create 2020-05-08 12:50
 */
@Service
public class RabbitService {

    //发送消息引入消息模板
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //编写发送消息的方法

    /**
     * 发送非延迟消息
     * @param exchange 发送到那个交换机
     * @param routingKey 交换机绑定到那个队列
     * @param message 发送的消息内容
     * @return
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {
        //发送消息
        rabbitTemplate.convertAndSend(exchange,routingKey,message);
        return true;
    }

    /**
     * 发送延迟消息
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param message 消息
     * @param delayTime 延迟的时间
     * @return
     */
    public boolean sendDelayMessage(String exchange, String routingKey, Object message, int delayTime) {
        //设置发送消息
        rabbitTemplate.convertAndSend(exchange, routingKey, message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                //设置延迟时间 单位毫秒
                message.getMessageProperties().setDelay(delayTime*1000);
                return message;
            }
        });
        return true;
    }

}
