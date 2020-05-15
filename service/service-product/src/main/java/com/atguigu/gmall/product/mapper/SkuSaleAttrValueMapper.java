package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SkuSaleAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-04-20 22:53
 */
@Mapper
public interface SkuSaleAttrValueMapper extends BaseMapper<SkuSaleAttrValue> {
    /**
     * 根据spuId查询List数据
     * @param spuId
     * @return
     */
    List<Map> selectSaleAttrValuesBySpu(Long spuId);
}
