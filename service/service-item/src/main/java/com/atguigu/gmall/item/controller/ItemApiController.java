package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 代表数据接口
 * @author Liyanbao
 * @create 2020-04-22 23:36
 */
@RestController
@RequestMapping("api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    //根据skuId获取数据
    @GetMapping("{skuId}")
    public Result getItem(@PathVariable Long skuId) {
        //根据skuId获取商品详情页面需要展示的数据
        Map<String, Object> result = itemService.getBySkuId(skuId);
        //返回数据
        return Result.ok(result);
    }
}
