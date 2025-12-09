package com.hmall.trade.constants;

public interface TradeMqConstants {
    // 订单支付超时交换机
    String DELAY_EXCHANGE = "trade.delay.topic";
    // 订单支付超时队列，用于检查订单支付状态
    String DELAY_ORDER_QUEUE = "trade.order.delay.queue";
    // 订单支付超时路由
    String DELAY_ORDER_ROUTING_KEY = "order.query";


    // 秒杀订单创建交换机
    String TRADE_EXCHANGE_NAME = "trade.topic";
    // 秒杀订单创建路由
    String ROUTING_KEY_SECKILL_ORDER_CREATE = "seckill.order.create";
    // 秒杀订单队列
    String SECKILL_ORDER_QUEUE = "seckill.order.queue";
}