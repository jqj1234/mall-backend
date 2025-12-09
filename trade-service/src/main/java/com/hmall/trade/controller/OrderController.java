package com.hmall.trade.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.R;
import com.hmall.common.utils.BeanUtils;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.dto.SeckillOrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.domain.vo.OrderVO;
import com.hmall.trade.service.IOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "订单管理接口")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final IOrderService orderService;

    @ApiOperation("根据id查询订单")
    @GetMapping("/{id}")
    public R<OrderVO> queryOrderById(@Param("订单id") @PathVariable("id") Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            return R.ok(null);
        }
        return R.ok(BeanUtils.copyBean(order, OrderVO.class));
    }

    @ApiOperation("根据订单id查询详细信息")
    @GetMapping("/details/{orderId}")
    public R<List<OrderDetail>> queryOrderDetailsById(@PathVariable("orderId") Long orderId) {
        return orderService.selectOrderDetailsById(orderId);
    }

    @ApiOperation("创建订单")
    @PostMapping
    public R<Long> createOrder(@RequestBody OrderFormDTO orderFormDTO) {
        return R.ok(orderService.createOrder(orderFormDTO));
    }

    @ApiOperation("秒杀创建订单")
    @PostMapping("/seckill")
    public R<Long> createSeckillOrder(@RequestBody SeckillOrderFormDTO seckillOrderFormDTO) {
        return orderService.createSeckillOrder(seckillOrderFormDTO);
    }

    @ApiOperation("分页查询所有订单")
    @GetMapping()
    public R<PageDTO<OrderVO>> queryAllOrders(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                           @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        PageDTO<OrderVO> result = orderService.queryAllOrders(pageNum, pageSize);
        return R.ok(result);
    }

    @ApiOperation("标记订单已支付")
    @ApiImplicitParam(name = "orderId", value = "订单id", paramType = "path")
    @PutMapping("/{orderId}")
    public R markOrderPaySuccess(@PathVariable("orderId") Long orderId) {
        orderService.markOrderPaySuccess(orderId);
        return R.ok();
    }

}
