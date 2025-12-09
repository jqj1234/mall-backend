package com.hmall.search.listener;

import com.hmall.common.constants.MqConstants;
import com.hmall.search.service.ISearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author ：蒋青江
 * @date ：2025/8/1 15:40
 * @description ：商品状态监听器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemStatusListener {

    private final ISearchService searchService;


    // 监听商品上架消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.item.up.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.ITEM_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstants.ITEM_UP_KEY
    ))
    public void itemUpMsg(Long id) {
        log.info("监听到商品上架：" + id);
        searchService.saveItemById(id);
    }

    // 监听商品下架消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.item.down.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.ITEM_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstants.ITEM_DOWN_KEY
    ))
    public void itemDownMsg(Long id) {
        log.info("监听到商品下架：" + id);
        searchService.deleteItemById(id);
    }
}
