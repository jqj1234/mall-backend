package com.hmall.item.constants;

/**
 * @author ：蒋青江
 * @date ：2025/12/2 11:24
 * @description ：秒杀商品
 */
public class SeckillMqConstants {
    // 秒杀商品缓存创建或删除延时交换机
    public static final String DELAY_EXCHANGE = "seckill.delay.topic";
    // 构建秒杀商品缓存延时路由键
    public static final String DELAY_BEGIN_SECKILL_ROUTING_KEY = "seckill.begin";
    // 构建秒杀商品缓存延时队列
    public static final String DELAY_BEGIN_SECKILL_QUEUE = "seckill.begin.delay.queue";


    // 删除秒杀商品缓存延时路由键
    public static final String DELAY_END_SECKILL_ROUTING_KEY = "seckill.end";
    // 删除秒杀商品缓存延时队列
    public static final String DELAY_END_SECKILL_QUEUE = "seckill.end.delay.queue";

}
