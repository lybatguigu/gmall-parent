package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-05-09 17:51
 */
@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    /**
     * https://www.domain.com/CallBack/return_url?
     * out_trade_no=ATGUIGU1589019212939354&
     * version=1.0&
     * app_id=2018020102122556&
     * charset=utf-8&
     * sign_type=RSA2&trade_no=2020050922001462751418103035&
     * auth_app_id=2018020102122556&timestamp=2020-05-09%2018:15:54&
     * seller_id=2088921750292524&method=alipay.trade.page.pay.return&
     * total_amount=0.01&
     * sign=SVY7j3ICWUKAMSYdkBpPTMwP/ZRAdllCBPFoRpQVMeff6wjeB69fYzaF8llRYK7REB2jnBoXA6SY+ZX3YOiZUO8zYmVZCKwA/zvliB2PTIdmQliOqMSXr5yNjsEXuU0YpAPJ4a1rqjvRCan5evEmQS1iZygVML+loTBNEZO2CbtuY1DdrnonerFkPUZkjckNE3IFdk3AqEJGepVoFQj44Sd+T/MPIa/0nvkJkdGHftOTiAAmLJqGsKt4e+EgGyfBjqZbQhVuYsgMy9TXIrIHaKqBb/b610GdNmiKF6LUV6nNOxOpb9myxCiAsZigeldChUL6umsVx8rJOiaCXjYGRw==
     */

    @Autowired
    private AlipayService alipayService;
    @Autowired
    private PaymentService paymentService;

    //根据订单id来完成支付二维码的显示
    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String submitOrder(@PathVariable Long orderId) {
        //调用方法
        String aliPay = "";
        try {
            aliPay = alipayService.createAliPay(orderId);

        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return aliPay;
    }

    //同步回调地址return_payment_url=http://api.gmall.com/api/payment/alipay/callback/return
    @RequestMapping("callback/return")
    public String callback() {
        //显示支付成功页面 return_order_url=http://payment.gmall.com/pay/success.html
        return "redirect:" + AlipayConfig.return_order_url;
    }

    //异步回调  内网穿透，需要支付宝与电商平台做数据校验
    // notify_payment_url=http://hn3ph6.natappfree.cc/api/payment/alipay/callback/notify
    @RequestMapping("callback/notify")
    @ResponseBody
    public String aliPayNotify(@RequestParam Map<String, String> paramsMap) {
        //Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false;

        //因为将支付宝异步通知的参数封装到paramsMap集合中了，
        String trade_status = paramsMap.get("trade_status");
        //获取out_trade_no 查询当前paymentInfo数据
        String out_trade_no = paramsMap.get("out_trade_no");
        try {
            //验证签名成功
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //通过验证totalAmount等参数
            //在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            //TRADE_SUCCESS||TRADE_FINISHED表示在支付宝中已经支付了
            //交易记录PAID
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                //支付成功更改支付的状态payment_status=PAID
                //上述是一个订单只能支付一次，发现状态已经是支付或者关闭的状态下。
                //只有当前交易记录为UNPAID，在支付的时候已经成功
                //查询支付状态
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());

                //查询支付交易记录状态如果是PAID或CLOSED应该返回failure
                if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name()) || paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())) {
                    return "failure";
                }
                //验证金额和out_trade_no                total_amount
//                String total_amount = paramsMap.get("total_amount");
//                int amount = Integer.parseInt(total_amount);
//                BigDecimal totalAmount = new BigDecimal(amount);
//                if (paymentInfo.getTotalAmount().compareTo(totalAmount) == 0 && paymentInfo.getOutTradeNo().equals(out_trade_no)) {
//                    //除了PAID CLOSED 外，那么就应该更新交易记录
//                    paymentService.paySuccess(out_trade_no, PaymentType.ALIPAY.name(), paramsMap);
//                    //返回支付成功
//                    return "success";
//                }
                //除了PAID CLOSED 外，那么就应该更新交易记录
                paymentService.paySuccess(out_trade_no, PaymentType.ALIPAY.name(), paramsMap);
                //返回支付成功
                return "success";
            }
        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId) {
        //调用退款接口
        boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }

    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId) {
        Boolean aBoolean = alipayService.checkPayment(orderId);
        return aBoolean;
    }

    //根据订单id关闭订单
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId) {
        Boolean aBoolean = alipayService.closePay(orderId);
        return aBoolean;
    }

    @GetMapping("getpaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getpaymentInfo(@PathVariable String outTradeNo) {
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (null != paymentInfo) {
            return paymentInfo;
        }
        return null;
    }
}
