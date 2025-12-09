package com.hmall.api.fallback;

import com.hmall.api.client.TradeClient;
import com.hmall.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ：蒋青江
 * @date ：2025/7/29 16:54
 * @description ：trade 服务调用失败的fallback
 */
@Slf4j
public class TradeClientFallback implements FallbackFactory<TradeClient> {
    @Override
    public TradeClient create(Throwable cause) {
        return new TradeClient() {
            @Override
            public void markOrderPaySuccess(Long orderId) {
                log.error("调用标记订单id为{}的订单支付成功失败：具体参数为：{}", orderId,cause);
                throw new BizIllegalException("调用标记订单id为{}的订单支付成功失败", cause);
            }
        };
    }
}
