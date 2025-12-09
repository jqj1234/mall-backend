package com.hmall.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author ：蒋青江
 * @date ：2025/7/28 9:45
 * @description ：自定义全局过滤器
 */
@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        System.out.println("pre请求路径：" + request.getPath());
        return chain.filter(exchange).then(Mono.fromRunnable(()->{
            System.out.println("post请求路径：" + request.getPath());
        }));
    }

    @Override
    public int getOrder() {
        // 优先级，数字越小优先级越高
        return 0;
    }
}
