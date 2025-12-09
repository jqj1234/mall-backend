package com.hmall.gateway.filter;

import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author ：蒋青江
 * @date ：2025/7/28 9:55
 * @description ：自定义过滤器
 */
@Component
public class PrintAnyGatewayFilterFactory extends AbstractGatewayFilterFactory<PrintAnyGatewayFilterFactory.Config> {
    @Override
    public GatewayFilter apply(Config config) {
//        return new GatewayFilter() {
//            @Override
//            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//                System.out.println("PrintAnyGatewayFilterFactory的pre....");
//                return chain.filter(exchange);
//            }
//        };
        return new OrderedGatewayFilter(new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                // 接收来自配置文件的参数
                String a = config.getA();
                System.out.println("a = " + a);
                String b = config.getB();
                System.out.println("b = " + b);
                String c = config.getC();
                System.out.println("c = " + c);
                System.out.println("PrintAnyGatewayFilterFactory的pre....");

                return chain.filter(exchange);

            }
        }, 100);
    }

    // 接收参数的静态内部类
    @Data
    public static class Config {
        private String a;
        private String b;
        private String c;

    }

    // 配置接收来着配置文件的参数
    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("a", "b", "c");
    }

    public PrintAnyGatewayFilterFactory() {
        super(Config.class);
    }
}
