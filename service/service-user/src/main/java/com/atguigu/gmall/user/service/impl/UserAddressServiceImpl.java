package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-05-06 18:01
 */
@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

    //服务层调用mapper层
    @Autowired
    private UserAddressMapper userAddressMapper;

    //根据用户id查询List<UserAddress> 地址
    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {

        return userAddressMapper.selectList(new QueryWrapper<UserAddress>().eq("user_id", userId));
    }

}
