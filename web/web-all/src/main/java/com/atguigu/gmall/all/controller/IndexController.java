package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Liyanbao
 * @create 2020-04-27 0:51
 */
@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    //注入模板引擎
    @Autowired
    private SpringTemplateEngine templateEngine;

    /**
     * 如果没有静态页面，自己生成一个静态页面数据模板
     */
    @GetMapping("caeateHtml")
    @ResponseBody
    public Result caeataHtml() throws IOException {
        //创建时必须有三级分类数据
        Result result = productFeignClient.getBaseCategoryList();
        Context context = new Context();
        //result.getData() 表示所有三级分类数据
        context.setVariable("list", result.getData());
        //生成一个在D盘根目录index.html的页面
        FileWriter fileWriter = new FileWriter("D:\\index.html");
        //使用模板引擎
        templateEngine.process("index/index.html", context, fileWriter);
        return Result.ok();
    }

    //访问首页,走的以上的引擎模板
//    @GetMapping({"/", "index.html"})
//    public String index1() {
//        return "index";
//    }
    //用缓存数据访问首页
    @GetMapping({"/", "index.html"})
    public String index(HttpServletRequest request) {
        //获取首页分类的数据
        Result result = productFeignClient.getBaseCategoryList();
        request.setAttribute("list", result.getData());
        return "index/index";
    }
}
