package com.hmall.item.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.constants.MqConstants;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.mapper.ItemMapper;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void deductStock(List<OrderDetailDTO> items) {
        String sqlStatement = "com.hmall.item.mapper.ItemMapper.updateStock";
        boolean r = false;
        try {
            r = executeBatch(items, (sqlSession, entity) -> sqlSession.update(sqlStatement, entity));
        } catch (Exception e) {
            log.error("更新库存异常", e);
            throw new BizIllegalException("更新库存异常！");
        }
        if (!r) {
            throw new BizIllegalException("库存不足！");
        }
    }

    @Override
    public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
        return BeanUtils.copyList(listByIds(ids), ItemDTO.class);
    }


    @Override
    public void updateItemStatus(Long id, Integer status) {
        Item item = new Item();
        item.setId(id);
        item.setStatus(status);
        this.updateById(item);

        // 判断状态
        String routingKey = status == 1 ? MqConstants.ITEM_UP_KEY : MqConstants.ITEM_DOWN_KEY;

        // 发送消息上架/下架商品
        rabbitTemplate.convertAndSend(MqConstants.ITEM_EXCHANGE_NAME, routingKey, id);

        // 如果是上架，需要重构向量检索
//        if (status == 1) {
//            // 发送消息更新图像检索tensor
//            // rabbitTemplate.convertAndSend(MqConstants.ITEM_IMAGE_EXCHANGE_NAME, MqConstants.ITEM_IMAGE_ROUTING_KEY, id);
//            rabbitTemplate.convertAndSend(MqConstants.ITEM_IMAGE_EXCHANGE_NAME, MqConstants.ITEM_IMAGE_ROUTING_KEY, id, new MessagePostProcessor() {
//                @Override
//                public Message postProcessMessage(Message message) throws AmqpException {
//                    // 设置延迟时间
//                    message.getMessageProperties().setDelay(3000);
//                    return message;
//                }
//            });
//        }

    }
}
