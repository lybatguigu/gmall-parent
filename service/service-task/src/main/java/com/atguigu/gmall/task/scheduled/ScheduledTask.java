package com.atguigu.gmall.task.scheduled;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Liyanbao
 * @create 2020-05-11 22:58
 */
@Component
@EnableScheduling //开启定时任务
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;

    //定义任务开启时间 凌晨1点钟 分时日月周年
    //@Scheduled(cron = "0 0 1 * * ?")
    //表示每隔30秒触发当前任务
    @Scheduled(cron = "0/30 * * * * ?")
    public void task() {
        //发送消息  发送的内容空。处理消息的时候扫描秒杀的商品
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_1, "");
    }

    //每天晚上删除缓存中的秒杀数据,这个是每天晚上18点
    @Scheduled(cron = "* * 18 * * ?")
    public void taskDelRedis() {
        //发送消息  发送的内容空。处理消息的时候扫描秒杀的商品
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_18, "");
    }

}
