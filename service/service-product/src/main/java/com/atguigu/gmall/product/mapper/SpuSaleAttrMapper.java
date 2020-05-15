package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-04-20 2:04
 */
public interface SpuSaleAttrMapper extends BaseMapper<SpuSaleAttr> {
    /**
     * 根据spuId查询销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrList(Long spuId);

    /**
     * 根据skuId和spuId查询销售属性和销售属性值
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(Long skuId, Long spuId);
}
