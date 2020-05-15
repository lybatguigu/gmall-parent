package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author Liyanbao
 * @create 2020-05-08 22:34
 * 编写取消订单的配置类
 */
@Configuration
public class OrderCanelMqConfig {

    //声明队列
    @Bean
    public Queue delayQueue() {
        return new Queue(MqConst.QUEUE_ORDER_CANCEL, true);
    }

    //定义交换机
    @Bean
    public CustomExchange delayExchange() {
        //配置参数
        HashMap<String, Object> map = new HashMap<>();
        //基于插件的指定参数，固定的
        map.put("x-delayed-type", "direct");
        //基于插件的交换机类型key是固定值
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",true,false,map);
    }

    //交换机和队列绑定
    @Bean
    public Binding delayBinding() {
        //返回绑定结果
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }

}
