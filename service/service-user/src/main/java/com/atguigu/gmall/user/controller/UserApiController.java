package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.service.UserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-05-06 18:07
 */
@RestController
@RequestMapping("api/user")
public class UserApiController {

    @Autowired
    private UserAddressService userAddressService;

    /**
     * 根据用户id查询用户地址列表
     *
     * @param userId
     * @return
     */
    @GetMapping("inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable String userId) {
        return userAddressService.findUserAddressListByUserId(userId);
    }

    //编辑
    @PutMapping("inner/updateAddressById/{id}")
    public Result updateAddressById(@PathVariable Long id) {
        UserAddress userAddress = new UserAddress();
        userAddress.setId(id);
        userAddressService.updateById(userAddress);
        return Result.ok();
    }
    //删除
    @DeleteMapping("inner/removeAddressById/{id}")
    public Result removeAddressById(@PathVariable Long id) {
        userAddressService.removeById(id);
        return Result.ok();
    }
}
