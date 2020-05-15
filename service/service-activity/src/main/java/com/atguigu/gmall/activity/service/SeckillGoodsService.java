package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-05-12 0:19
 */
public interface SeckillGoodsService {

    /**
     * 查询所有的秒杀商品
     * @return
     */
    List<SeckillGoods> findAll();

    /**
     * 根据skuId查询商品详情
     * @param skuId
     * @return
     */
    SeckillGoods getSeckillGoodsById(Long skuId);

    /**
     * 预下单处理
     * @param userId
     * @param skuId
     */
    void seckillOrder(String userId, Long skuId);

    /**
     * 检查订单
     * @param skuId
     * @param userId
     * @return
     */
    Result checkOrder(Long skuId, String userId);
}
