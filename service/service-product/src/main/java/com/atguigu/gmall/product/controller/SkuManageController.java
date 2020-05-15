package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-04-20 21:37
 */
@Api(tags = "sku接口")
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;


    /**
     * http://api.gmall.com/admin/product/spuImageList/4
     * 根据spuId查询图片列表集合
     *
     * @param spuId
     * @return
     */
    @GetMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId) {
        //调用服务层
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        //返回数据
        return Result.ok(spuImageList);
    }

    //http://api.gmall.com/admin/product/spuSaleAttrList/3
    //加载销售的属性
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId) {
        //调用服务层
        List<SpuSaleAttr> spuSaleAttrList = manageService.spuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }


    /**
     * @param skuInfo:保存库存单元信息表
     * @return http://api.gmall.com/admin/product/saveSkuInfo
     */
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo) {

        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    /**
     * skuInfo分页列表查询
     * http://api.gmall.com/admin/product/list/1/10
     *
     * @param page
     * @param size
     * @return
     */
    @GetMapping("list/{page}/{size}")
    public Result skuInfoList(@PathVariable Long page,
                              @PathVariable Long size) {
        Page<SkuInfo> skuInfoPage = new Page<>(page, size);
        IPage<SkuInfo> skuInfoIPage = manageService.selectPage(skuInfoPage);
        return Result.ok(skuInfoIPage);
    }

    //http://api.gmall.com/admin/product/onSale/20上架
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId) {
        manageService.onSale(skuId);
        return Result.ok();
    }
    //http://api.gmall.com/admin/product/cancelSale/20下架
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId) {
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}
