package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-05-06 22:34
 */
public interface OrderService extends IService<OrderInfo> {

    //定义保存订单 由trade.html 看到需要返回一个订单id
    // 传入的参数看trade.html
    Long saveOrderInfo(OrderInfo orderInfo);

    //生成流水号，同时放入缓存
    String getTradeNo(String userId);

    /**
     * 比较流水号 用页面的和缓存做比较
     * @param tradeNo 页面的
     * @param userId 获取缓存的
     * @return
     */
    boolean checkTradeNo(String tradeNo, String userId);

    //删除缓存的流水号
    void deleteTradeNo(String userId);

    /**
     * 根据skuId，skuNum判断是否有足够的库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 根据订单id关闭过期订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 根据订单id修改订单的状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 通过订单id发送消息给库存，减库存
     * @param orderId
     */
    void sendOrderStatus(Long orderId);


    /**
     * 将orderInfo变为map集合
     * @param orderInfo
     * @return
     */
    Map initWareorder(OrderInfo orderInfo);

    /**
     * 拆单方法
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(long orderId, String wareSkuMap);

    /**
     *关闭过期订单
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId, String flag);
}
