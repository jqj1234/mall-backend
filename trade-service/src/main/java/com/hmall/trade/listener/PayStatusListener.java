package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.common.constants.MqConstants;
import com.hmall.common.domain.MultiDelayMessage;
import com.hmall.common.domain.R;
import com.hmall.trade.constants.TradeMqConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
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

/**
 * @author ：蒋青江
 * @date ：2025/7/30 9:48
 * @description ：
 */
@Component
public class PayStatusListener {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private PayClient payClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    // 监听队列中的订单id
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.PAY_SUCCESS_QUEUE, durable = "true"),
            exchange = @Exchange(value = MqConstants.PAY_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstants.PAY_SUCCESS_KEY
    ))
    public void listenerPayStatus(Long payOrderId) {
        orderService.markOrderPaySuccess(payOrderId);
    }

    /**
     * 监听延迟消息中的订单状态
     * 1、获取订单id
     * 2、查询订单；判断订单状态是否为 未支付；其它则不更新
     * 3、未支付订单，则查询订单信息
     * 3.1、如果支付成功，更新订单状态
     * 3.2、如果还有剩余延迟时间，则继续发送下一个延迟消息；
     * 4、如果超时则取消订单
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = TradeMqConstants.DELAY_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(value = TradeMqConstants.DELAY_EXCHANGE, type = ExchangeTypes.TOPIC, delayed = "true"),
            key = TradeMqConstants.DELAY_ORDER_ROUTING_KEY
    ))
    public void listenerPayTimeOut(MultiDelayMessage<Long> message) {
        Long orderId = message.getData();
        Order oldOrder = orderService.getById(orderId);
        // 1. 查询订单；判断订单状态是否为 未支付；其它则不更新
        if (oldOrder == null || oldOrder.getStatus() > 1) {
            return;
        }
        // 2. 未支付订单，则查询订单信息
        R<PayOrderDTO> result = payClient.queryPayOrderDTOByBizOrderNo(orderId);
        PayOrderDTO payOrderDTO = result.getData();
        // 0：待提交，1:待支付，2：支付超时或取消，3：支付成功
        if (payOrderDTO != null && payOrderDTO.getStatus() == 3) {
            // 3. 订单支付成功，更新订单状态
            orderService.markOrderPaySuccess(orderId);
            return;
        }
        // 查询是否还有剩余时间
        if (message.hasNextDelay()) {
            // 获取下一个延迟时间
            Integer delayTime = message.removeNextDelay();
            // 发送延迟消息
            rabbitTemplate.convertAndSend(TradeMqConstants.DELAY_EXCHANGE, TradeMqConstants.DELAY_ORDER_ROUTING_KEY, message, new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    message.getMessageProperties().setDelay(delayTime);
                    return message;
                }
            });
            return;
        }
        if(oldOrder.getOrderType() == 1){
            // 超时取消普通订单
            orderService.cancelOrder(orderId);
        }else if(oldOrder.getOrderType() == 2){
            // 超时取消秒杀订单
            orderService.cancelSeckillOrder(oldOrder);
        }
    }

}
