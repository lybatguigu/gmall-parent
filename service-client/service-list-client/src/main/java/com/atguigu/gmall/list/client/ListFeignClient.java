package com.atguigu.gmall.list.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.impl.ListDegradeFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Liyanbao
 * @create 2020-04-27 21:51
 */
@FeignClient(name = "service-list", fallback = ListDegradeFeignClient.class)
public interface ListFeignClient {

    //热度排名
    @GetMapping("api/list/inner/incrHotScore/{skuId}")
    Result incrHotScore(@PathVariable Long skuId);

    /**
     * 搜索商品
     * @param searchParam
     * @return
     */
    @PostMapping("api/list")
    Result search(@RequestBody SearchParam searchParam);

    /**
     * 上架商品
     * @param skuId
     * @return
     */
    @GetMapping("api/list/inner/upperGoods/{skuId}")
    Result upperGoods(@PathVariable Long skuId);

    /**
     * 下架商品
     * @param skuId
     * @return
     */
    @GetMapping("api/list/inner/lowerGoods/{skuId}")
    Result lowerGoods(@PathVariable Long skuId);
}
