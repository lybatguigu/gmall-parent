package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author Liyanbao
 * @create 2020-05-08 21:59
 */
@Configuration
public class DelayedMqConfig {
    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    //声明一个队列
    @Bean
    public Queue delayQueue() {
        return new Queue(queue_delay_1, true);

    }

    //声明一个交换机
    @Bean
    public CustomExchange delayExchange() {
        //配置参数
        HashMap<String, Object> map = new HashMap<>();
        //基于插件的指定参数，固定的
        map.put("x-delayed-type", "direct");
        //基于插件的交换机类型key是固定值
        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,map);
    }

    //绑定关系
    @Bean
    public Binding delayBinding() {
        //基于插件的绑定关系
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(routing_delay).noargs();
    }

}
