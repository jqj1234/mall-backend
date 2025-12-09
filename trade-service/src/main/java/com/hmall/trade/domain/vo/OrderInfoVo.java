package com.hmall.trade.domain.vo;

import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.domain.po.OrderLogistics;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * @author ：蒋青江
 * @date ：2025/10/15 21:19
 * @description ：用户订单信息VO
 */
@Data
@ApiModel(description = "订单信息VO")
public class OrderInfoVo {
    private Order order;
    private List<OrderDetail> orderDetails;
    private OrderLogistics orderLogistics;
    private String address;
}
