package com.hmall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.CartClient;
import com.hmall.api.client.ItemClient;
import com.hmall.api.client.PayClient;
import com.hmall.api.client.UserClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.constants.MqConstants;
import com.hmall.common.constants.SeckillRedisPrefix;
import com.hmall.common.domain.MultiDelayMessage;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.R;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.constants.TradeMqConstants;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.dto.SeckillOrderFormDTO;
import com.hmall.trade.domain.dto.SeckillOrderMessage;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.domain.vo.OrderVO;
import com.hmall.trade.mapper.OrderMapper;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderLogisticsService;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final RabbitTemplate rabbitTemplate;
    private final IOrderDetailService orderDetailService;
    private final PayClient payClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final IdentifierGenerator identifierGenerator;

    @Override
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        R<List<ItemDTO>> r = itemClient.queryItemByIds(itemIds);
        List<ItemDTO> items = r.getData();
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.7.订单类型 普通订单
        order.setOrderType(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
//        cartClient.deleteCartItemByIds(itemIds);
        // 改为使用MQ异步清空购物车
        rabbitTemplate.convertAndSend(MqConstants.CART_EXCHANGE_NAME, MqConstants.ROUTING_KEY_CART_CLEAR, itemIds,
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        // 通过消息头设置用户信息
                        message.getMessageProperties().setHeader("user-info", UserContext.getUser());
                        return message;
                    }
                });

        // 测试分布式事务
//        int i = 1 / 0;

        // 4.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        // 5.发送延迟消息,用于订单超时取消,多级延迟：20s、40s、19min、10min，总共30min
        MultiDelayMessage<Long> msg = MultiDelayMessage.of(order.getId(), 40000, 1000 * 60 * 19, 1000 * 60 * 10);
        rabbitTemplate.convertAndSend(TradeMqConstants.DELAY_EXCHANGE, TradeMqConstants.DELAY_ORDER_ROUTING_KEY, msg, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                // 设置延迟时间
                message.getMessageProperties().setDelay(20000);
                return message;
            }
        });

        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
//        // 查询订单
//        Order oldOrder = getById(orderId);
//
//        // 如果订单状态不是待支付，则不更新
//        if(oldOrder == null || oldOrder.getStatus() > 1){
//            return;
//        }
//        Order order = new Order();
//        order.setId(orderId);
//        order.setStatus(2);
//        order.setPayTime(LocalDateTime.now());
//        updateById(order);


        // 业务幂等处理
        // update order set status = 2, pay_time = now() where id = orderId and status = 1
        lambdaUpdate().set(Order::getStatus, 2)
                .set(Order::getPayTime, LocalDateTime.now())
                .eq(Order::getStatus, 1)
                .eq(Order::getId, orderId)
                .update();
    }

    /**
     * 取消订单
     * - 将查询该订单对于的商品列表
     * - 将上述商品对于的购买数量返还到 商品微服务item-service  ---》优化：利用已有的一些接口实现 返还库存
     * - 更新交易订单的状态为取消
     *
     * @param orderId 订单id
     */
    @Override
    @GlobalTransactional
    public void cancelOrder(Long orderId) {
        List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, orderId).list();
        ArrayList<OrderDetailDTO> itemDTOS = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            Long itemId = orderDetail.getItemId();
            Integer num = orderDetail.getNum();
            // 将购买数量改为负数
            itemDTOS.add(new OrderDetailDTO().setItemId(itemId).setNum(-num));

        }
        if (CollUtils.isEmpty(itemDTOS)) {
            return;
        }
        // 恢复库存
        itemClient.deductStock(itemDTOS);
        // 更新订单为取消
        lambdaUpdate()
                .set(Order::getStatus, 5)
                .set(Order::getCloseTime, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)
                .update();
        // 将支付单修改为超时取消状态
        payClient.markPayOrderTimeoutOrCancel(orderId);
    }


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    private static final DefaultRedisScript<Object> ROLLBACK_SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);

        ROLLBACK_SECKILL_SCRIPT = new DefaultRedisScript<>();
        ROLLBACK_SECKILL_SCRIPT.setLocation(new ClassPathResource("rollback_seckill.lua"));
        ROLLBACK_SECKILL_SCRIPT.setResultType(Object.class);
    }

    /**
     * 创建秒杀订单
     *
     * @param seckillOrderFormDTO
     * @return
     */
    @Override
    public R<Long> createSeckillOrder(SeckillOrderFormDTO seckillOrderFormDTO) {
        Long itemId = seckillOrderFormDTO.getItemId();
        Long seckillItemId = seckillOrderFormDTO.getSeckillItemId();
        if (itemId == null || seckillItemId == null) {
            return R.error("参数错误！");
        }


        // 执行lua脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                itemId.toString(),
                UserContext.getUser().toString());
        // 判断结果是否小于0，小于0秒杀失败
        if (result == null) {
            return R.error("秒杀失败！");
        } else if (result == -1) {
            return R.error("请勿重复购买！");
        }

        // 预先生成订单id，用于返回前端轮询查询
        Long orderId = identifierGenerator.nextId(new Order()).longValue();

        // 有购买资格
//        Map<Object, Object> itemMap = stringRedisTemplate.opsForHash().entries("seckill:item:" + itemId);
//        String price = (String) itemMap.get("price");
//        String name = (String) itemMap.get("name");
//        String image = (String) itemMap.get("image");
//        String spec = (String) itemMap.get("spec");

//        // 创建订单
//        Order order = new Order();
//        Long orderId = identifierGenerator.nextId(new Order()).longValue();
//        order.setId(orderId);
//        order.setTotalFee(Integer.valueOf(price));
//        order.setCreateTime(LocalDateTime.now());
//        order.setPaymentType(seckillOrderFormDTO.getPaymentType());
//        order.setUserId(UserContext.getUser());
//        order.setStatus(1);
//
//        // 创建订单详情
//        OrderDetail orderDetail = new OrderDetail();
//        orderDetail.setId(identifierGenerator.nextId(new OrderDetail()).longValue());
//        orderDetail.setItemId(seckillOrderFormDTO.getItemId());
//        orderDetail.setOrderId(orderId);
//        orderDetail.setName(name);
//        orderDetail.setImage(image);
//        orderDetail.setSpec(spec);
//        orderDetail.setNum(1);
//        orderDetail.setPrice(Integer.valueOf(price));
//
//
//        // 将order和orderDetail发送mq异步写入数据库
//        SeckillOrderMessage message = new SeckillOrderMessage(order, orderDetail);
//        rabbitTemplate.convertAndSend(
//                MqConstants.TRADE_EXCHANGE_NAME,
//                MqConstants.ROUTING_KEY_SECKILL_ORDER_CREATE,
//                message
//        );

        // 将order和orderDetail发送mq异步写入数据库
        SeckillOrderMessage message = new SeckillOrderMessage(orderId, UserContext.getUser(), itemId, seckillItemId);
        rabbitTemplate.convertAndSend(
                TradeMqConstants.TRADE_EXCHANGE_NAME,
                TradeMqConstants.ROUTING_KEY_SECKILL_ORDER_CREATE,
                message
        );

        // 返回用户订单id
        return R.ok(orderId);
    }

    @Override
    public PageDTO<OrderVO> queryAllOrders(Integer pageNum, Integer pageSize) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, UserContext.getUser())
                .orderByDesc(Order::getCreateTime);
        Page<Order> result = this.page(page, queryWrapper);
        PageDTO<OrderVO> pageDTO = PageDTO.of(result, OrderVO.class);
        // 遍历订单详情
        for (OrderVO orderVO : pageDTO.getList()) {
            // 设置自动取消订单时间,创建时间加30分钟
            if (orderVO.getOrderType() == 1){
                orderVO.setAutoCloseTime(orderVO.getCreateTime().plusMinutes(30));
            }else if (orderVO.getOrderType() == 2){
                orderVO.setAutoCloseTime(orderVO.getCreateTime().plusMinutes(5));
            }
        }
        return pageDTO;
    }

    @Override
    public R<List<OrderDetail>> selectOrderDetailsById(Long orderId) {
        List<OrderDetail> orderDetails = orderDetailService.lambdaQuery()
                .eq(OrderDetail::getOrderId, orderId)
                .list();
        return R.ok(orderDetails);
    }

    /**
     * 恢复秒杀订单库存
     * @param order
     */
    @Override
    @GlobalTransactional
    public void cancelSeckillOrder(Order order) {
        Long userId = order.getUserId();
        Long orderId = order.getId();
        // 秒杀订单只有一个商品
        // 去商品详情表查询商品信息
        OrderDetail orderDetail = detailService.lambdaQuery().eq(OrderDetail::getOrderId, orderId).one();
        // 获取商品id
        Long itemId = orderDetail.getItemId();

        // 将数据库中秒杀活动商品库中的库存+1
        R r = itemClient.restoreSeckillStock(itemId);
        if (r.getCode() == 0) {
            throw new RuntimeException("库存恢复失败");
        }
        // 更新订单为取消
        lambdaUpdate()
                .set(Order::getStatus, 5)
                .set(Order::getCloseTime, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)
                .update();
        // 将支付单修改为超时取消状态
        payClient.markPayOrderTimeoutOrCancel(orderId);
        // redis中增加库存+1,hash中的stock字段+1
//        stringRedisTemplate.opsForHash().increment("seckill:item:" + itemId, "stock", 1);

        // 删除redis中set集合中记录的用户id
//        stringRedisTemplate.opsForSet().remove("seckill:order:" + itemId, userId);
        try {
            stringRedisTemplate.execute(ROLLBACK_SECKILL_SCRIPT,
                    Arrays.asList(SeckillRedisPrefix.SECKILL_ITEM + itemId, SeckillRedisPrefix.SECKILL_ORDER + itemId),
                    String.valueOf(userId));
        } catch (Exception e) {
            throw new RuntimeException("恢复库存失败");
        }

    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
