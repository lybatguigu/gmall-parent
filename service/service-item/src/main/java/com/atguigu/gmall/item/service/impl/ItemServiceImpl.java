package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Liyanbao
 * @create 2020-04-22 23:32
 */
@Service
public class ItemServiceImpl implements ItemService {

    //获取sku详情信息
    //在这个实现类中如何给map赋值
    //通过feign远程调用service-product中的方法
    @Autowired
    private ProductFeignClient productFeignClient;

    //从spring容器中获取线程池
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        Map<String, Object> map = new HashMap<>();
        //通过skuId去查询skuInfo数据
        CompletableFuture<SkuInfo> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);
        //上边获取skuInfo是需要返回值的，因为下边方法需要用skuInfo获取数据 -->supplyAsync()
        //保存销售属性和销售属性值集合到map中即可，不需要返回值-->thenAcceptAsync()
        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            //销售属性和销售属性值的集合
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
            map.put("spuSaleAttrList", spuSaleAttrList);
        }, threadPoolExecutor);

        //查询价格
        //单独起一个线程，查询出来直接放到map中，不需要返回值,用runAsync()方法
        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            //保存
            map.put("price", skuPrice);
        }, threadPoolExecutor);

        //获取分类信息,通过三级分类id查询分类数据
        CompletableFuture<Void> categoryViewCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            //保存
            map.put("categoryView", categoryView);
        }, threadPoolExecutor);

        //通过spuId查询用户点击销售属性值组成的sku
        CompletableFuture<Void> valuesSkuJsonCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            //转化为json字符串
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            //保存
            map.put("valuesSkuJson", valuesSkuJson);
        }, threadPoolExecutor);

        //商品详情页面被访问到的时候就调用一次热度排名,不需要返回值runAsync()
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);

//        Map<String, Object> map = new HashMap<>();
//        //通过skuId去查询skuInfo数据
//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//        //显示销售属性和销售属性值
//        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
//        //通过skuId查询商品价格
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        //通过三级分类id查询分类数据
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
//        //通过spuId查询用户点击销售属性值组成的sku
//        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//        //将map保存起来，给前台页面提供数据 把skuValueIdsMap 变为json字符串
//        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
//
//        //将集合数据放入map中
//        map.put("skuInfo", skuInfo);
//        map.put("price", skuPrice);
//        map.put("categoryView", categoryView);
//        map.put("spuSaleAttrList", spuSaleAttrList);
//        //保存json字符串数据
//        map.put("valuesSkuJson", valuesSkuJson);
        /**
         * allOf：等待所有任务完成
         * anyOf：只要有一个任务完成
         */
        CompletableFuture.allOf(skuCompletableFuture, spuSaleAttrCompletableFuture,
                skuPriceCompletableFuture, categoryViewCompletableFuture,
                valuesSkuJsonCompletableFuture,incrHotScoreCompletableFuture).join();
        return map;
    }
}
