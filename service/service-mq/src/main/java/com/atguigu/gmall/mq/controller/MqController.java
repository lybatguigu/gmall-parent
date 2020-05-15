package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Liyanbao
 * @create 2020-05-08 12:55
 */
@RestController
@RequestMapping("/mq")
public class MqController {

    //调用RabbitService.sendMessage()方法
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //发送消息的控制器
    @GetMapping("sendConfirm")
    public Result sendConfirm() {
        String message = "hello RabbitMq!!!";
        rabbitService.sendMessage("exchange.confirm", "routing.confirm", message);
        return Result.ok();
    }

//    @GetMapping("sendDeadLettle")
//    public Result sendDeadLettle() {
//        //声明一个时间格式对象
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        //发送一个消息helloihao，给当期的消息设置一个TTL
//        rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead, DeadLetterMqConfig.routing_dead_1,
//                "hellonihao", message -> {
//                    //定义发送的内容以及消息的TTL,消息的存活时间10秒
//                    message.getMessageProperties().setExpiration(1000 * 10 + "");
//                    System.out.println(simpleDateFormat.format(new Date()) + "Delay sent.");
//                    return message;
//                });
//        return Result.ok();
//    }

    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle() {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead, DeadLetterMqConfig.routing_dead_1, "atguigu");
        System.out.println(simpleDateFormat.format(new Date()) + "Delay sent.");
        return Result.ok();
    }

    @GetMapping("sendDelay")
    public Result sendDelay() {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay,
                simpleDateFormat.format(new Date()), new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        //设置延迟时间
                        message.getMessageProperties().setDelay(1000 * 10);
                        System.out.println(simpleDateFormat.format(new Date())+"Delay send ..");
                        return message;
                    }
                });
        return Result.ok();
    }
}
