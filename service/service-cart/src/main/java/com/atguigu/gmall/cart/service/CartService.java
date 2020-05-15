package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-05-04 0:51
 */
public interface CartService {
    //添加购物车的抽象方法
    // 添加购物车 用户Id，商品Id，商品数量。
    void addToCart(Long skuId, String userId, Integer skuNum);

    /**
     * 根据用户id查询购物车列表
     * @param userId 登录的用户id
     * @param userTempId 未登录的用户id
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 购物车选中状态
     * @param userId 用户id
     * @param isChecked 商品的选中状态
     * @param skuId 商品的id
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    /**
     * 删除购物车
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 根据用户id查询购物车列表，被选中的商品组成送货的清单
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据用户id 加载购物车数据并放入缓存
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
