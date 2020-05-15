package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-04-20 1:13
 */
@RestController
@RequestMapping("admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    //销售属性
    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList() {
        //查询所有销售属性的集合 引入service
        List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    //http://api.gmall.com/admin/product/saveSpuInfo
    //保存商品
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }
}
