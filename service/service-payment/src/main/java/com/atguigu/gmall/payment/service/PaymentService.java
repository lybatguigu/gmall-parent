package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-05-09 13:27
 */
public interface PaymentService {

    /**
     * 保存交易记录
     * @param orderInfo 保存交易记录的数据
     * @param paymentType 交易的方式
     */
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 根据outTradeNo和付款方法查询交易记录
     * @param outTradeNo
     * @param name
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String name);

    /**
     * 根据outTradeNo和付款方法去更新交易记录
     * @param outTradeNo
     * @param name
     */
    void paySuccess(String outTradeNo, String name);

    /**
     * 根据outTradeNo和付款方法去更新交易记录,更新trade_no，payment_status，callback_time，callback_content
     * @param outTradeNo
     * @param name
     * @param paramsMap 支付宝回调参数都在map中
     */
    void paySuccess(String outTradeNo, String name, Map<String, String> paramsMap);

    /**
     * 更新方法
     * @param outTradeNo
     * @param paymentInfo
     */
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    /**
     * 关闭交易记录
     * @param orderId
     */
    void closePayment(Long orderId);
}
