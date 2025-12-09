package com.hmall.item.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author ：蒋青江
 * @date ：2025/12/1 20:02
 * @description ：
 */
@Data
public class SeckillItemVO {
    // 秒杀活动id
    private Long seckillItemId;
    // 商品id
    private Long itemId;
    private String brand;
    private String name;
    private String image;
    private Integer seckillPrice;
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
}