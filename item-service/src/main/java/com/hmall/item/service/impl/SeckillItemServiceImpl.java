package com.hmall.item.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.domain.R;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.constants.SeckillMqConstants;
import com.hmall.common.constants.SeckillRedisPrefix;
import com.hmall.item.constants.SeckillStatus;
import com.hmall.item.domain.dto.SeckillItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.SeckillItem;
import com.hmall.item.domain.vo.CursorResult;
import com.hmall.item.domain.vo.SeckillItemVO;
import com.hmall.item.mapper.ItemMapper;
import com.hmall.item.mapper.SeckillItemMapper;
import com.hmall.item.service.ISeckillItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ：蒋青江
 * @date ：2025/10/10 14:18
 * @description ：秒杀商品服务实现类
 */
@Service
@RequiredArgsConstructor
public class SeckillItemServiceImpl extends ServiceImpl<SeckillItemMapper, SeckillItem> implements ISeckillItemService {
    private final ItemMapper itemMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;


    @Transactional
    @Override
    public R addSeckillItem(SeckillItemDTO seckillItemDTO) {
        // 秒杀时间设置合理性校验
        if (seckillItemDTO.getBeginTime().isAfter(seckillItemDTO.getEndTime())) {
            return R.error("秒杀结束时间不能早于开始时间");
        }
        // 秒杀开始时间不能早于当前时间+1小时
        if (seckillItemDTO.getBeginTime().isBefore(LocalDateTime.now().plusMinutes(30))) {
            return R.error("秒杀开始时间需要晚于半小时后");
        }
        // 秒杀开始时间不能晚于当前时间+3天
        if (seckillItemDTO.getBeginTime().isAfter(LocalDateTime.now().plusDays(3))) {
            return R.error("秒杀开始时间不能晚于3天后");
        }

        if (seckillItemDTO.getStock() <= 0) {
            return R.error("库存不能小于0");
        }

        // 严格限制：秒杀不能超过 14 天（即 14 * 24 * 60 * 60 * 1000 毫秒）
        long maxDurationMillis = 14L * 24 * 60 * 60 * 1000; // 14天的毫秒数
        long actualDurationMillis = Duration.between(seckillItemDTO.getBeginTime(), seckillItemDTO.getEndTime()).toMillis();

        if (actualDurationMillis > maxDurationMillis) {
            return R.error("秒杀活动持续时间不能超过14天");
        }
        // 1、校验商品
        Item item = itemMapper.selectById(seckillItemDTO.getItemId());
        // 商品不存在或者状态不是1
        if (item == null || item.getStatus() != 1) {
            return R.error("商品不存在或者状态是不在售卖中");
        }
        // 同一个商品（item_id）已经有秒杀，还未结束
        LambdaQueryChainWrapper<SeckillItem> queryChainWrapper = this.lambdaQuery()
                .eq(SeckillItem::getItemId, item.getId())
                .in(SeckillItem::getStatus, SeckillStatus.NOT_STARTED.getCode(), SeckillStatus.RUNNING.getCode());
        if (queryChainWrapper.count() > 0) {
            return R.error("该商品已经配置过秒杀活动，尚未结束！");
        }

        SeckillItem seckillItem = BeanUtils.copyBean(seckillItemDTO, SeckillItem.class);
        seckillItem.setItemId(item.getId());
        seckillItem.setStatus(1);
        seckillItem.setCreateTime(LocalDateTime.now());
        seckillItem.setUpdateTime(LocalDateTime.now());


        // 保存秒杀商品信息到数据库
        save(seckillItem);

        // 缓存秒杀商品信息
        long beginTime = seckillItem.getBeginTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = seckillItem.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();


        // 发送延迟消息，提前10分钟将秒杀信息缓存 Redis Hash
        int delayBeginTime = (int) (beginTime - System.currentTimeMillis() - 600000);
        rabbitTemplate.convertAndSend(SeckillMqConstants.DELAY_EXCHANGE, SeckillMqConstants.DELAY_BEGIN_SECKILL_ROUTING_KEY, seckillItem.getId(),
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        // 设置延迟时间
                        message.getMessageProperties().setDelay(Math.max(delayBeginTime, 0));
                        return message;
                    }
                });

        // 7. 发送延迟消息，用于当到达结束时间时，删除ZSet中秒杀商品信息
        long delayEndTime = endTime - System.currentTimeMillis();
        if (delayEndTime < 0) {
            throw new RuntimeException("添加秒杀商品失败");
        }
        rabbitTemplate.convertAndSend(SeckillMqConstants.DELAY_EXCHANGE, SeckillMqConstants.DELAY_END_SECKILL_ROUTING_KEY, seckillItem.getId(),
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        // 设置延迟时间
                        message.getMessageProperties().setDelay((int) delayEndTime);
                        return message;
                    }
                });

        return R.ok("添加秒杀商品成功");
    }

    @Override
    public R getActiveSeckillItemsWithCursor(Long lastBeginTime, Long lastItemId, Integer limit) {
        // 1. 当前时间戳（毫秒），过滤已结束的秒杀
        long currentTime = System.currentTimeMillis();

        // 2. 构建Redis ZSet查询参数
        Set<String> itemIdSet;
        if (lastBeginTime == null || lastItemId == null) {
            // 2.1 首次查询：查询开始时间 >= 当前时间的商品，按开始时间正序取前N条
            itemIdSet = stringRedisTemplate.opsForZSet().rangeByScore(
                    SeckillRedisPrefix.SECKILL_ACTIVE,
                    0, // min：当前时间（只取未开始/进行中的）
                    currentTime, // max：无穷大
                    0, // offset：从第0条开始
                    limit // count：取limit条
            );
        } else {
            // 2.2 分页查询：以上一次的最后一条记录为游标
            // ZSet的score是beginTime，当score相同时（同一时间多个商品），按itemId字典序排序
            // 条件：score > lastBeginTime OR (score == lastBeginTime AND itemId > lastItemId)
            // 实现方式：先按score范围查询，再过滤itemId
            Set<String> tempSet = stringRedisTemplate.opsForZSet().rangeByScore(
                    SeckillRedisPrefix.SECKILL_ACTIVE,
                    lastBeginTime, // min：上一次的开始时间
                    currentTime, // max：无穷大
                    0, // offset：从第0条开始（后续过滤）
                    limit + 10 // 多取10条防止过滤后不足（应对同score多itemId场景）
            );

            if (CollUtil.isEmpty(tempSet)) {
                return R.ok(new CursorResult<>(Collections.emptyList(), null, null));
            }

            // 过滤出符合游标条件的itemId：score > lastBeginTime 或 (score == lastBeginTime 且 itemId > lastItemId)
            itemIdSet = tempSet.stream()
                    .filter(itemId -> {
                        Double score = stringRedisTemplate.opsForZSet().score(SeckillRedisPrefix.SECKILL_ACTIVE, itemId);
                        if (score == null) return false; // 已过期或被删除的商品

                        long beginTime = score.longValue();
                        if (beginTime > lastBeginTime) {
                            return true;
                        } else if (beginTime == lastBeginTime) {
                            // itemId转为Long比较（确保数字类型的正确排序）
                            return Long.parseLong(itemId) > lastItemId;
                        }
                        return false;
                    })
                    .limit(limit)
                    .collect(Collectors.toSet());
        }

        // 3. 无数据返回
        if (CollUtil.isEmpty(itemIdSet)) {
            return R.ok(new CursorResult<>(Collections.emptyList(), null, null));
        }

        // 4. 批量查询Redis Hash中的商品详情，并过滤已结束的秒杀
        List<SeckillItemVO> seckillItemVOList = new ArrayList<>();
        Long nextLastBeginTime = null;
        Long nextLastItemId = null;

        for (String itemIdStr : itemIdSet) {
            Long itemId = Long.parseLong(itemIdStr);
            String hashKey = SeckillRedisPrefix.SECKILL_ITEM + itemId;

            // 4.1 查询Hash中的所有字段
            Map<Object, Object> hashMap = stringRedisTemplate.opsForHash().entries(hashKey);
            if (CollUtil.isEmpty(hashMap)) {
                // Hash数据不存在（已过期），从ZSet中删除该记录
                continue;
            }

            // 4.2 转换为VO对象
            SeckillItemVO vo = convertHashToVO(hashMap);
            if (vo == null) continue;

            // 4.3 过滤已结束的秒杀（双重校验，避免缓存过期未清理）
            long endTimeMillis = vo.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (endTimeMillis < currentTime) {
                // 清理过期缓存
                stringRedisTemplate.opsForZSet().remove(SeckillRedisPrefix.SECKILL_ACTIVE, itemIdStr);
                stringRedisTemplate.delete(hashKey);
                continue;
            }

            seckillItemVOList.add(vo);

            // 4.4 记录最后一条数据的游标信息（用于下一页查询）
            nextLastBeginTime = vo.getBeginTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            nextLastItemId = itemId;
        }

        // 5. 按开始时间正序排序（确保分页顺序一致）
        seckillItemVOList.sort(Comparator.comparing(SeckillItemVO::getBeginTime)
                .thenComparing(SeckillItemVO::getItemId));

        // 6. 构建返回结果
        CursorResult<SeckillItemVO> result = new CursorResult<>();
        result.setData(seckillItemVOList);
        // 若返回数据量等于limit，说明可能有下一页，返回游标；否则游标为null
        if (seckillItemVOList.size() == limit) {
            result.setNextLastBeginTime(nextLastBeginTime);
            result.setNextLastItemId(nextLastItemId);
        } else {
            result.setNextLastBeginTime(null);
            result.setNextLastItemId(null);
        }

        return R.ok(result);
    }

    @Override
    public R getSeckillItemById(Long id) {
        Map<Object, Object> item = stringRedisTemplate.opsForHash().entries(SeckillRedisPrefix.SECKILL_ITEM + id);

        return R.ok(item);
    }

    @Override
    public R deductStock(Long id) {
        // 1. 减库存 update seckill_item set stock = stock - 1 where id = ? and stock > 0
        this.lambdaUpdate()
                .setSql("stock = stock - 1")
                .eq(SeckillItem::getId, id)
                .gt(SeckillItem::getStock, 0)
                .update();
        return R.ok("扣减秒杀商品库存成功");
    }

    /**
     * 查询秒杀商品价格
     */
    @Override
    public R<Integer> querySeckillItemPriceById(Long id) {
        SeckillItem seckillItem = this.getById(id);
        if (seckillItem != null) {
            return R.ok(seckillItem.getSeckillPrice());
        }
        return R.error("秒杀商品不存在");
    }

    /**
     * 恢复秒杀商品库存
     */
    @Override
    public R restoreSeckillStock(Long id) {
        this.lambdaUpdate()
                .eq(SeckillItem::getItemId, id)
                .eq(SeckillItem::getStatus, 2)
                .setSql("stock = stock + 1")
                .update();
        return R.ok();
    }

    /**
     * Redis Hash数据转换为VO对象
     */
    private SeckillItemVO convertHashToVO(Map<Object, Object> hashMap) {
        try {
            SeckillItemVO vo = new SeckillItemVO();

            // 1. 商品ID（Long）
            vo.setItemId(Long.parseLong(hashMap.getOrDefault("itemId", "0").toString()));
            vo.setSeckillItemId(Long.parseLong(hashMap.getOrDefault("seckillItemId", "0").toString()));
            // 2. 品牌（String）
            vo.setBrand(hashMap.getOrDefault("brand", "").toString());
            // 3. 商品名称（String）
            vo.setName(hashMap.getOrDefault("name", "").toString());
            // 4. 商品图片（String）
            vo.setImage(hashMap.getOrDefault("image", "").toString());
            // 4. 秒杀价格（Integer，原缓存中是String类型）
            vo.setSeckillPrice(Integer.parseInt(hashMap.getOrDefault("price", "0").toString()));
            // 5. 库存（Integer）
            vo.setStock(Integer.parseInt(hashMap.getOrDefault("stock", "0").toString()));
            // 6. 开始时间（Long时间戳 → LocalDateTime）
            long beginTimeMillis = Long.parseLong(hashMap.getOrDefault("beginTime", "0").toString());
            vo.setBeginTime(timestampToLocalDateTime(beginTimeMillis));
            // 7. 结束时间（Long时间戳 → LocalDateTime）
            long endTimeMillis = Long.parseLong(hashMap.getOrDefault("endTime", "0").toString());
            vo.setEndTime(timestampToLocalDateTime(endTimeMillis));

            // 校验核心字段有效性（避免无效数据）
            if (vo.getItemId() <= 0 || vo.getSeckillPrice() < 0 || vo.getStock() < 0) {
                return null;
            }

            return vo;
        } catch (NumberFormatException e) {
            log.error("Hash数据转换VO失败：数字格式异常", e);
            return null;
        } catch (Exception e) {
            log.error("Hash数据转换VO失败", e);
            return null;
        }
    }

    /**
     * 工具方法：Long时间戳（毫秒）→ LocalDateTime
     */
    private LocalDateTime timestampToLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }
}
