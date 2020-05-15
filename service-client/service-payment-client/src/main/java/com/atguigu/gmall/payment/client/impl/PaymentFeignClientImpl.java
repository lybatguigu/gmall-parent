package com.atguigu.gmall.payment.client.impl;


import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import org.springframework.stereotype.Component;

/**
 * @author Liyanbao
 * @create 2020-05-10 16:34
 */
@Component
public class PaymentFeignClientImpl implements PaymentFeignClient {
    @Override
    public Boolean checkPayment(Long orderId) {
        return null;
    }

    @Override
    public PaymentInfo getpaymentInfo(String outTradeNo) {
        return null;
    }

    @Override
    public Boolean closePay(Long orderId) {
        return null;
    }
}
