package com.hmall.api.fallback;

import com.hmall.api.client.CartClient;
import com.hmall.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;

/**
 * @author ：蒋青江
 * @date ：2025/7/29 16:51
 * @description ：购物车fallback
 */
@Slf4j
public class CartClientFallback implements FallbackFactory<CartClient> {
    @Override
    public CartClient create(Throwable cause) {
        return new CartClient() {
            @Override
            public void deleteCartItemByIds(Collection<Long> ids) {
                log.error("调用购物车id集合删除购物车项失败：具体参数为：{}", ids,cause);
                throw new BizIllegalException("调用购物车id集合删除购物车项失败");
            }
        };
    }
}
