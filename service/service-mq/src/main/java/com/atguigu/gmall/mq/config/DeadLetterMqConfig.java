package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author Liyanbao
 * @create 2020-05-08 20:36
 */
@Configuration
public class DeadLetterMqConfig {

    //声明变量
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    //声明一个交换机
    @Bean
    public DirectExchange exchange() {
        //返回一个交换机
        return new DirectExchange(exchange_dead, true, false, null);
    }

    @Bean
    public Queue queue1() {
        //设置参数
        HashMap<String, Object> hashMap = new HashMap<>();
        //设置一个死信交换机
        hashMap.put("x-dead-letter-exchange", exchange_dead);
        //给当前的死信交换机绑定一个队列，通过routingKey绑定
        hashMap.put("x-dead-letter-routing-key", routing_dead_2);
        //统一规定延迟时间
        hashMap.put("x-message-ttl", 1000 * 10);
        //最后一个参数，表示是否要给当前的队列设置参数，如果有参数设置则需要写在map中
        return new Queue(queue_dead_1, true, false, false, hashMap);
    }

    //给单独的队列设置一个绑定关系
    @Bean
    public Binding binding() {
        //绑定队列 将队列1通过routingkey绑定到交换机
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    //声明第二个队列,如果队列1出现问题则走队列2
    @Bean
    public Queue queue2() {
        return new Queue(queue_dead_2, true, false, false, null);
    }

    //将队列2绑定到交换机
    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}
