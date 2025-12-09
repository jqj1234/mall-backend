package com.hmall.api.client;

import com.hmall.api.fallback.TradeClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * @author ：蒋青江
 * @date ：2025/7/27 16:16
 * @description ：交易微服务接口
 */
@FeignClient(value = "trade-service",fallbackFactory = TradeClientFallback.class)
public interface TradeClient {
    @PutMapping("/orders/{orderId}")
    public void markOrderPaySuccess(@PathVariable("orderId") Long orderId);
}
