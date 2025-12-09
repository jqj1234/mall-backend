package com.hmall.trade.domain.dto;

import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage {
    /** 订单Id */
    private Long orderId;
    /** 用户Id */
    private Long userId;
    /** 商品Id */
    private Long itemId;
    /** 秒杀订单Id */
    private Long seckillItemId;
}