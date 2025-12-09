package com.hmall.trade.domain.dto;

import com.hmall.api.dto.OrderDetailDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "秒杀交易下单表单实体")
public class SeckillOrderFormDTO {
    @ApiModelProperty("收货地址id")
    private Long addressId;
    @ApiModelProperty("支付类型")
    private Integer paymentType;
    @ApiModelProperty("商品id")
    private Long itemId;
    @ApiModelProperty("秒杀活动商品id")
    private Long seckillItemId;
}
