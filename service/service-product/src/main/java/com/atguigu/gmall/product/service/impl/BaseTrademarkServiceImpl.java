package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-04-18 16:12
 */
@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper, BaseTrademark> implements BaseTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;


    //实现品牌分页的列表
    @Override
    public IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam) {
        QueryWrapper<BaseTrademark> baseTrademarkQueryWrapper = new QueryWrapper<>();
        baseTrademarkQueryWrapper.orderByDesc("id");
        IPage<BaseTrademark> page = baseTrademarkMapper.selectPage(pageParam, baseTrademarkQueryWrapper);
        return page;
    }

    /**
     * 查询所有品牌的属性集合
     * @return
     */
    @Override
    public List<BaseTrademark> getTrademarkList() {
        return baseTrademarkMapper.selectList(null);
    }
}
