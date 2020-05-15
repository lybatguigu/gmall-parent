package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author Liyanbao
 * @create 2020-05-02 1:12
 */
@Service
public class UserServiceImpl implements UserService {

    //服务层要执行sql：select * from user_info where userName = ? and password = ?
    //引入mapper
    @Autowired
    private UserInfoMapper userInfoMapper;



    //登录的方法 还需将密码进行加密
    @Override
    public UserInfo login(UserInfo userInfo) {
        //密码需要加密 MD5加密 获取用户在页面输入的密码
        String passwd = userInfo.getPasswd();
        //对输入的密码进行加密,再跟数据库进行匹配
        String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        //执行sql,用户名和密码在数据库只能是唯一的，返回的是一个对象
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name", userInfo.getLoginName()).eq("passwd", newPwd);
        //数据库查询之后的对象
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        if (null != info) {
            //说明数据库中有当前的用户
            return info;
        }
        //说明数据库中没有当前的用户,返回null
        return null;
    }
}
