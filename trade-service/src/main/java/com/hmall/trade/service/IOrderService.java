package com.hmall.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.domain.R;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.dto.SeckillOrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.domain.vo.OrderVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IOrderService extends IService<Order> {

    Long createOrder(OrderFormDTO orderFormDTO);

    void markOrderPaySuccess(Long orderId);

    void cancelOrder(Long orderId);

    R<Long> createSeckillOrder(SeckillOrderFormDTO seckillOrderFormDTO);

    PageDTO<OrderVO> queryAllOrders(Integer pageNum, Integer pageSize);

    R<List<OrderDetail>> selectOrderDetailsById(Long orderId);

    void cancelSeckillOrder(Order order);
}
