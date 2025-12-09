package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.api.fallback.PayClientFallback;
import com.hmall.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * @author ：蒋青江
 * @date ：2025/7/31 9:15
 * @description ：支付微服务接口
 */
@FeignClient(value = "pay-service", fallbackFactory = PayClientFallback.class, configuration = DefaultFeignConfig.class)
public interface PayClient {

    @GetMapping("/pay-orders/biz/{id}")
    public R<PayOrderDTO> queryPayOrderDTOByBizOrderNo(@PathVariable Long id);

    /**
     * 根据订单id标记支付单为超时或取消
     * @param orderId 订单id
     */
    @PutMapping("/pay-orders/{id}")
    public void markPayOrderTimeoutOrCancel(@PathVariable("id") Long orderId);

}
