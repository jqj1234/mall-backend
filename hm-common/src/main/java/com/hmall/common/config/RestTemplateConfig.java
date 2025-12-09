package com.hmall.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // 标识这是一个配置类
public class RestTemplateConfig {

    // 定义RestTemplate的Bean，供Spring容器管理
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}