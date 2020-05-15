package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-04-23 18:09
 */
@Controller
public class ItemController {

    //调用service-item，通过feign远程调用
    @Autowired
    private ItemFeignClient itemFeignClient;

    //用户如何访问商品详页面？通过商品检索列表
    //商品详情的控制器如何定义？域名，和skuId.html
    //如何获取到商品skuId？@PathVariable Long skuId
    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model) {
        //通过feign远程调用获取商品详情数据
        Result<Map> result = itemFeignClient.getItem(skuId);
        //在后台将商品详情的页面保存到后台+
        //addAllAttributes:使用这个方法的原因？因为map中存储了多个key和value。
        model.addAllAttributes(result.getData());
        //返回商品详情页面
        return "item/index";
    }
}
