package com.atguigu.gmall.payment.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.payment.service.PaymentService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.ws.Action;

/**
 * @author Liyanbao
 * @create 2020-05-10 15:22
 */
@Component
public class PaymentReceiver {

    @Autowired
    private PaymentService paymentService;

    //获取消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_CLOSE),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE),
            key = {MqConst.ROUTING_PAYMENT_CLOSE}
    ))
    public void closePayment(Long orderId, Message message, Channel channel) {
        //id不为空
        if (null != orderId) {
            //关闭交易paymentInfo 支付宝
            paymentService.closePayment(orderId);
        }
        //手动确认消息被消费
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
