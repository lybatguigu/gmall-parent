package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-05-06 21:05
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductFeignClient productFeignClient;


    //auth/trade 走这个控制器必须需要登录
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        //获取用户地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //获取送货的清单
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        //声明一个送货清单的集合
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();

        int totalNum = 0;
        //送货清单应该是orderDetail
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            //记录件数，让每个商品的skuNum相加
            totalNum += cartInfo.getSkuNum();
            //将每个订单明细添加到当前的集合中
            orderDetailList.add(orderDetail);
        }
        //算出当前订单的总金额
        OrderInfo orderInfo = new OrderInfo();
        //将订单明细赋值给orderInfo
        orderInfo.setOrderDetailList(orderDetailList);
        //计算总金额
        orderInfo.sumTotalAmount();

        //将数据封装到map集合中
        HashMap<String, Object> map = new HashMap<>();
        //保存总金额,通过页面trade.html 可以找到页面对应存储的key=totalAmount
        map.put("totalAmount", orderInfo.getTotalAmount());
        //保存userAddressList
        map.put("userAddressList", userAddressList);
        //保存总数量totalNum 以spu为总件数
        //map.put("totalNum", orderDetailList.size());
        //以sku为总件数
        map.put("totalNum", totalNum);
        //保存订单明细 detailArrayList
        map.put("detailArrayList", orderDetailList);

        //生成流水号并保存到作用域提供页面使用
        String tradeNo = orderService.getTradeNo(userId);
        //保存tradeNo
        map.put("tradeNo", tradeNo);

        //返回数据集合
        return Result.ok(map);
    }

    //下订单控制器
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        //在保存之前将用户id赋值给orderInfo
        orderInfo.setUserId(Long.parseLong(userId));

        //在下订单之前做校验:流水号不能无刷新重复提交
        String tradeNo = request.getParameter("tradeNo");//页面存的tradeNo
        //调用比较的方法
        boolean flag = orderService.checkTradeNo(tradeNo, userId);
        //判断比较的结果
        if (!flag) {
            //提示
            return Result.fail().message("不能重复下单");
        }
        //比较完删除流水号
        orderService.deleteTradeNo(userId);

        //验证库存：用户购买的每个商品
        //循环订单明细中的每个商品
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (null != orderDetailList && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                //循环判断 result=true 表示有足够的库存
                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!result) {
                    //库存不够
                    return Result.fail().message(orderDetail.getSkuName() + "库存不足！");
                }
                //检查价格是否有变动 orderDetail.getOrderPrice() = skuInfo.getPrice()
                //如果比较结果不一致，提示用户重新下订单。购物车价格也需要变
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                    //判断不等于0说明价格变动
                    //更新购物车中的价格
                    cartFeignClient.loadCartCache(userId);
                    return Result.fail().message(orderDetail.getSkuName() + "商品价格变动，请重新下单");
                }

            }
        }
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }

    /**
     * 内部调用获取订单
     *
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
        return orderService.getOrderInfo(orderId);
    }

    //拆单的控制器  http://order.gmall.com/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request) {
        //获取传递的参数
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        //获取子订单集合,根据当前传过来的参数进行获取
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId), wareSkuMap);
        List<Map> mapArrayList = new ArrayList<>();
        //获取子订单集合的字符串
        for (OrderInfo orderInfo : subOrderInfoList) {
            //将子订单中的部分数据变为map再将map转换为字符串
            //一个map表示一个子订单对象，拆单有多个orderInfo，放入map中统一存储

            Map map = orderService.initWareorder(orderInfo);
            mapArrayList.add(map);
        }
        //返回子订单的集合字符串
        return JSON.toJSONString(mapArrayList);
    }

    //提交秒杀订单
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo) {
        //保存订单数据
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }
}
