package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Liyanbao
 * @create 2020-04-17 22:19
 */
@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private RabbitService rabbitService;


    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        // select * from baseCategory2 where Category1Id = ?
        QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        baseCategory2QueryWrapper.eq("category1_id", category1Id);
        List<BaseCategory2> baseCategory2s = baseCategory2Mapper.selectList(baseCategory2QueryWrapper);
        return baseCategory2s;
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        // select * from baseCategory3 where Category2Id = ?
        QueryWrapper<BaseCategory3> baseCategory3QueryWrapper = new QueryWrapper<>();
        baseCategory3QueryWrapper.eq("category2_id", category2Id);
        List<BaseCategory3> baseCategory3s = baseCategory3Mapper.selectList(baseCategory3QueryWrapper);
        return baseCategory3s;
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    //保存平台属性
    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        /**
         * base_attr_info :平台属性
         * base_attr_value : 平台属性值
         */
        if (baseAttrInfo.getId() != null) {
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else{
            //新增
            baseAttrInfoMapper.insert(baseAttrInfo); //平台属性
        }

        //修改时，需要先删除在新增数据
        //修改的条件 ：baseAttrValue.attrId = baseAttrInfo.id
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id", baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueQueryWrapper);

        //保存属性值,得到的页面所要保存的平台属性值集合
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList != null && attrValueList.size() > 0) {
            //往数据库循环添加数据
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }


    //根据平台属性ID获取平台属性,返回平台属性值集合
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //查询平台属性值的集合放入平台属性中
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    //根据属性id获取属性值
    private List<BaseAttrValue> getAttrValueList(Long attrId){
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id",attrId);
        List<BaseAttrValue> baseAttrValues = baseAttrValueMapper.selectList(baseAttrValueQueryWrapper);
        return baseAttrValues;
    }

    //根据三级分类信息分页查询商品的信息
    //根据分类id 进行分页查询数据
    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id", spuInfo.getCategory3Id());
        spuInfoQueryWrapper.orderByDesc("id");
        //第一个参数，pageParam，第二个参数 查询的条件
        IPage<SpuInfo> spuInfoIPage = spuInfoMapper.selectPage(pageParam, spuInfoQueryWrapper);
        return spuInfoIPage;
    }

    //查询所有销售属性的集合
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    //保存商品
    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        //SpuInfo 商品表
        spuInfoMapper.insert(spuInfo);
        //spuSaleAttr 商品属性表
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                //因为页面没有提供spuId
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //spuSaleAttrValue 商品属性值表
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        //因为页面没有提供spuId
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        //页面提交时没有给销售属性名字
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
        //spuImage 商品图片表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                //因为页面没有提供spuId
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }

    }

    //根据商品id查询图片的列表
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> spuImageQueryWrapper = new QueryWrapper<>();
        spuImageQueryWrapper.eq("spu_id", spuId);
        return  spuImageMapper.selectList(spuImageQueryWrapper);
    }

    //根据spuId查询销售属性的集合
    @Override
    public List<SpuSaleAttr> spuSaleAttrList(Long spuId) {
        //编写sql语句，多表关联查询，销售属性的值存在不同的表中
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
        return spuSaleAttrList;
    }

    //保存库存单元信息
    @Override
    @Transactional //多表切记加事务
    public void saveSkuInfo(SkuInfo skuInfo) {
//        skuInfo: 库存单元表
        skuInfoMapper.insert(skuInfo);
//        skuAttrValue: 库存单元与平台属性，平台属性值的关系
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0) {
            //循环插入
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                //查看页面是否未传入的属性字段(实体类有，页面没有的)
                //skuId：没有需要自己手动添加库存单元id
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }
//        skuSaleAttrValue: 销售属性，销售属性值
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {
            //循环插入
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                //查看页面是否未传入的属性字段(实体类有，页面没有的)
                //skuId 和 spuId
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                //spuId skuInfo实体类里边就有，获取即可
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
//        skuImage：库存单元图片表。
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() > 0) {
            //遍历插入
            for (SkuImage skuImage : skuImageList) {
                //skuId
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        //发送消息到rabbitmq
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuInfo.getId());

    }

    //查询skuInfo分页的信息
    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoPage) {
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.orderByDesc("id");
       return skuInfoMapper.selectPage(skuInfoPage, skuInfoQueryWrapper);
    }

    //上架商品
    @Override
    public void onSale(Long skuId) {
        //skuId 就是 skuInfo里边的主键id
        //上架状态 is_sale = 1
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(1);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        //发送消息到rabbitmq
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);

    }

    //下架商品
    @Override
    public void cancelSale(Long skuId) {
        //下架状态 is_sale = 0
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(0);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        //发送商品的下架操作
        //发送消息到rabbitmq
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);

    }

    //根据skuId 查询skuInfo
    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX) //sku:
    public SkuInfo getSkuInfo(Long skuId) {
        //利用redisson获取分布式锁查询数据。
       // return getSkuInfoRedisson(skuId);
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //定义存储商品的key
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //获取数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //说明要从数据取数据
            if (null == skuInfo) {
                //利用redisson定义分布式锁
                //定义分布式锁的key
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockKey);
                //上锁
                boolean flag = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (flag) {
                    try {
                        //业务逻辑代码
                        skuInfo = getSkuInfoDB(skuId);
                        //为防止缓存穿透
                        if (skuInfo == null) {
                            SkuInfo skuInfo1 = new SkuInfo();
                            //因为对象是空的。所有缓存的过期时间要短一点
                            redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        //将数据库中的数据放入缓存
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                } else {
                    //其他线程等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //再次调用查询的方法
                    return getSkuInfo(skuId);
                }
            } else {
                /**
                 * 如果用户查询一个在数据库中根本不存在的数据时，那么我们存一个空的对象放入缓存中，
                 * 实际上我们应该想要获取的是不是空对象，并且对象的属性也是有值的。
                 * 根据id判断这个对象的属性是不是空的,如果对象是空的话(属性是空的)，直接返回一个空null
                 */
                if (null == skuInfo.getId()) {
                    return null;
                }
                //走缓存
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //为了防止缓存宕机，让应用程序暂时查询数据库
        return getSkuInfoDB(skuId);
    }
    //利用redis获取分布式锁 查询数据
    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //1.定义key 存储商品(sku)的key=sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //2.去缓存中取数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //整合的流程
            if (skuInfo==null) {
                //从数据库查询数据,然后放入缓存：注意：需要添加锁
                //定义分布式锁的key=sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                //获取一个随机字符串
                String uuid = UUID.randomUUID().toString();
                //为了防止查询数据库击穿，执行分布式锁的命令
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                //判断是否添加锁成功,获取到分布式锁
                if (isExist) {
                    //获取到了分布式锁,走数据库查询数据并放入缓存,
                    System.out.println("获取到分布式锁");
                    skuInfo = getSkuInfoDB(skuId);
                    //判断数据库中的数据是否为空
                    if (skuInfo == null) {
                        //为了防止缓存穿透，赋值一个空的对象放入缓存
                        SkuInfo skuInfo1 = new SkuInfo();
                        //放入缓存的超时时间,注意：不需要太长.
                        redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    //从数据库查询出来的数据不为空还把skuInfo放进去
                    redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    //删除锁,定义Lua脚本
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptText(script);
                    //根据锁的key找锁的值匹配删除
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
                    //删除完后把数据返回
                    return skuInfo;
                } else {
                    //未获取到，其他的线程等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //记录缓存宕机的日志,让管理员处理
                        e.printStackTrace();
                    }
                    //睡醒了之后，调用查询的方法
                    return getSkuInfo(skuId);
                }

            } else {
                /**
                 * 如果用户查询一个在数据库中根本不存在的数据时，那么我们存一个空的对象放入缓存中，
                 * 实际上我们应该想要获取的是不是空对象，并且对象的属性也是有值的。
                 * 根据id判断这个对象的属性是不是空的,如果对象是空的话(属性是空的)，直接返回一个空null
                 */
                if (null == skuInfo.getId()) {
                    return null;
                }
                //走缓存
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //如果缓存宕机了那么我们优先让应用程序访问数据库。
        return getSkuInfoDB(skuId);
    }
    //根据skuId查询数据库中的数据
    private SkuInfo getSkuInfoDB(Long skuId) {
        //select * from slu_info where id = skuId
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            //select * from sku_image where sku_id = skuId
            //根据skuid查询图片列表的数据
            QueryWrapper<SkuImage> skuImageQueryWrapper = new QueryWrapper<>();
            skuImageQueryWrapper.eq("sku_id", skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageQueryWrapper);
            //查询出来的集合赋值给skuInfo
            skuInfo.setSkuImageList(skuImageList);
        }
        //这个skuInfo不但有skuName，weight，还有sku_default_image,imageList
        return skuInfo;
    }

    //根据三级分类id查询分类的信息
    @Override
    @GmallCache(prefix = "categoryViewByCategory3Id:")
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        //BaseCategoryView.id 与 BaseCategoryView.category3Id 是同一个值，那么id作为主键，也是category3Id作为主键
        return baseCategoryViewMapper.selectById(category3Id);
    }

     //根据skuId查询sku的价格
    @Override
    @GmallCache(prefix = "skuPrice:")
    public BigDecimal getSkuPrice(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        //只要价格
        if (skuInfo != null) {
            return skuInfo.getPrice();
        } else {
            //返回默认初始值
            return new BigDecimal("0");
        }
    }

    //根据skuId和spuId查询销售属性和销售属性值
    @Override
    @GmallCache(prefix = "spuSaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        //多表关联查询
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    //根据spuId 获取销售属性值id与skuId组成的一个集合数据
    @Override
    @GmallCache(prefix = "saleAttrValuesBySpu:")
    public Map getSaleAttrValuesBySpu(Long spuId) {
        HashMap<Object, Object> hashMap = new HashMap<>();
        //通过mapper查询数据
        List<Map> mapList = skuSaleAttrValueMapper.selectSaleAttrValuesBySpu(spuId);
        if (mapList != null && mapList.size() > 0) {
            for (Map map : mapList) {
                //数据库查询出来的value_ids作为key，skuId作为value
                hashMap.put(map.get("value_ids"), map.get("sku_id"));
            }
        }
        return hashMap;
    }

    //页面查询所有的分类数据
    @Override
    @GmallCache(prefix = "baseCategoryList:")
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> list = new ArrayList<>();
        /**
         * 1. 先获取到所有的分类数据 一级，二级，三级
         * 2.开始组装数据
         *      组装条件:分类id为主外键
         * 3. 将组装的数据封装到List<JSONObject>接口里
         */
        //分类数据在视图中
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //按照一级分类的id进行分组
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //定义一个index
        int index = 1;
        //获取一级分类的数据，包含一级分类的id和一级分类的名称
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            //获取一级分类id
            Long category1Id = entry1.getKey();
            //放入一级分类id
            //声明一个对象
            JSONObject category1 = new JSONObject();
            category1.put("index", index);
            category1.put("categoryId", category1Id);
            //存储categoryName数据
            List<BaseCategoryView> category2List = entry1.getValue();
            String category1Name = category2List.get(0).getCategory1Name();
            category1.put("categoryName", category1Name);

            //迭代index
            index++;
            //获取二级分类数据
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //准备给二级分类数据赋值,二级分类数据添加到一级分类 categoryChild 中
            List<JSONObject> category2Child = new ArrayList<>();
            //二级分类数据可能有很多条
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                //获取二级分类数据的id
                Long category2Id = entry2.getKey();
                //声明一个category2Id 二级分类的数据对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId", category2Id);
                //放入二级分类的名称，需要先获取二级分类所有的值。
                List<BaseCategoryView> category3List = entry2.getValue();
                category2.put("categoryName", category3List.get(0).getCategory2Name());
                //将二级分类数据添加到二级分类的集合中
                category2Child.add(category2);

                //获取三级数据,声明一个集合
                List<JSONObject> category3Child = new ArrayList<>();
                //循环 获取 category3List 数据
                category3List.stream().forEach(category3view->{
                    //声明一个category3Id 三级分类的数据对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId", category3view.getCategory3Id());
                    category3.put("categoryName", category3view.getCategory3Name());
                    //将三级分类数据添加到三级分类数据的集合
                    category3Child.add(category3);
                });
                //二级中还有catrgoryChild 是添加的三级分类的数据
                category2.put("categoryChild", category3Child);
            }
            //将二级分类数据放入一级分类里边
            category1.put("categoryChild", category2Child);
            //将所有的category1添加到集合中
            list.add(category1);
        }
        return list;
    }

    //根据品牌的id查询品牌数据
    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }

    //根据商品skuId 获取平台属性和平台属性值的名称
    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        //多表关联:sku_attr_value,base_attr_info,base_attr_value
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }
}
