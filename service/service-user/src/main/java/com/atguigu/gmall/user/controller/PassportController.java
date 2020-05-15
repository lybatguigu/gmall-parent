package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.aspectj.weaver.patterns.IToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Liyanbao
 * @create 2020-05-03 18:32
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportController {

    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    //@RequestBody 在login.html中有一个登录方法，登录方法提交的数据是this.user{提交过来的是json数据}
    //@RequestBody将json数据转化为java对象
    //编写映射路径
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo) {
        //暂时不返回页面,保存数据,保存用户的信息
        //返回页面，在其他控制器处理的

        UserInfo info = userService.login(userInfo);
        //说明用户在数据库中存在
        if (null != info) {
            //声明一个map集合，记录相关的数据
            HashMap<String, Object> map = new HashMap<>();
            //根据sso的分析过程用户登录的信息放入缓存中，才能保证每个模块都能访问到用户的信息
            //声明token
            String token = UUID.randomUUID().toString().replace("-", "");
            //记录token
            map.put("token", token);
            //记录用户昵称
            map.put("nickName", info.getNickName());
            //定义key  = user:login:token   value = userId
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(userKey, info.getId().toString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            //返回
            return Result.ok(map);
        } else {
            return Result.fail().message("用户名和密码不正确");
        }
    }

    //退出登录
    @GetMapping("logout")
    public Result logout(HttpServletRequest request) {
        //因为缓存中存储用户数据的时候需要一个token，删除需要token组成key
        //当登录成功后token放到了cookie中和header中
        //从header中获取token
        String token = request.getHeader("token");
        //直接删除缓存中的数据
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX + token);
        //清空一下cookie中的数据

        return Result.ok();
    }
}
