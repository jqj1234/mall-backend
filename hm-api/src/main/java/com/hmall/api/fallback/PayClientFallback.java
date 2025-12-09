package com.hmall.api.fallback;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.common.domain.R;
import com.hmall.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ：蒋青江
 * @date ：2025/7/31 9:13
 * @description ：pay 服务调用失败的fallback
 */
@Slf4j
public class PayClientFallback implements FallbackFactory<PayClient> {

    @Override
    public PayClient create(Throwable cause) {
        return new PayClient() {
            @Override
            public R<PayOrderDTO> queryPayOrderDTOByBizOrderNo(Long id) {
                log.error("远程调用PayClient.queryPayOrderByBizOrderNo 失败；参数{}", id, cause);
                return R.error("远程调用PayClient.queryPayOrderByBizOrderNo失败");
            }

            @Override
            public void markPayOrderTimeoutOrCancel(Long orderId) {
                log.error("远程调用PayClient.markPayOrderTimeoutOrCancel 失败；参数{}", orderId, cause);
                throw new BizIllegalException("远程调用PayClient.markPayOrderTimeoutOrCancel失败");
            }
        };
    }
}
