package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-04-18 0:01
 */
@Api(tags = "商品的基础属性接口")
@RestController
@RequestMapping("admin/product")
//@CrossOrigin
public class ManageController {
    @Autowired
    ManageService manageService;

    //一级分类
    @GetMapping("getCategory1")
    public Result getCategory1() {
        //一级分类的数据集合
        List<BaseCategory1> baseCategory1List = manageService.getCategory1();
        return Result.ok(baseCategory1List);
    }

    //二级分类
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id) {
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    //三级分类
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id) {
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    //根据分类Id 获取平台属性数据
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result attrInfoList(@PathVariable Long category1Id,
                               @PathVariable Long category2Id,
                               @PathVariable Long category3Id) {

        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(attrInfoList);
    }

    //保存平台属性
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        //保存数据
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    //根据平台属性ID获取平台属性,返回平台属性值集合
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId) {
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(attrValueList);
    }

    //根据三级分类id 分页查询数据
    @GetMapping("{page}/{size}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long size,
                        SpuInfo spuInfo) {
        Page<SpuInfo> pageParam = new Page<>(page, size);
        IPage<SpuInfo> spuInfoIPage = manageService.selectPage(pageParam, spuInfo);
        return Result.ok(spuInfoIPage);
    }

}
