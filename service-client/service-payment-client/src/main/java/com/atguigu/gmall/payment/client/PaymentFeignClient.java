package com.atguigu.gmall.payment.client;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.impl.PaymentFeignClientImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author Liyanbao
 * @create 2020-05-10 16:33
 */
@FeignClient(name = "service-payment", fallback = PaymentFeignClientImpl.class)
public interface PaymentFeignClient {


    //检查是否在支付宝中有交易记录
    @GetMapping("/api/payment/alipay/checkPayment/{orderId}")
    Boolean checkPayment(@PathVariable Long orderId);
    //查看paymentInfo中是否有交易记录
    @GetMapping("/api/payment/alipay/getpaymentInfo/{outTradeNo}")
    PaymentInfo getpaymentInfo(@PathVariable String outTradeNo);

    //根据订单id关闭订单
    @GetMapping("/api/payment/alipay/closePay/{orderId}")
    Boolean closePay(@PathVariable Long orderId);

}
