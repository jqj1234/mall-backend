package com.hmall.api.client;

import com.hmall.api.dto.AddressDTO;
import com.hmall.api.fallback.UserClientFallback;
import com.hmall.common.domain.R;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ：蒋青江
 * @date ：2025/7/27 16:15
 * @description ：用户微服务接口
 */
@FeignClient(value = "user-service", fallbackFactory = UserClientFallback.class)
public interface UserClient {
    @PutMapping("/users/money/deduct")
    public R deductMoney(@RequestParam("pw") String pw, @RequestParam("amount") Integer amount);

}
