package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author Liyanbao
 * @create 2020-05-06 22:37
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;
    @Value("${ware.url}")
    private String WARE_URL;

    //保存数据
    @Override
    @Transactional //多表保存加上事务
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //保存orderInfo 在保存订单之前缺少数据：总金额，用户ID，订单状态,第三方交易编号,创建订单时间，订单过期时间，进程状态
        orderInfo.sumTotalAmount();
        //用户id在控制器中可以获取到
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //第三方交易编号 支付宝使用,可以保障支付的幂等性
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //创建订单时间
        orderInfo.setCreateTime(new Date());
        //过期时间 先获取日历对象
        Calendar calendar = Calendar.getInstance();
        //在日历的基础上添加一天
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //进程状态  与订单状态有绑定关系
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        //订单的主题描述，获取订单明细中的订单商品名称然后拼接
        //订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuilder stringBuilder = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            stringBuilder.append(orderDetail.getSkuName() + " ");
        }
        //长度处理
        if (stringBuilder.toString().length() > 150) {
            orderInfo.setTradeBody(stringBuilder.toString().substring(0, 150));
        } else {
            orderInfo.setTradeBody(stringBuilder.toString());
        }
        orderInfoMapper.insert(orderInfo);

        //循环得到里边的每个订单的明细
        for (OrderDetail orderDetail : orderDetailList) {
            //赋值orderId
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }
        //发送消息队列:发送延迟消息,如果在规定的时间内未付款，取消订单,根据订单id取消
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);
        //返回订单id
        return orderInfo.getId();
    }

    //生成流水号
    @Override
    public String getTradeNo(String userId) {
        //获取流水号
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        //将流水号放入缓存
        String tradeNoKey = "user:" + userId + ":tradeCode";
        //使用String 数据类型
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;
    }
    /**
     * 比较流水号 页面的比较缓存的
     * @param tradeNo 页面的
     * @param userId 获取缓存的
     * @return
     */
    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {
        //获取缓存流水号
        String tradeNoKey = "user:" + userId + ":tradeCode";
        //获取缓存里的流水号
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNo.equals(redisTradeNo);
    }

    //删除流水号
    @Override
    public void deleteTradeNo(String userId) {
        String tradeNoKey = "user:" + userId + ":tradeCode";
        //删除
        redisTemplate.delete(tradeNoKey);
    }

    //根据skuId和skuNum判断库存是否足够
    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        //远程调用库存系统 httpClientUtil 工具类
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //使用HttpClientUtil不使用feginclient是因为，仓库系统是一个单独的springBoot工程。没有springCloud工程
        //0表示没有库存  1表示有
        return "1".equals(result);
    }

    //根据订单id关闭过期订单
    @Override
    public void execExpiredOrder(Long orderId) {
        //关闭订单，将订单的状态order_status = CLOSE process_status = CLOSE
        //update order_info set order_status = CLOSE and process_status = CLOSE where id = orderId
        //后续还需要做一些关于订单的更新操作，例：支付完成order_status = PAID process_status = PAID
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        //发送消息关闭支付宝的订单
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
    }

    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        //update order_info set order_status = CLOSE and process_status = CLOSE where id = orderId
        //声明一个orderinfo
        OrderInfo orderInfo = new OrderInfo();
       //赋值更新的条件
        orderInfo.setId(orderId);
        //赋值更新的内容
        orderInfo.setProcessStatus(processStatus.name());
        //订单状态，从进程状态中获取
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfoMapper.updateById(orderInfo);
    }

    //根据订单Id 查询订单信息
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        //select * from order_info where id = orderId
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        //查询订单明细信息 select * from order_detail where order_id = orderId
        QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
        orderDetailQueryWrapper.eq("order_id", orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(orderDetailQueryWrapper);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    //发送消息给库存，通知减库存
    @Override
    public void sendOrderStatus(Long orderId) {
        //更新订单状态
        updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        //发送json字符串，通过接口文档，发送的数据都是从orderInfo中获取的部分字段组成的json
        String wareJson = initWareorder(orderId);
        //发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);

    }
    //获取发送的json字符串
    private String initWareorder(Long orderId) {
        //json字符串是由orderInfo组成
        //先获取orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        //将orderInfo中的部分字段先转化为Map集合再转化为json字符串
        Map map = initWareorder(orderInfo);
        return JSON.toJSONString(map);
    }
    //方法重载   将orderInfo中的部分字段先转化为Map集合
    public Map initWareorder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        List<Map> mapList = new ArrayList<>();
        //获取订单明细
        //details:[{skuId:101,skuNum:1,skuName:小米手64G},{skuId:201,skuNum:1,skuName:索尼耳机}]
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("skuId", orderDetail.getSkuId());
            hashMap.put("skuNum", orderDetail.getSkuNum());
            hashMap.put("skuName", orderDetail.getSkuName());
            mapList.add(hashMap);
        }
        map.put("details",mapList);//获取的集合里包含Map
        return map;
    }

    //拆单
    @Override
    public List<OrderInfo> orderSplit(long orderId, String wareSkuMap) {
        /**
         * 1.获取原始订单，要知道谁被拆
         * 2.将wareSkuMap参数转化为程序可以操作的对象。
         * 3.创建新的子订单
         * 4.给子订单进行赋值
         * 5.保存子订单
         * 6.将子订单添加到集合
         * 7.修改订单状态
         */
        List<OrderInfo> subOrderInfoList = new ArrayList<>();

        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        //wareSkuMap = [{wareId:1,skuIds:[2,10]},{wareId:2,skuIds:[3]}] --List<Map>
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        //判断当前的map集合是否为空
        if (null != mapList && mapList.size() > 0) {
            for (Map map : mapList) {
                //获取仓库id
                String wareId = (String) map.get("wareId");
                List<String> skuIds = (List<String>) map.get("skuIds");

                //新子订单
                OrderInfo subOrderInfo = new OrderInfo();
                //通过属性拷贝赋值
                BeanUtils.copyProperties(orderInfoOrigin, subOrderInfo);
                //拷贝注意id，主键id自增
                subOrderInfo.setId(null);
                //给原始订单id
                subOrderInfo.setParentOrderId(orderId);
                //仓库id赋值
                subOrderInfo.setWareId(wareId);
                //声明子订单明细
                List<OrderDetail> orderDetails = new ArrayList<>();
                //赋值子订单明细表,获取原始订单的明细集合
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                for (OrderDetail orderDetail : orderDetailList) {
                    //条件对比  商品id
                    for (String skuId : skuIds) {
                        //判断skuId是否存在
                        if (Long.parseLong(skuId) == orderDetail.getSkuId().intValue()) {
                            //把子订单名字保存
                            orderDetails.add(orderDetail);
                        }
                    }
                }
                //子订单明细赋值给子订单
                subOrderInfo.setOrderDetailList(orderDetails);
                //计算子订单的总金额
                subOrderInfo.sumTotalAmount();
                //保存子订单
                saveOrderInfo(subOrderInfo);
                //添加子订单到集合
                subOrderInfoList.add(subOrderInfo);
            }
        }
        //修改订单状态
        updateOrderStatus(orderId, ProcessStatus.SPLIT);
        return subOrderInfoList;
    }

    //关闭过期订单
    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        //关闭订单，将订单的状态order_status = CLOSE process_status = CLOSE
        //update order_info set order_status = CLOSE and process_status = CLOSE where id = orderId
        //后续还需要做一些关于订单的更新操作，例：支付完成order_status = PAID process_status = PAID
        //flag=1的时候只关闭orderInfo
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        if ("2".equals(flag)) {
            //发送消息关闭支付宝的订单,或者关闭交易记录
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
        }
    }
}
