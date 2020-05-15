package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-04-22 23:47
 */

@Api(tags = "测试商品详情数据内部接口")
@RestController
@RequestMapping("api/product")
//这个类中的所有数据是为了给外部提供数据使用的
public class ProductApiController {

    @Autowired
    private ManageService manageService;

    //inner/*/* 这种控制器中的映射表示内部接口调用的
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId) {
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        return skuInfo;
    }

    //通过三级分类id获取分类的信息
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id) {
        BaseCategoryView baseCategoryView = manageService.getCategoryViewByCategory3Id(category3Id);
        //返回分类的数据
        return baseCategoryView;
    }

    //根据skuId查询sku价格
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId) {
        return manageService.getSkuPrice(skuId);
    }

    //通过skuId，spuId查询销售属性的数据
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId, @PathVariable Long spuId) {
        return manageService.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    //点击销售属性值进行切换
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId) {
        //返回数据
        return manageService.getSaleAttrValuesBySpu(spuId);
    }


    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList() {
        List<JSONObject> baseCategoryList = manageService.getBaseCategoryList();
        return Result.ok(baseCategoryList);
    }

    /**
     * 根据品牌id查询品牌的数据
     *
     * @param tmId
     * @return
     */
    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId) {
        return manageService.getTrademarkByTmId(tmId);
    }

    /**
     * 根据商品skuId 获取平台属性和平台属性值的名称(查询平台属性集合对象)
     * @param skuId
     * @return
     */
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId) {
        return manageService.getAttrList(skuId);
    }
}
