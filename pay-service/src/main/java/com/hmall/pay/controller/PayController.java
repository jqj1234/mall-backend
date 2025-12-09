package com.hmall.pay.controller;

import com.hmall.common.domain.R;
import com.hmall.common.utils.BeanUtils;
import com.hmall.pay.domain.dto.PayApplyDTO;
import com.hmall.pay.domain.dto.PayOrderFormDTO;
import com.hmall.pay.domain.po.PayOrder;
import com.hmall.pay.domain.vo.PayOrderVO;
import com.hmall.pay.enums.PayType;
import com.hmall.pay.service.IPayOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "支付相关接口")
@RestController
@RequestMapping("/pay-orders")
@RequiredArgsConstructor
public class PayController {

    private final IPayOrderService payOrderService;

    @ApiOperation("根据订单id查询支付单")
    @GetMapping("/biz/{id}")
    public R<PayOrderVO> queryPayOrderDTOByBizOrderNo(@PathVariable Long id) {
        PayOrder payOrder = payOrderService.lambdaQuery().eq(PayOrder::getBizOrderNo, id).one();
        return R.ok(BeanUtils.copyBean(payOrder, PayOrderVO.class));
    }

    @ApiOperation("查询支付单")
    @GetMapping
    public List<PayOrderVO> queryPayOrders() {
        return BeanUtils.copyList(payOrderService.list(), PayOrderVO.class);
    }

    @ApiOperation("生成支付单")
    @PostMapping
    public R<String> applyPayOrder(@RequestBody PayApplyDTO applyDTO) {
        if (!PayType.BALANCE.equalsValue(applyDTO.getPayType())) {
            // 目前只支持余额支付
//            throw new BizIllegalException("抱歉，目前只支持余额支付");
            return R.error("抱歉，目前只支持余额支付");
        }
        return R.ok(payOrderService.applyPayOrder(applyDTO));
    }

    @ApiOperation("尝试基于用户余额支付")
    @ApiImplicitParam(value = "支付单id", name = "id")
    @PostMapping("/{id}")
    public R tryPayOrderByBalance(@PathVariable("id") Long id, @RequestBody PayOrderFormDTO payOrderFormDTO) {
        payOrderFormDTO.setId(id);
        payOrderService.tryPayOrderByBalance(payOrderFormDTO);
        return R.ok();
    }

    /**
     * 根据订单id标记支付单为超时或取消
     *
     * @param orderId
     */
    @ApiOperation("根据订单id标记支付单为超时或取消")
    @PutMapping("/{id}")
    public void markPayOrderTimeoutOrCancel(@PathVariable("id") Long orderId) {
        payOrderService.markPayOrderTimeoutOrCancel(orderId);
    }
}
