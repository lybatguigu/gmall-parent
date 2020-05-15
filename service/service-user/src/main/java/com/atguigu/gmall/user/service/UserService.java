package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

/**
 * @author Liyanbao
 * @create 2020-05-02 1:09
 */
public interface UserService {

    //登录数据的接口:select * from user_info where userName = ? and password = ?

    /**
     * 登录的方法
     * 需要对密码进行加密操作
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);
}
