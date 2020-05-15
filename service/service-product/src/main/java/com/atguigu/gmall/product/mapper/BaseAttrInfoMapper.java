package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-04-17 22:10
 */
@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {
    /**
     * 根据分类id 查询平台属性集合对象 编写xml文件
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> selectBaseAttrInfoList(@Param("category1Id") Long category1Id,@Param("category2Id") Long category2Id,@Param("category3Id") Long category3Id);

    /**
     * 根据商品skuId 获取平台属性和平台属性值的名称(查询平台属性集合对象)
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> selectBaseAttrInfoListBySkuId(Long skuId);
}
