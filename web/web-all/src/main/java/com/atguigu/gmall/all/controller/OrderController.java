package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.bouncycastle.cms.PasswordRecipientId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.xml.ws.Action;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-05-03 21:28
 */
@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;


    @GetMapping("trade.html")
    public String trade(Model model) {
        Result<Map<String, Object>> result = orderFeignClient.trade();
        //保存数据
        model.addAllAttributes(result.getData());
        //返回页面
        return "order/trade";
    }
}
