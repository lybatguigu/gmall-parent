package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Liyanbao
 * @create 2020-05-08 21:10
 */
@Configuration
@Component
public class DeadLetterReceiver {

    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void gtMsg(String msg) {
        System.out.println("接收数据" + msg);
        //声明一个时间格式对象
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //看接收到的消息数据
        System.out.println("Receive queue_dead_2: " + simpleDateFormat.format(new Date()) + " Delay rece." + msg);
    }
}
