package com.hmall.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.R;
import com.hmall.item.domain.dto.SeckillItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.SeckillItem;

/**
 * @author ：蒋青江
 * @date ：2025/10/10 14:17
 * @description ：
 */
public interface ISeckillItemService extends IService<SeckillItem> {
    /**
     * 添加秒杀商品
     *
     * @param seckillItemDTO 秒杀商品信息
     * @return 是否添加成功
     */
    R addSeckillItem(SeckillItemDTO seckillItemDTO);

    R getActiveSeckillItemsWithCursor(Long lastBeginTime, Long lastItemId, Integer limit);

    R getSeckillItemById(Long id);

    R deductStock(Long id);

    R<Integer> querySeckillItemPriceById(Long id);

    R restoreSeckillStock(Long id);
}
