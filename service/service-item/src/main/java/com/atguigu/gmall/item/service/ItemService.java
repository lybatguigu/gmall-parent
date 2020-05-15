package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-04-22 23:26
 */
public interface ItemService {

    //商品页面要获取到数据，那么需要有skuId
    //那么这个商品的id是从哪传过来的呢？
    //商品详情页面是从List检索页面传过来的。
    //https://item.jd.com/100010260254.html
    //https://item.jd.com 域名  100010260254.html：控制器   {skuId}.html

    /**
     * 获取sku详情信息
     * 需将数据封装到map中
     * map.put("price","商品的价格")
     * @param skuId 库存单元的Id
     * @return
     */
    Map<String, Object> getBySkuId(Long skuId);
}
