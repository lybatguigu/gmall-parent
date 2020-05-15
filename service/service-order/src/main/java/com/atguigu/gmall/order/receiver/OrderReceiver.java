package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-05-08 22:53
 */
@Component //取消订单
public class OrderReceiver {

    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentFeignClient paymentFeignClient;
    @Autowired
    private RabbitService rabbitService;

    //获取队列中的消息
    //发送消息的时候发送的是订单id
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) {
        //判断订单id不为空
        if (null != orderId) {
            //根据订单id查询订单表中是否有当前记录
            OrderInfo orderInfo = orderService.getById(orderId);
            if (null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                /**
                 * 先关闭paymentinfo后管orderInfo，因为支付成功后，异步回调先修改的paymentInfo，
                 * 然后在发送的异步通知修改订单的状态.
                 */
                //关闭流程，先看电商平台交易记录中是否有数据，有就关闭
                PaymentInfo paymentInfo = paymentFeignClient.getpaymentInfo(orderInfo.getOutTradeNo());
                //判断电商交易记录,交易记录表中有数据，用户一定走到了生成二维码哪一步
                if (null != paymentInfo && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())) {
                    //关闭交易,检查支付宝中是否有交易记录
                    Boolean flag = paymentFeignClient.checkPayment(orderId);
                    //true说明用户在支付宝中产省了交易记录.说明扫了二维码
                    if (flag) {
                        //关闭支付宝
                        Boolean result = paymentFeignClient.closePay(orderId);
                        //判断是否关闭成功
                        if (result) {
                            //关闭支付宝的订单成功 ,关闭orderInfo表和paymentInfo表
                            orderService.execExpiredOrder(orderId, "2");
                        } else {
                            //关闭失败,如果用户付款成功了我们调用关闭接口失败
                            //走正常流程
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, orderId);
                        }
                        //false说明没有扫二维码，只是到了二维码
                    } else {
                        //关闭支付宝的订单成功 ,关闭orderInfo表和paymentInfo表
                        orderService.execExpiredOrder(orderId, "2");
                    }
                //如果说paymentInfo中为空呢没有数据
                } else {
                    //只关闭orderInfo
                    orderService.execExpiredOrder(orderId, "1");
                }
            }
        }
        //手动确认消费消息已经处理
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //监听消息，更改订单的状态
    //rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, paymentInfo.getOrderId());
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void getMsg(Long orderId, Message message, Channel channel) {
        //id不为空
        if (null != orderId) {
            //判断支付状态是未付款
            OrderInfo orderInfo = orderService.getById(orderId);
            if (null != orderInfo) {
                if (orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                    //更新订单的状态
                    orderService.updateOrderStatus(orderId,ProcessStatus.PAID);
                    //发送消息给库存
                    orderService.sendOrderStatus(orderId);
                }
            }
            //手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    //监听库存系统减库存的消息队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson, Message message, Channel channel) {
        //消息不为空
        if (!StringUtils.isEmpty(msgJson)) {
            //msgJson是由map组成的,转为map获取里边的orderId和status
            Map map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
            //判断减库存是否成功
            if ("DEDUCTED".equals(status)) {
                //减库存成功,将订单状态变为等待发货
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
            } else {
                //减库存失败，订单中的商品数量大于库存数量发生超卖
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.STOCK_EXCEPTION);
            }
        }
    }
}
