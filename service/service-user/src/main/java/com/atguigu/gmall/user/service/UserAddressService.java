package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-05-06 18:00
 */
public interface UserAddressService extends IService<UserAddress> {

    /**
     * 根据用户id查询用户地址
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(String userId);

}
