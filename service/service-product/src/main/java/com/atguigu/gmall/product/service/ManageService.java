package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-04-17 22:13
 */
public interface ManageService {
    /**
     * 查询所有一级分类的信息
     *
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类id 查询二级分类的数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类id 查询三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类id 获取平台属性数据
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    //保存平台属性
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性id查询平台属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(Long attrId);

    /**
     * 根据三级分类id 进行分页查询数据
     * @param pageParam 封装page第几页，和每页显示的条数
     * @param spuInfo 根据查询的条件查询
     * @return
     */
    IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo);

    /**
     * 查询所有的销售属性集合
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    //保存商品
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据商品id查询图片列表
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 根据spuId查询销售属性的集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> spuSaleAttrList(Long spuId);

    /**
     * 保存库存单元信息
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 查询skuInfo分页信息
     * @param skuInfoPage
     * @return
     */
    IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoPage);

    //商品上架
    void onSale(Long skuId);

    //商品下架
    void cancelSale(Long skuId);

    /**
     * 根据skuId 查询skuInfo
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);


    /**
     * 根据三级分类id查询分类的名称
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    /**
     * 根据skuId获取sku的价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据skuId和spuId查询销售属性，销售属性值
     *
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@Param("skuId") Long skuId, @Param("spuId") Long spuId);

    /**
     * 根据spuId 获取销售属性值id与skuId组成的一个集合数据
     * @param spuId
     * @return
     */
    Map getSaleAttrValuesBySpu(Long spuId);

    /**
     * 页面查询所有的分类数据
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据品牌id 查询品牌数据
     * @param tmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long tmId);

    /**
     * 根据商品skuId 获取平台属性和平台属性值的名称
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);
}
