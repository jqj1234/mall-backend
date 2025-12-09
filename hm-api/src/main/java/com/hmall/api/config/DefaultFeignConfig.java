package com.hmall.api.config;

import com.hmall.api.fallback.*;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * @author ：蒋青江
 * @date ：2025/7/27 11:20
 * @description ：日志级别
 */
public class DefaultFeignConfig {
    // 注册日志记录级别：
    // None - 默认级别，不记录日志
    // Basic - 记录请求和响应的请求头
    // Headers - 记录请求和响应的请求头和响应体
    // Full - 记录请求和响应的请求头、响应体和元数据
    @Bean
    public feign.Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    // 注册ItemClientFallback
    @Bean
    public ItemClientFallback itemClientFallback() {
        return new ItemClientFallback();
    }

    // 注册CartClientFallback
    @Bean
    public CartClientFallback cartClientFallback() {
        return new CartClientFallback();
    }

    // 注册TradeClientFallback
    @Bean
    public TradeClientFallback tradeClientFallback() {
        return new TradeClientFallback();
    }

    // 注册UserClientFallback
    @Bean
    public UserClientFallback userClientFallback() {
        return new UserClientFallback();
    }

    @Bean
    public EmbeddingClientFallback embeddingClientFallback() {
        return new EmbeddingClientFallback();
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                Long userId = UserContext.getUser();
                if (userId != null) {
                    requestTemplate.header("user-info", userId.toString());
                }
            }
        };
    }


}
