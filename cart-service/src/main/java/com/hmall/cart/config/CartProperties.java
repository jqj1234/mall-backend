package com.hmall.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ：蒋青江
 * @date ：2025/7/28 12:27
 * @description ：购物车配置文件，从nacos中获取
 */
@Component
@ConfigurationProperties(prefix = "hm.cart")
@Data
public class CartProperties {
    private Integer maxAmount;
}
