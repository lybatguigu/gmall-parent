package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

/**
 * @author Liyanbao
 * @create 2020-05-09 17:25
 */
public interface AlipayService {

    /**
     * 支付接口 ,根据订单id完成支付
     * @param orderId
     * @return
     * @throws AlipayApiException
     */
    String createAliPay(Long orderId) throws AlipayApiException;

    /**
     * 根据orderId退款
     * @param orderId
     * @return
     */
    boolean refund(Long orderId);

    /**
     * 关闭支付宝交易记录
     * @param orderId
     * @return
     */
    Boolean closePay(Long orderId);

    /**
     * 检查是否在支付宝中有交易记录
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);
}
