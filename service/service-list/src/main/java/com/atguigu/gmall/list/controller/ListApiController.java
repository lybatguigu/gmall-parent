package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @author Liyanbao
 * @create 2020-04-27 11:25
 */
@RestController
@RequestMapping("api/list")
public class ListApiController {

    //生成mapping 引擎elasticsearch
    @Autowired
    private ElasticsearchRestTemplate restTemplate;
    @Autowired
    private SearchService searchService;

    @GetMapping("inner/createIndex")
    public Result createIndex() {
        //创建index
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    //上架商品
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId) {
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    //下架商品
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId) {
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    //热度排名
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId) {
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    //搜索商品
    //使用json传值,将json转化为java对象
    @PostMapping
    public Result search(@RequestBody SearchParam searchParam) throws IOException {
        SearchResponseVo search = searchService.search(searchParam);
        return Result.ok(search);
    }
}
