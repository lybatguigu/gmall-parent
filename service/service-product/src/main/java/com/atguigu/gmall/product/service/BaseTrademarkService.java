package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-04-18 16:09
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {

    /**
     * 实现品牌分页的列表
     * @param pageParam
     * @return
     */
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam);

    //查询所有品牌的属性集合
    List<BaseTrademark> getTrademarkList();
}
