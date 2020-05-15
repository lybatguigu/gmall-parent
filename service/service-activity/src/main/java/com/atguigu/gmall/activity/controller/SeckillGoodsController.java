package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-05-12 0:24
 */
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderFeignClient orderFeignClient;

    //查询所以秒杀商品数据
    @GetMapping("findAll")
    public Result findAll() {
        return Result.ok(seckillGoodsService.findAll());
    }

    //获取秒杀的商品的详情数据
    @GetMapping("getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId) {
        return Result.ok(seckillGoodsService.getSeckillGoodsById(skuId));
    }

    //seckill/queue.html?skuId='+this.skuId+'&skuIdStr='+skuIdStr
    //skuIdStr:下单码
    //获取下单码
    @GetMapping("/auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request) {
        //将用户id 进行MD5加密。加密后的字符串，就是下单码
        String userId = AuthContextHolder.getUserId(request);
        //用户要秒杀的商品
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoodsById(skuId);
        //判断这个商品要存在
        if (null != seckillGoods) {
            //获取下单码，在商品的秒杀时间范围内才能获取,在活动开始后结束前。
            Date curTime = new Date();//获取的当前系统时间
            if (DateUtil.dateCompare(seckillGoods.getStartTime(), curTime) && DateUtil.dateCompare(curTime, seckillGoods.getEndTime())) {
                //符合条件,下单码才能生成
                String skuIdStr = MD5.encrypt(userId);
                //保存skuIdStr返回给页面
                return Result.ok(skuIdStr);
            }
        }
        return Result.fail().message("获取下单码失败！");
    }

    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId, HttpServletRequest request) {
        //检查下单码   页面提交来的下单码
        String skuIdStr = request.getParameter("skuIdStr");
        //下单码生成的规则  是用MD5将用户的id加密生成的
        String userId = AuthContextHolder.getUserId(request);
        //根据后台规则生成的下单码
        String skuIdStrRes = MD5.encrypt(userId);
        if (!skuIdStr.equals(skuIdStrRes)) {
            //请求不合法 返回的一个码
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //获取校验状态位
        String status = (String) CacheHelper.get(skuId.toString());
        //判断状态位
        if (StringUtils.isEmpty(status)) {
            //请求不合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //是1的话可以抢购
        if ("1".equals(status)) {
            //记录当前谁在抢购商品,自定义一个用户抢购的实体类
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);
            //需要将用户放入队列中进行排队
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
        } else {
            //说明商品已售完
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        return Result.ok();
    }

    //轮询页面的状态
    @GetMapping(value = "auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId, HttpServletRequest request) {
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //调用服务层的方法
        return seckillGoodsService.checkOrder(skuId, userId);
    }

    //准备给下订单页面提供支持的
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request) {
        //显示收货人和地址，送货清单，总金额...
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //获取收货地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //获取用户购买的商品
        //key=seckill:orders field=userId value=用户秒杀得到的商品
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode) {
            return Result.fail().message("非法操作");
        }
        //获取用户要购买的商品。从订单取
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        //声明集合存储订单明细
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        //给订单明细赋值
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        orderDetail.setSkuNum(orderRecode.getNum());//在页面显示用户秒杀的商品,给当前订单的里边的num固定值
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());

        //还需要将数据保存到数据库
        detailArrayList.add(orderDetail);

        //计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();

        HashMap<String, Object> map = new HashMap<>();
        map.put("totalAmount", orderInfo.getTotalAmount());
        map.put("userAddressList", userAddressList);
        map.put("detailArrayList", detailArrayList);
        map.put("totalNum", orderRecode.getNum());
        return Result.ok(map);
    }

    //提交订单
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //赋值用户id
        orderInfo.setUserId(Long.parseLong(userId));
        //获取用户购买的数据 key=seckill:orders field=userId value=用户秒杀得到的商品
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode) {
            return Result.fail().message("非法操作");
        }
        //提交订单
        Long orderId = orderFeignClient.submitOrder(orderInfo);
        if (null == orderId) {
            return Result.fail().message("非法操作,下单失败！");
        }
        //删除对应的下单信息
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);
        //将用户真正下单的记录保存
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId, orderId.toString());

        return Result.ok(orderId);
    }
}
