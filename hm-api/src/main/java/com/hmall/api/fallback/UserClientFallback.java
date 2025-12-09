package com.hmall.api.fallback;

import com.hmall.api.client.UserClient;
import com.hmall.api.dto.AddressDTO;
import com.hmall.common.domain.R;
import com.hmall.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ：蒋青江
 * @date ：2025/7/29 17:31
 * @description ：user
 */
@Slf4j
public class UserClientFallback implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public R deductMoney(String pw, Integer amount) {
                log.error("调用扣减用户余额失败：具体参数为：{}", amount,cause);
//                throw new BizIllegalException("调用扣减用户余额失败",cause);
                return R.error("扣减用户余额失败");
            }
        };
    }
}
