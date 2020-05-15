package com.atguigu.gmall.cart.client.impl;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-05-05 0:40
 */
@Component
public class CartDegradeFeignClient implements CartFeignClient {
    @Override
    public Result addToCart(Long skuId, Integer skuNum) {
        return null;
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        return null;
    }

    @Override
    public Result loadCartCache(String userId) {
        return null;
    }
}
