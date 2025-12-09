package com.hmall.common.constants;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class MqConstants {
    // 清空购物车（购物车微服务监听消息，交易微服务创建订单后发送消息）
    public static final String CART_EXCHANGE_NAME = "cart.topic";
    public static final String ROUTING_KEY_CART_CLEAR = "cart.clear";
    public static final String CART_CLEAR_QUEUE = "cart.clear.queue";

    // 商品上下架 （搜索微服务监听消息，商品微服务发送消息）
    public static final String ITEM_EXCHANGE_NAME = "items.topic";
    public static final String ITEM_UP_KEY = "item.up";
    public static final String ITEM_DOWN_KEY = "item.down";


    // 支付成功后修改订单状态 (交易微服务监听消息,支付微服务发送消息)
    public static final String PAY_EXCHANGE_NAME = "pay.topic";
    public static final String PAY_SUCCESS_KEY = "pay.success";
    public static final String PAY_SUCCESS_QUEUE = "mark.order.pay.queue";

}