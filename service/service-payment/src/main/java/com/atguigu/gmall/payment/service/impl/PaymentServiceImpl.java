package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-05-09 13:31
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private RabbitService rabbitService;

    //保存交易记录
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {

        //paymentInfo记录的当前一个订单的支付状态
        //一个订单只能在表中有一条记录
        Integer count = paymentInfoMapper.selectCount(new QueryWrapper<PaymentInfo>().eq("order_id", orderInfo.getId()).eq("payment_type", paymentType));
        //如果交易记录中已经存在了订单id，和支付类型那么就不能再次插入到数据库中
        if (count > 0) {
            return;
        }
        //创建paymenginfo对象
        PaymentInfo paymentInfo = new PaymentInfo();
        //给paymentInfo赋值。数据来源于orderInfo
        //查询orderInfo,根据订单id查询 orderId outTradeNo subject...TotalAmount
        Long orderId = orderInfo.getId();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        //支付的类型
        paymentInfo.setPaymentType(paymentType);

        paymentInfoMapper.insert(paymentInfo);
    }

    //根据outTradeNo和付款方法查询交易记录
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        //select * from payment_info where out_trade_no = out_trade_no and payment_type = name
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo).eq("payment_type", name);
        return paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
    }

    //根据outTradeNo和付款方法去更新交易记录
    @Override
    public void paySuccess(String outTradeNo, String name) {
        //update payment_info set payment_status = PAID where out_trade_no = outTradeNo and payment_type = name

        PaymentInfo paymentInfoUpdate = new PaymentInfo();
        paymentInfoUpdate.setPaymentStatus(PaymentStatus.PAID.name());
        //更新回调时间
        paymentInfoUpdate.setCallbackTime(new Date());
        //更新回调内容
        paymentInfoUpdate.setCallbackContent("异步回调~~");

        //第一个参数：paymentinfo，表示要更新的内容放入paymentInfo中
        //第二个参数：更新的条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo).eq("payment_type", name);
        paymentInfoMapper.update(paymentInfoUpdate, paymentInfoQueryWrapper);
    }

    //更新trade_no，payment_status，callback_time，callback_content
    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramsMap) {
        //update payment_info set payment_status = PAID where out_trade_no = outTradeNo and payment_type = name

        PaymentInfo paymentInfoUpdate = new PaymentInfo();
        paymentInfoUpdate.setPaymentStatus(PaymentStatus.PAID.name());
        //更新回调时间
        paymentInfoUpdate.setCallbackTime(new Date());
        //更新回调内容
        paymentInfoUpdate.setCallbackContent(paramsMap.toString());
        //追加更新trade_no ,支付宝交易号在map中
        String trade_no = paramsMap.get("trade_no");
        paymentInfoUpdate.setTradeNo(trade_no);

        //第一个参数：paymentinfo，表示要更新的内容放入paymentInfo中
        //第二个参数：更新的条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo).eq("payment_type", name);
        paymentInfoMapper.update(paymentInfoUpdate, paymentInfoQueryWrapper);

        //查询订单id
        PaymentInfo paymentInfo = getPaymentInfo(outTradeNo, name);
        //支付成功之后发送消息通知订单,更改订单状态
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, paymentInfo.getOrderId());
    }

    //更新方法
    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        //update payment_info set payment_status = PAID where out_trade_no = outTradeNo and payment_type = name
        paymentInfoMapper.update(paymentInfo, new QueryWrapper<PaymentInfo>().eq("out_trade_no", outTradeNo));

    }

    //关闭交易记录
    @Override
    public void closePayment(Long orderId) {

        //更新paymentInfo payment_status = closed
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id", orderId);
        //关闭之前查询
        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        /**
         * 交易记录表中的数据产生时间
         * 当用户点击支付宝生成二维码的时候paymentInfo表里才会有数据的
         * 如果只是点下单，没有点生成二维码的时候表里是不会有数据的
         *
         * 根据上述条件，先查询是否有交易记录，如果没有则不关闭。
         */
        //如果交易记录中没有当前数据则返回不关闭
        if (null == count || count.intValue() == 0) {
            return;
        }
        //第一参数，更新的内容  第二：更新条件
        //update payment_info set payment_status = closed where order_id = orderId
        paymentInfoMapper.update(paymentInfo, paymentInfoQueryWrapper);
    }
}
