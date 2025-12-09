package com.hmall.item.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author ：蒋青江
 * @date ：2025/10/10 14:10
 * @description ：
 */
@Data
@ApiModel(description = "秒杀商品实体")
public class SeckillItemDTO {
    /**
     * 关联的商品的id
     */
    @ApiModelProperty("商品id")
    private Long itemId;

    /**
     * 库存
     */
    @ApiModelProperty("秒杀商品库存")
    private Integer stock;
    /**
     * 秒杀价
     */
    @ApiModelProperty("秒杀商品价格")
    private Integer seckillPrice;

    /**
     * 创建时间
     */
    @ApiModelProperty("秒杀商品创建时间")
    private LocalDateTime createTime;

    /**
     * 生效时间
     */
    @ApiModelProperty("秒杀商品生效时间")
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @ApiModelProperty("秒杀商品失效时间")
    private LocalDateTime endTime;

    /**
     * 更新时间
     */
    @ApiModelProperty("秒杀商品更新时间")
    private LocalDateTime updateTime;
}
