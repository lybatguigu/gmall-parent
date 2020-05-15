package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Liyanbao
 * @create 2020-05-04 0:52
 */
@Service
public class CartServiceImpl implements CartService {

    //引入mapper
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /**
         * 思路:
         * 1.添加购物车是判断购物车中是否有该商品
         * 如果有则数量相加
         * 没有就直接添加
         *
         * 特殊处理：添加购物车时，直接将购物车添加到缓存中
         */
        //定义个cartkey,获取购物车
        String cartKey = getCartKey(userId);
        //添加的时候看看数据库中是否有数据
        if (!redisTemplate.hasKey(cartKey)) {
            //查询数据库并添加到缓存
            loadCartCache(userId);
        }
        //select * from cart_info where sku_id = ? and user_id = ?
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("sku_id", skuId).eq("user_id", userId);
        //看数据库中是否有添加的商品
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQueryWrapper);
        //判断
        if (null != cartInfoExist) {
            //说明购物车已经添加过当前商品 +1
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            //赋值一个实时的价格,在数据库中不存在的
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfoExist.setSkuPrice(skuPrice);

            //更新数据库
            cartInfoMapper.updateById(cartInfoExist);
            //添加缓存：添加完成之后直接如果要去查询购物车列表的时候，直接走缓存
            //如果缓存过期了才会查询数据库
            //redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);
        } else {
            //购物车没有商品
            CartInfo cartInfo = new CartInfo();
            //给当前的cartinfo赋值
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            //第一次添加购物车的时候，添加购物车的价格与实时价格应该是统一的
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            //cartInfo.setIsChecked();//默认选择状态
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoMapper.insert(cartInfo);
            //添加缓存：添加完成之后直接如果要去查询购物车列表的时候，直接走缓存
            //如果缓存过期了才会查询数据库
            //redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfo);
            cartInfoExist = cartInfo;
        }
        //添加缓存：添加完成之后直接如果要去查询购物车列表的时候，直接走缓存
        //如果缓存过期了才会查询数据库
        // 使用hash数据类型 hset(key,field,value)   key=user:userId:cart field=skuId value=购物车的字符串
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);

        //购物车在缓存中应该有过期时间
        setCartKeyExpire(cartKey);
    }

    //设置过期时间
    private void setCartKeyExpire(String cartKey){
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    //获取购物中的cartkey
    private String getCartKey(String userId) {
        //区分谁的购物车
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;

    }

    //根据用户id查询购物车列表
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        //声明一个集合存储购物车数据
        List<CartInfo> cartInfoList = new ArrayList<>();
        //判断用户是否登录 userId登录的用户id  userTempId 未登录的用户id
        if (StringUtils.isEmpty(userId)) {
            //获取临时购物车数据
            cartInfoList = getCartList(userTempId);
            return cartInfoList;
        }
        //登录
        if (!StringUtils.isEmpty(userId)) {
            //1.查询未登录购物车中是否有数据
            List<CartInfo> cartTempList = getCartList(userTempId);
            //2. 当临时购物车数据不为空的情况下
            if (!CollectionUtils.isEmpty(cartTempList)) {
                //登录加未登录合并之后的数据
                cartInfoList = mergeToCartList(cartTempList, userId);
                //3. 删除未登录购物车数据
                deleteCartList(userTempId);
            }
            //如果未登录购物车没有数据呢？
            if (CollectionUtils.isEmpty(cartTempList) || StringUtils.isEmpty(userTempId)) {
                //获取登录购物车数据
                cartInfoList = getCartList(userId);
            }
            return cartInfoList;
        }
        return cartInfoList;
    }

    //删除购物车
    private void deleteCartList(String userTempId) {
        //未登录购物车数据: 一个存在缓存，一个存在数据库
        //删除数据，对数据进行DML操作，DML：insert，update，delete
        //先删除缓存
        //先获取购物车的key
        String cartKey = getCartKey(userTempId);
        Boolean aBoolean = redisTemplate.hasKey(cartKey);
        //如果缓存有key
        if (aBoolean) {
            //删除
            redisTemplate.delete(cartKey);
        }
        //删除数据库
        //delete from caer_info where user_id = userTempId
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id", userTempId));

    }

    //合并购物车
    private List<CartInfo> mergeToCartList(List<CartInfo> cartTempList, String userId) {
        //合并  登录+未登录
        //通过用户id获取登录数据
        List<CartInfo> cartLoginList = getCartList(userId);
        //合并的条件？是商品的id=skuId
        //以skuId为key，以cartInfo为value 的一个map集合
        Map<Long, CartInfo> cartInfoMapLogin = cartLoginList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        //循环未登录购物车数据
        for (CartInfo cartInfoNoLogin : cartTempList) {
            //取出未登录的商品id
            Long skuId = cartInfoNoLogin.getSkuId();
            //看登录购物车中是否有未登录的skuId，如果有就得到数据
            if (cartInfoMapLogin.containsKey(skuId)) { //有数据
                //获取登录数据
                CartInfo cartInfoLogin = cartInfoMapLogin.get(skuId);
                //商品数量相加
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum() + cartInfoNoLogin.getSkuNum());

                //细节操作：合并时需要判断是否有商品被勾选，说明未登录状态下有商品时选中状态
                if (cartInfoNoLogin.getIsChecked().intValue() == 1) {
                    //那么登录状态下的商品应该为选中的
                    cartInfoLogin.setIsChecked(1);
                }
                //更新数据库
                cartInfoMapper.updateById(cartInfoLogin);
            } else { //没有数据
                //把数据直接插到数据库
                //将用户id赋值,把未登录的临时id赋值为登录时的用户id
                cartInfoNoLogin.setUserId(userId);
                cartInfoMapper.insert(cartInfoNoLogin);
            }
        }
        //将合并之后的数据查询出来
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    //获取临时购物车中的数据,也可以获取登录购物车的数据
    private List<CartInfo> getCartList(String userId) {
        //声明一个集合存储数据
        List<CartInfo> cartInfoList = new ArrayList<>();
        //如果传进来的用户id为空
        if (StringUtils.isEmpty(userId)) {
            return cartInfoList;
        }
        //如果传进来的用户id不为空
        //查询缓存
        //先获取缓存的key
        String cartKey = getCartKey(userId);
        //根据当前的购物车key来获取缓存的数据
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        //判断集合是否为空
        if (null != cartInfoList && cartInfoList.size() > 0) {
            //返回来的数据就是 List<CartInfo>
            // 展示购物车数据的时候有一个排序的规则,根据更新时间
            //当前项目没有更新时间，模拟一个按照id排序  Comparator比较器
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            //返回当前集合
            return cartInfoList;
        } else {
            //缓存没有数据,走数据库并放入缓存
            // 根据userId来查询数据
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }
    //根据用户id查询数据库中的购物车数据并添加到缓存
    public List<CartInfo> loadCartCache(String userId) {
        //select * from cart_info where user_id = userId
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(cartInfoQueryWrapper);
        //如果数据库中没有数据
        if (CollectionUtils.isEmpty(cartInfoList)) {
            return cartInfoList;
        }
        //如果数据库中有购物车列表
        //将集合中的每个CartInfo循环放入缓存
        //一次放入多条数据
        HashMap<String, CartInfo> hashMap = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            //hash数据结构:hset(key,field,value) key=user:userId:cart field=skuId value=cartInfo字符串
            //hash数据结构:hmset(key,map) new Hashmap() map.put(field,value)
            //第一：只要走到这个方法说明缓存失效了,既然缓存已经失效了，我们要查询一下最新的价格，将数据放入缓存
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            //将数据放到map里
            hashMap.put(cartInfo.getSkuId().toString(), cartInfo);
        }
        //将map放到缓存
        //获取缓存的key
        String cartKey = getCartKey(userId);
        redisTemplate.opsForHash().putAll(cartKey, hashMap);
        //设置缓存的过期时间
        setCartKeyExpire(cartKey);
        //返回最终的数据
        return cartInfoList;
    }

    //购物车选中状态
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //需要更改当前的商品中isChecked状态
        //需知道商品存在哪个位置 一个位置mysql 二个redis
        //update cart_info set is_checked = isChecked where user_id = userId and sku_id = skuId
        //redis: 先将redis中的数据查询出来，。cartInfo。cartInfo.setIsChecked(isChecked)
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId).eq("sku_id", skuId);
        //第一个参数cartInfo:表示更新的内容，第二个wrapper设置更新的条件
        cartInfoMapper.update(cartInfo, cartInfoQueryWrapper);

        //获取缓存的key=user:userId:cart
        String cartKey = getCartKey(userId);
        //根据hash数据结构获取数据
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        //判断选中的商品在这个购物车中是否存在
        if (boundHashOperations.hasKey(skuId.toString())) {
            //获取当前商品id所对应的cartInfo
            CartInfo cartInfoUpd = (CartInfo) boundHashOperations.get(skuId.toString());
            //赋值选中状态
            cartInfoUpd.setIsChecked(isChecked);
            //将改后的cartInfo放入缓存
            boundHashOperations.put(skuId.toString(), cartInfoUpd);
            //每次修改缓存完成后需要设置一个过期时间
            setCartKeyExpire(cartKey);
        }
    }

    //删除购物车数据
    @Override
    public void deleteCart(Long skuId, String userId) {
        //删除两个地方 Mysql redis
        //获取缓存的key 最好先删除缓存 后删除数据库
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        //判断缓存中是否有商品的id
        if (boundHashOperations.hasKey(skuId.toString())) {
            //有key就删除
            boundHashOperations.delete(skuId.toString());
        }
        //删除数据库
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id", userId).eq("sku_id", skuId));
    }

    //根据用户id查询购物车列表，被选中的商品组成送货的清单
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        // 在展示购车列表中才能点击去结算下订单页面
        //直接查询缓存,因为在展示的过程中一定存在缓存中。
        List<CartInfo> cartInfoList = new ArrayList<>();
        //定义缓存的key
        String cartKey = getCartKey(userId);
        //获取缓存中的数据
        List<CartInfo> cartCacheList = redisTemplate.opsForHash().values(cartKey);
        if (null != cartCacheList && cartCacheList.size() > 0) {
            //循环遍历购物车中的数据
            for (CartInfo cartInfo : cartCacheList) {
                //获取被选中的数据
                if (cartInfo.getIsChecked().intValue() == 1) {
                    //将被选中的商品添加到集合中
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }
}
