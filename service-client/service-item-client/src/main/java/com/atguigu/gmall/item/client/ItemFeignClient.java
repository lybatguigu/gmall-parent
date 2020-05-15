package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author Liyanbao
 * @create 2020-04-23 18:01
 */
@FeignClient(value = "service-item",fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {

    //调用service-item中的方法
    //通过skuId查询商品详情数据
    //用户调用ItemFeignClient.getItem方法时，本质是调用ItemApiController.getItem方法
    @GetMapping("api/item/{skuId}")
    Result getItem(@PathVariable Long skuId);

}
