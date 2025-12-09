package com.hmall.trade.listener;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.MultiDelayMessage;
import com.hmall.common.domain.R;
import com.hmall.trade.constants.TradeMqConstants;
import com.hmall.trade.domain.dto.SeckillOrderMessage;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author ：蒋青江
 * @date ：2025/10/10 20:00
 * @description ：秒杀订单监听器
 */
@Slf4j
@Component
public class SeckillOrderListener {
    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderDetailService orderDetailService;

    @Autowired
    private ItemClient itemClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    // 监听秒杀订单创建队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = TradeMqConstants.SECKILL_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(value = TradeMqConstants.TRADE_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = TradeMqConstants.ROUTING_KEY_SECKILL_ORDER_CREATE
    ))
    @GlobalTransactional
    public void listenerSeckillOrderCreate(SeckillOrderMessage message) {
        Long orderId = message.getOrderId();
        // 幂等性校验：查询订单是否已存在
        Order existOrder = orderService.getById(orderId);
        if (existOrder != null) {
            log.info("秒杀订单已存在，无需重复处理，orderId: {}", orderId);
            return;
        }
        // 创建订单和订单详情
        // 查询商品详情
        R<ItemDTO> res = itemClient.queryItemById(message.getItemId());
        ItemDTO itemDTO = res.getData();

        // 查询秒杀商品详情，获取秒杀价格
        R<Integer> result = itemClient.querySeckillItemById(message.getSeckillItemId());
        Integer seckillPrice = result.getData();
        if (seckillPrice == null) {
            throw new RuntimeException("秒杀商品不存在");
        }

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(message.getUserId());
        order.setCreateTime(LocalDateTime.now());
        order.setStatus(1);
        order.setOrderType(2);
        order.setPaymentType(5);
        order.setTotalFee(seckillPrice);
        orderService.save(order);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        orderDetail.setItemId(message.getItemId());
        orderDetail.setName(itemDTO.getName());
        orderDetail.setNum(1);
        orderDetail.setPrice(seckillPrice);
        orderDetail.setImage(itemDTO.getImage());
        orderDetail.setSpec(itemDTO.getSpec());
        orderDetail.setCreateTime(LocalDateTime.now());
        orderDetail.setUpdateTime(LocalDateTime.now());
        orderDetailService.save(orderDetail);

        // 扣减商品库存
        Long seckillItemId = message.getSeckillItemId();
        R r = itemClient.deductStock(seckillItemId);
        if (r.getCode() == 0) {
            throw new RuntimeException("扣减库存失败");
        }


        // 发送延迟消息，用于订单支付超时取消
        // 秒杀订单支付超时取消，延迟5分钟 20s 、40s 、4min
        MultiDelayMessage<Long> msg = MultiDelayMessage.of(orderId, 40000, 1000 * 60 * 4);
        rabbitTemplate.convertAndSend(TradeMqConstants.DELAY_EXCHANGE, TradeMqConstants.DELAY_ORDER_ROUTING_KEY, msg, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                // 设置延迟时间
                message.getMessageProperties().setDelay(20000);
                return message;
            }
        });
    }
}
