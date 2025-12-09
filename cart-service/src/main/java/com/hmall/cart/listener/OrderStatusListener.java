package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import com.hmall.common.constants.MqConstants;
import com.hmall.common.utils.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：蒋青江
 * @date ：2025/7/30 10:30
 * @description ：订单状态监听器
 */
@Component
@AllArgsConstructor
public class OrderStatusListener {
    private final ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.CART_CLEAR_QUEUE, durable = "true"),
            exchange = @Exchange(value = MqConstants.CART_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstants.ROUTING_KEY_CART_CLEAR
    ))
    public void listenerOrderCreate(List<Long> itemIds, @Header("user-info") Long userId) {
        // 设置用户id
        UserContext.setUser(userId);
        cartService.removeByItemIds(itemIds);
        // 移除用户id
        UserContext.removeUser();
    }
}
