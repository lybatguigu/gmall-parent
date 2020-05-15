package com.atguigu.gmall.product.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Feign ：远程调用
 *
 * @author Liyanbao
 * @create 2020-04-23 16:31
 */
@FeignClient(value = "service-product", fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {

    //调用service-product服务中的数据接口(ProductApiController)
    //调用根据skuId查询skuInfo数据的方法
    //调用过程：当用户调用ProductFeignClient.getSkuInfo方法时，本质是调用了ProductApiController控制器的getSkuInfo方法
    @GetMapping("api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable Long skuId);

    /**
     * 通过三级分类id查询分类信息
     *
     * @param category3Id
     * @return
     */
    @GetMapping("/api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id);

    /**
     * 获取sku最新价格
     *
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable(value = "skuId") Long skuId);

    /**
     * 根据spuId，skuId 查询销售属性集合
     *
     * @param skuId
     * @param spuId
     * @return
     */
    @GetMapping("/api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId, @PathVariable("spuId") Long spuId);

    /**
     * 根据spuId 查询map 集合属性
     *
     * @param spuId
     * @return
     */
    @GetMapping("/api/product/inner/getSkuValueIdsMap/{spuId}")
    Map getSkuValueIdsMap(@PathVariable("spuId") Long spuId);

    @GetMapping("/api/product/getBaseCategoryList")
    Result getBaseCategoryList();

    /**
     * 根据品牌id查询品牌的数据
     * @param tmId
     * @return
     */
    @GetMapping("api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademark(@PathVariable Long tmId);

    /**
     * 根据商品skuId 获取平台属性和平台属性值的名称(查询平台属性集合对象)
     * @param skuId
     * @return
     */
    @GetMapping("api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable Long skuId);
}
