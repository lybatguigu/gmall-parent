package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePageRefundResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * @author Liyanbao
 * @create 2020-05-09 17:27
 */
@Service
public class AlipayServiceImpl implements AlipayService {

    //获取AlipayClient对象
    @Autowired
    private AlipayClient alipayClient;
    //远程获取orderInfo数据
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private PaymentService paymentService;


    //根据订单id完成支付
    @Override
    public String createAliPay(Long orderId) throws AlipayApiException {
        //根据订单id查询订单数据
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //在生成二维码的时候，调用一下保存交易记录方法
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());


        //已经注入到spring容器中了 AlipayClient
        //AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        //设置同步回调的url
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //设置异步回调的url
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url); //在公共参数中设置回跳和通知地址
        //设置二维码需要的参数 json
        HashMap<String, Object> map = new HashMap<>();
        //封装参数 orderInfo中的out_trade_no
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", orderInfo.getTotalAmount());
        map.put("subject", "买买买!!!");

        alipayRequest.setBizContent(JSON.toJSONString(map));
        //返回
        return alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
    }

    //根据orderId退款
    @Override
    public boolean refund(Long orderId) {
        //获取订单中的数据
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        //声明map
        //out_trade_no在orderInfo和peymentInfo里

        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("refund_amount", orderInfo.getTotalAmount());
        map.put("refund_reason", "正常退款~~~");
        //map.put("out_request_no", "HZ01RF001"); 部分退款使用
        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.pageExecute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            //关闭交易状态
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
            paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(),paymentInfo);
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    //关闭支付宝交易记录
    @Override
    public Boolean closePay(Long orderId) {
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        //out_trade_no 在orderInfo和paymentInfo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("operator_id", "YX01");
        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeCloseResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    //查看在支付宝中是否有交易记录
    @Override
    public Boolean checkPayment(Long orderId) {
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());

        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }
}
