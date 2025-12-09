package com.hmall.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ：蒋青江
 * @date ：2025/7/30 9:41
 * @description ：
 */
@Configuration
// 如果存在RabbitTemplate类，则创建MqConfig
@ConditionalOnClass(RabbitTemplate.class)
public class MqConfig {
    // 注册json消息转换器
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        // 设置每个消息都携带一个id
        converter.setCreateMessageIds(true);
        return converter;
    }
}
