package com.hmall.item.listener;

import com.hmall.item.constants.SeckillMqConstants;
import com.hmall.common.constants.SeckillRedisPrefix;
import com.hmall.item.constants.SeckillStatus;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.SeckillItem;
import com.hmall.item.service.IItemService;
import com.hmall.item.service.ISeckillItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ：蒋青江
 * @date ：2025/12/2 11:22
 * @description ：
 */
@Component
@Slf4j
public class SeckillCacheListener {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ISeckillItemService seckillItemService;

    @Autowired
    private IItemService itemService;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = SeckillMqConstants.DELAY_BEGIN_SECKILL_QUEUE, durable = "true"),
            exchange = @Exchange(value = SeckillMqConstants.DELAY_EXCHANGE, type = ExchangeTypes.TOPIC, delayed = "true"),
            key = SeckillMqConstants.DELAY_BEGIN_SECKILL_ROUTING_KEY
    )
    )
    public void listenerSeckillCacheBuild(Long seckillItemId) {
        // 构建秒杀缓存
        SeckillItem seckillItem = seckillItemService.getById(seckillItemId);
        if (seckillItem == null) {
            log.warn("秒杀活动不存在: {}", seckillItemId);
            return;
        }
        if (seckillItem.getStatus() != SeckillStatus.NOT_STARTED.getCode()) {
            return;
        }
        Long itemId = seckillItem.getItemId();
        Item item = itemService.getById(itemId);

        long beginTime = seckillItem.getBeginTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = seckillItem.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();


        Map<String, String> seckillInfo = new HashMap<>();
        seckillInfo.put("seckillItemId", String.valueOf(seckillItemId)); // 秒杀活动id
        seckillInfo.put("itemId", String.valueOf(itemId)); // 秒杀商品id
        seckillInfo.put("brand", item.getBrand());
        seckillInfo.put("name", item.getName());
        seckillInfo.put("image", item.getImage());
        seckillInfo.put("price", String.valueOf(seckillItem.getSeckillPrice())); // 秒杀价格
        seckillInfo.put("stock", String.valueOf(seckillItem.getStock()));       // 库存
        seckillInfo.put("beginTime", String.valueOf(beginTime)); // 开始时间（毫秒时间戳）
        seckillInfo.put("endTime", String.valueOf(endTime));   // 结束时间（毫秒时间戳）

        String redisKey = SeckillRedisPrefix.SECKILL_ITEM + item.getId();
        stringRedisTemplate.opsForHash().putAll(redisKey, seckillInfo);
        stringRedisTemplate.expire(redisKey, Duration.ofMillis(endTime - System.currentTimeMillis()));


        // 添加到活跃秒杀列表（ZSet）中
        stringRedisTemplate.opsForZSet().add(SeckillRedisPrefix.SECKILL_ACTIVE, itemId.toString(), beginTime);

        // 修改状态为正在秒杀
        seckillItemService.lambdaUpdate()
                .set(SeckillItem::getStatus, SeckillStatus.RUNNING.getCode())
                .eq(SeckillItem::getStatus, SeckillStatus.NOT_STARTED.getCode())
                .eq(SeckillItem::getId, seckillItemId)
                .update();

        log.info("构建秒杀缓存成功：{}", seckillItem);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = SeckillMqConstants.DELAY_END_SECKILL_QUEUE, durable = "true"),
            exchange = @Exchange(value = SeckillMqConstants.DELAY_EXCHANGE, type = ExchangeTypes.TOPIC, delayed = "true"),
            key = SeckillMqConstants.DELAY_END_SECKILL_ROUTING_KEY
    )
    )
    public void listenerSeckillCacheDelete(Long seckillItemId) {
        // 删除秒杀缓存
        SeckillItem seckillItem = seckillItemService.getById(seckillItemId);
        // 从zset中删除member为itemId的元素
        String itemIdStr = String.valueOf(seckillItem.getItemId());
        // 从活跃秒杀列表（ZSet）中移除
        stringRedisTemplate.opsForZSet().remove(SeckillRedisPrefix.SECKILL_ACTIVE, itemIdStr);
        // 删除Redis中的秒杀商品信息
        stringRedisTemplate.delete(SeckillRedisPrefix.SECKILL_ITEM + itemIdStr);
        // 删除Redis中的秒杀一人一单的set集合
        stringRedisTemplate.delete(SeckillRedisPrefix.SECKILL_ORDER + itemIdStr);
        // 修改秒杀商品状态
        seckillItemService.lambdaUpdate()
                .set(SeckillItem::getStatus, SeckillStatus.FINISHED.getCode())
                .eq(SeckillItem::getStatus, SeckillStatus.RUNNING.getCode())
                .eq(SeckillItem::getId, seckillItemId);
        log.info("删除秒杀缓存成功：{}", seckillItem);
    }
}
