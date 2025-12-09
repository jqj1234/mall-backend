package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.fallback.CartClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

/**
 * @author ：蒋青江
 * @date ：2025/7/27 15:58
 * @description ：
 */
@FeignClient(value = "cart-service",fallbackFactory = CartClientFallback.class, configuration = DefaultFeignConfig.class)
public interface CartClient {

    /**
     * 根据id批量删除购物车
     * @param ids
     */
    @DeleteMapping("/carts")
    void deleteCartItemByIds(@RequestParam("ids") Collection<Long> ids);
}
