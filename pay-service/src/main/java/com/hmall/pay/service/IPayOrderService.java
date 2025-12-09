package com.hmall.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.R;
import com.hmall.pay.domain.dto.PayApplyDTO;
import com.hmall.pay.domain.dto.PayOrderFormDTO;
import com.hmall.pay.domain.po.PayOrder;

/**
 * <p>
 * 支付订单 服务类
 * </p>
 */
public interface IPayOrderService extends IService<PayOrder> {

    String applyPayOrder(PayApplyDTO applyDTO);

    void tryPayOrderByBalance(PayOrderFormDTO payOrderFormDTO);


    /**
     * 根据订单id标记支付单为超时或取消
     * @param orderId
     */
    void markPayOrderTimeoutOrCancel(Long orderId);
}
