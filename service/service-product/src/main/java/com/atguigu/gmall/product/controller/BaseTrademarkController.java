package com.atguigu.gmall.product.controller;


import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-04-18 15:58
 */
@RestController
@RequestMapping("admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //实现品牌分页的列表
    @GetMapping("{page}/{size}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long size) {
        Page<BaseTrademark> pageParam = new Page<>(page, size);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.selectPage(pageParam);
        return Result.ok(baseTrademarkIPage);
    }

    //http://api.gmall.com/admin/product/baseTrademark/remove/3
    //删除品牌
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        baseTrademarkService.removeById(id);
        return Result.ok();
    }
    //http://api.gmall.com/admin/product/baseTrademark/get/3
    //获取品牌
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }
    //修改品牌   根据id修改。需要传一个主键id  对象里边有id可以直接传id  由json字符串转java对象
    @PutMapping("update")
    public Result update(@RequestBody BaseTrademark baseTrademark) {
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }
    //保存品牌   需要将json字符串转换为java对象
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark) {
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }


    /**
     * 查询所有品牌的属性集合
     * @return
     */
    @GetMapping("getTrademarkList")
    public Result getTrademarkList() {
        List<BaseTrademark> baseTrademarkList = baseTrademarkService.getTrademarkList();
        return Result.ok(baseTrademarkList);
    }
}
