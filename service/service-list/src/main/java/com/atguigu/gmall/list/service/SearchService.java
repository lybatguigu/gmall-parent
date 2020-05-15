package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

/**
 * @author Liyanbao
 * @create 2020-04-27 20:30
 */
public interface SearchService {

    /**
     * 上架商品
     * @param skuId
     */
    void upperGoods(Long skuId);

    /**
     * 下架商品
     * @param skuId
     */
    void lowerGoods(Long skuId);

    /**
     * 更新热度排名
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * 根据用户输入的条件查询数据
     * @param searchParam
     * @return
     * @throws IOException
     */
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}
