package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-05-12 0:31
 */
@Controller
public class SeckilController {

    @Autowired
    private ActivityFeignClient activityFeignClient;

    //http://activity.gmall.com/seckill.html
    @GetMapping("seckill.html")
    public String getAll(Model model) {
        //获取秒杀商品数据
        Result result = activityFeignClient.findAll();
        //后台存储一个list集合数据
        model.addAttribute("list", result.getData());
        //返回秒杀商品页面
        return "seckill/index";
    }

    @GetMapping("seckill/{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model) {
        //获取秒杀商品的详情数据
        Result result = activityFeignClient.getSeckillGoods(skuId);
        //存储item对象
        model.addAttribute("item", result.getData());
        //返回商品详情页面
        return "seckill/item";
    }

    /**
     * seckill/queue.html?skuId='+this.skuId+'&skuIdStr='+skuIdStr
     * 秒杀排队
     * 根据请求获取到 skuId  和 skuIdStr
     *
     * @param skuId
     * @param skuIdStr
     * @param request
     * @return
     */
    @GetMapping("seckill/queue.html")
    public String queue(@RequestParam(name = "skuId") Long skuId,
                        @RequestParam(name = "skuIdStr") String skuIdStr,
                        HttpServletRequest request) {
        //存储下单码和skuId前台获取
        request.setAttribute("skuId", skuId);
        request.setAttribute("skuIdStr", skuIdStr);
        return "seckill/queue";
    }

    @GetMapping("seckill/trade.html")
    public String trade(Model model) {
        //获取下单数据
        Result<Map<String, Object>> result = activityFeignClient.trade();
        if (result.isOk()) {
            //将数据保存给页面提供渲染
            model.addAllAttributes(result.getData());
            //返回订单页面
            return "seckill/trade";
        } else {
            model.addAttribute("message",result.getMessage());
            //返回失败的订单页面
            return "seckill/fail";
        }

    }
}
