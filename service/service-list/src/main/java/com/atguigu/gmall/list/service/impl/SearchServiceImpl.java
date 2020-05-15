package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Liyanbao
 * @create 2020-04-27 20:31
 */
@Service
public class SearchServiceImpl implements SearchService {

    //mysql中的数据，通过feign远程调用
    @Autowired
    private ProductFeignClient productFeignClient;
    //引入这个类操作eslasticsearch
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    //上架商品
    @Override
    public void upperGoods(Long skuId) {

        //将实体类Goods中的数据放入es里
        Goods goods = new Goods();
        //给goods赋值
        //需要先通过productFeignClient来查询skuInfo的信息数据
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //赋值
        goods.setId(skuInfo.getId());
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        goods.setTitle(skuInfo.getSkuName());
        goods.setPrice(skuInfo.getPrice().doubleValue());
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        goods.setPrice(skuPrice.doubleValue());
        goods.setCreateTime(new Date());
        //查询品牌的数据
        BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
        if (null != trademark) {
            goods.setTmId(trademark.getId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());
        }
        //获取分类的数据,通过skuInfo中的数据获取三级分类的id
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        if (null != categoryView) {
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory1Name(categoryView.getCategory1Name());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory3Name(categoryView.getCategory3Name());
        }
        //给平台属性赋值
        //通过远程调用service-product中的查询方法来获取平台属性和平台属性值数据
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        if (null != attrList && attrList.size() > 0) {
            //循环获取里边的数据 ,baseAttrInfo相当于一个平台属性的对象
            //将每一个销售属性存起来
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
                //赋值平台属性对象
                SearchAttr searchAttr = new SearchAttr();
                //存储平台属性的id
                searchAttr.setAttrId(baseAttrInfo.getId());
                //elasticsearch中需要存储的平台属性名
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                //存储平台属性值的名称
                //先获取到平台属性值的数据
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                //获取平台属性值的名称
                String valueName = attrValueList.get(0).getValueName();
                searchAttr.setAttrValue(valueName);
                //将每一个的销售属性和销售属性值返回去
                return searchAttr;
            }).collect(Collectors.toList());

            //保存数据
            goods.setAttrs(searchAttrList);
        }

        //保存
        goodsRepository.save(goods);
    }

    //下架商品
    @Override
    public void lowerGoods(Long skuId) {
        //下架就是删除elasticsearch中的数据
        goodsRepository.deleteById(skuId);
    }

    //更新热度排名
    @Override
    public void incrHotScore(Long skuId) {
        //借助redisTemplate 记录商品被访问的次数
        // redis 注意两点:第一点：数据类型（五种） 第二点：key是谁需要定义好
        //数据类型:zset 用来做排名
        String hotKey = "hotScore";
        //hotScore增长之后的值
        Double hotScore = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        //定义规则，符合规则时更新一次elasticsearch
        if (hotScore % 10 == 0) {
            //更新elasticsearch,先查询出来直接赋值保存返回的是Optional里边的每一行数据
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(Math.round(hotScore));
            //把最新的数据保存到elasticsearch
            goodsRepository.save(goods);
        }
    }

    //根据用户输入的条件查询数据
    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        //1.制作dsl语句
        // 2. 执行dsl语句
        //3. 获取执行的结果
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //引入操作elasticsearch客户端的类
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //获取执行之后的数据,在这个方法中可以将总记录数放入total中
        SearchResponseVo responseVo = parseSearchResult(response);
        //设置分页相关的数据
        responseVo.setPageSize(searchParam.getPageSize());
        responseVo.setPageNo(searchParam.getPageNo());
        //设置总条数可以从es中获取hits.total 所有这可以省略不要
        //responseVo.setTotal();
        //设置总页数   10  3  4  |  9  3  3
        //传统的公式：(total%pageSize==0?total/pageSize:total/pageSize+1)
        //新的公式。
        long totalPages = (responseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        responseVo.setTotalPages(totalPages);
        return responseVo;
    }

    //制作返回结果集
    private SearchResponseVo parseSearchResult(SearchResponse response) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //赋值
//        private List<SearchResponseTmVo> trademarkList;
//        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
//        private List<Goods> goodsList = new ArrayList<>();
//        private Long total;//总记录数
//        private Integer pageSize;//每页显示的内容
//        private Integer pageNo;//当前页面
//        private Long totalPages;

        //品牌数据通过聚合得到的
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        //获取品牌id  Aggregation接口中并没有获取到桶(buckets)的方法需要进行转化
        //ParsedLongTerms是Aggregation的一个实现类
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        //从桶中获取数据
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            //获取品牌id
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            searchResponseTmVo.setTmId(Long.parseLong(((Terms.Bucket) bucket).getKeyAsString()));
            //获取品牌的名称
            Map<String, Aggregation> tmIdSubAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            //品牌名称的Agg 品牌数据类型是字符串String
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdSubAggregationMap.get("tmNameAgg");
            //获取到了品牌的名称并赋值
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            //获取品牌的logo
            ParsedStringTerms tmlogoUrlAgg = (ParsedStringTerms) tmIdSubAggregationMap.get("tmLogoUrlAgg");
            String tmLogoUrl = tmlogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);
            //返回品牌
            return searchResponseTmVo;
        }).collect(Collectors.toList());
        //赋值给品牌数据
        searchResponseVo.setTrademarkList(trademarkList);

        //获取平台属性数据，应该也是从聚合中获取
        //attrAgg 数据类型是nested 需要转换一下。
        //Aggregation attrAgg = aggregationMap.get("attrAgg");
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        //获取平台属性id的数据 attrIdAgg  改为Long类型
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        //判断桶的集合不可为空
        if (null != buckets && buckets.size() > 0) {
            //循环遍历
            List<SearchResponseAttrVo> attrsList = buckets.stream().map(bucket -> {
                //获取平台属性对象
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                //获取attrNameAgg 中的数据 名称数据类型是字符串
                ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
                //赋值平台属性的名称
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                searchResponseAttrVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());

                //赋值平台属性值集合 先要获取attrValueAgg
                ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> valueBuckets = attrValueAgg.getBuckets();
                //获取valueBuckets集合里的数据
                //根据流式编程转化为map，map的key就是桶的key,通过key获取里边的数据，并变为list集合
                List<String> valueList = valueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                searchResponseAttrVo.setAttrValueList(valueList);
                //返回平台属性对象
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            //赋值平台属性
            searchResponseVo.setAttrsList(attrsList);
        }

        //获取商品数据 goodsList
        //声明一个存储商品的集合
        ArrayList<Goods> goodsList = new ArrayList<>();
        //品牌数据需要从查询结果集中获取
        SearchHits hits = response.getHits();//结果集
        SearchHit[] subHits = hits.getHits();
        if (null != subHits && subHits.length > 0) {
            //循环遍历数据
            for (SearchHit subHit : subHits) {
                //获取商品的json字符串
                String goodsJson = subHit.getSourceAsString();
                //将json字符串变成Goods.class
                Goods goods = JSONObject.parseObject(goodsJson, Goods.class);
                //获取商品时，如果是按照商品的名称查询时，商品名称显示的时候应高亮显示
                //从高亮中获取我的商品名称
                if (subHit.getHighlightFields().get("title") != null) {
                    //说明当前用户查询时按照全文检索的方式查询的。
                    //将高亮商品名称赋值给goods
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];//取0的原因，因为高亮是title对应的只有一个值
                    goods.setTitle(title.toString());
                }
                //添加商品到集合里
                goodsList.add(goods);
            }
        }
        searchResponseVo.setGoodsList(goodsList);

        //总记录数
        searchResponseVo.setTotal(hits.totalHits);

        return searchResponseVo;
    }

    //自动生成dsl语句
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //在dsl最外层的大括号 称为查询器:{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();//查询器
        //声明一个queryBuidler对象 query下的bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断查询的关键字不为空
        if (StringUtils.isNotEmpty(searchParam.getKeyword())) {
            //创建一个queryBuidler对象
            //MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("title", searchParam.getKeyword());
            /**
             * .operator(Operator.AND) --> demo: 用户查询荣耀手机的时候，es如何执行的？
             * 1.先分词 荣耀和手机 加上这个AND 表示这个title中这两个字段必须存在。
             * 如果还有一个OR 表示这个title有其中一个即可
             */
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            //查询过滤下有一个must
            boolQueryBuilder.must(title);
        }
        //品牌的设置。需先得到品牌数据   trademark=2:华为
        //2相当于tmId(品牌的id)  华为相当于tmName
        String trademark = searchParam.getTrademark();
        if (StringUtils.isNotEmpty(trademark)) {
            //不为空说明按照品牌查询   把trademark=2:华为使用 : 分隔
            String[] split = StringUtils.split(trademark, ":");
            //判断分隔之后的数据格式是否正确
            if (null != split && split.length == 2) {
                //按照品牌的id来过滤
                TermQueryBuilder tmId = QueryBuilders.termQuery("tmId", split[0]);
                boolQueryBuilder.filter(tmId);
            }
        }
        //设置分类id过滤 通过一级分类id和二级分类id和三级分类id
        if (null != searchParam.getCategory1Id()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }
        if (null != searchParam.getCategory2Id()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (null != searchParam.getCategory3Id()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }
        //平台属性 用户是不是输入的平台属性查询
        //props=23:4G:运行内存
        //平台属性Id 平台属性值名称 平台属性名
        String[] props = searchParam.getProps();
        if (null != props && props.length > 0) {
            //循环遍历
            for (String prop : props) {
                //props=23:4G:运行内存 需要分隔出来。
                String[] split = StringUtils.split(prop, ":");
                //判断分隔之后的格式是否正确
                if (null != split && split.length == 3) {
                    //构建查询语句 query下有bool
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    //在nested下还有一个bool
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    //匹配查询
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", split[1]));
                    //将subBoolQuery放入nested下的bool里边
                    //nested查询。是将平台属性值和平台属性名作为独立的查询
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));

                    //将boolQuery放入总的查询器中
                    boolQueryBuilder.filter(boolQuery);

                }
            }
        }
        //执行query方法
        searchSourceBuilder.query(boolQueryBuilder);
        //构建分页的数据 分页和最外层的query是同一级的，属性查询器的对象
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);//开始的条数
        searchSourceBuilder.size(searchParam.getPageSize());

        //排序 1:hotScore 2:price
        String order = searchParam.getOrder();
        //判断不为空
        if (StringUtils.isNotEmpty(order)) {
            //再次进行分割
            String[] split = StringUtils.split(order, ":");
            //判断分割后的格式  如果传的是1:hotScore 走if  但是如果传的是 1  或者 只传一个 price 就走else
            if (null != split && split.length == 2) {
                //设置排序规则。看点的是1还是2
                //定义一个排序字段
                String field = null; //选择的1就变为hotScore 2就是price
                switch (split[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                //设置排序   排序也属于查询器的
                searchSourceBuilder.sort(field, "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
            } else {
                //默认走根据热度进行降序的排列
                searchSourceBuilder.sort("hotScore", SortOrder.DESC);
            }
        }
        //高亮显示 还是与query并列的
        //声明一个高亮对象然后设置高亮的规则
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");//商品的名称高亮显示
        highlightBuilder.preTags("<span style=color:red>");//前缀
        highlightBuilder.postTags("</span>");//后缀
        searchSourceBuilder.highlighter(highlightBuilder);

        //设置聚合
        //聚合品牌
        //tmIdAgg:自定义的  tmId:品牌id   在 field 下还有品牌的名称   subAggregation:表示子聚合
        //tmNameAgg:自定义的 tmName:品牌名称
        //品牌的logo和名称同是属于品牌id下的   所以是并级
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        //将聚合规则添加到查询器
        searchSourceBuilder.aggregation(termsAggregationBuilder);

        //平台属性
        //先设置nested聚合
        //attrIdAgg:自定义的 attrs.attrId:平台属性id
        // 名称是在子id的下边  attrNameAgg:自定义的  attrs.attrName:平台属性名
        //attrValueAgg:自定义的   attrs.attrValue：平台属性值
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","attrs")
            .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
            .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
            .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
            ));

        //设置有效的数据，查询的时候，那些字段需要显示？
        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImg", "title", "price"}, null);

        //GET /goods/info/_search
        //设置索引库index,type
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        //打印dsl语句
        String query = searchSourceBuilder.toString();
        System.out.println("dsl语句=>" + query);

        return searchRequest;
    }
}
