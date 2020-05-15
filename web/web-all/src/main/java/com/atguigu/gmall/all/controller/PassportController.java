package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Liyanbao
 * @create 2020-05-03 20:54
 */
@Controller
public class PassportController {

    @GetMapping("login.html")
    public String login(HttpServletRequest request) {
        //从哪里点击的登录应该跳回到哪
        String originUrl = request.getParameter("originUrl");
        //需要保存originUrl,前台需要跳转originUrl: [[${originUrl}]]
        request.setAttribute("originUrl", originUrl);
        return "login";
    }
}
