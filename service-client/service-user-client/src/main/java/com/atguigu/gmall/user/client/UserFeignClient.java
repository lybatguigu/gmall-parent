package com.atguigu.gmall.user.client;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.client.impl.UserDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author Liyanbao
 * @create 2020-05-06 18:18
 */
@FeignClient(name = "service-user",fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {

    @GetMapping("api/user/inner/findUserAddressListByUserId/{userId}")
    List<UserAddress> findUserAddressListByUserId(@PathVariable String userId);
}
