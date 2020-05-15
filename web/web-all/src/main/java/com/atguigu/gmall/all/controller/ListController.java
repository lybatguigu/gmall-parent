package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Liyanbao
 * @create 2020-04-30 23:15
 */
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;


    //@RequestMapping("list.html")
    @GetMapping("list.html")
    public String list(SearchParam searchParam, Model model) {
        //将数据保存,在index.html渲染
        //数据从哪来？service-list
        Result<Map> result = listFeignClient.search(searchParam);
        model.addAllAttributes(result.getData());
        //页面渲染的时候需要urlParam:记录拼接的url参数列表用的
        //searchParam接收用户查询的条件都拼接到url上
        String urlParam = makeUrlParam(searchParam);
        //获取品牌传递过来的参数
        String trademark = getTrademark(searchParam.getTrademark());
        //获取平台属性的
        List<Map<String, String>> propsList = getMakeProps(searchParam.getProps());
        //获取排序规则
        Map<String, Object> order = getOrder(searchParam.getOrder());

        //保存用户查询的数据
        model.addAttribute("searchParam", searchParam);
        model.addAttribute("urlParam", urlParam);
        model.addAttribute("trademarkParam", trademark);
        //存储平台属性
        model.addAttribute("propsParamList", propsList);
        //存储排序规则
        model.addAttribute("orderMap", order);
        return "list/index";
    }

    //记录查询的条件拼接的url参数
    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //判断是否根据关键字查询的
        if (searchParam.getKeyword() != null) {
            //记录keyword
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //判断是否根据分类id查询
        //http://list.gmall.com/list.html?category1Id=2
        if (searchParam.getCategory1Id() != null) {
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        if (searchParam.getCategory2Id() != null) {
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (searchParam.getCategory3Id() != null) {
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        //判断是否根据品牌查询  &trademark=2
        if (searchParam.getTrademark() != null) {
            if (searchParam.getTrademark().length() > 0) {
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //判断是否根据平台属性值  &props=1:2800-4499:价格
        if (searchParam.getProps() != null) {
            for (String prop : searchParam.getProps()) {
                if (urlParam.length() > 0) {
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        //记录的拼接条件
        String urlParamStr = urlParam.toString();
        return "list.html?" + urlParamStr;
    }

    //获取品牌名称 品牌:品牌名称   trademark=2:华为
    private String getTrademark(String trademark) {
        if (trademark != null && trademark.length() > 0) {
            //将字符串分割trademark=2:华为
            //String[] split = trademark.split(":");
            String[] split = StringUtils.split(trademark, ":");
            //判断符合数据格式
            if (null != split || split.length == 2) {
                return "品牌:" + split[1];
            }
        }
        return "";
    }

    //获取平台属性值过滤得到面包屑
    //传入的参数是多个是个数组
    private List<Map<String, String>> getMakeProps(String[] props) {
        List<Map<String, String>> list = new ArrayList<>();
        //判断传过来的参数是否为空
        if (null != props && props.length > 0) {
            //是数组要循环里边的数据
            for (String prop : props) {
                //prop每个值的格式是2:6.35-6.44英寸:屏幕尺寸 由id和属性值和属性名组成
                //分割
                //String[] split = StringUtils.split(prop, ":");
                //得到字符串数据
                String[] split = prop.split(":");
                //循环数组中的每个数据.符合数据格式
                if (null != split && split.length == 3) {
                    //将字符串中的每个值放到map中
                    // 保存的是平台属性id和平台属性值的名称和平台属性名
                    HashMap<String, String> map = new HashMap<>();
                    map.put("attrId", split[0]);
                    map.put("attrValue", split[1]);
                    map.put("attrName", split[2]);

                    //将map放到集合里
                    list.add(map);
                }
            }
        }
        return list;
    }

    //获取排序规则
    private Map<String, Object> getOrder(String order) {
        HashMap<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(order)) {
            //order=2:desc
            String[] split = order.split(":");
            //符合格式
            if (null != split && split.length == 2) {
                //type代表的是用户点击的那个字段
                map.put("type", split[0]);
                //sort代表的是排序的规则
                map.put("sort", split[1]);
            }
        } else {
            //如果没有order排序规则
            map.put("type", "1");
            //sort代表的是排序的规则
            map.put("sort", "asc");
        }
        return map;
    }
}
