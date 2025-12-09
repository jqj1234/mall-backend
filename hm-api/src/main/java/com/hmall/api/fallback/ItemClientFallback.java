package com.hmall.api.fallback;

import cn.hutool.core.collection.CollUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.domain.R;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

/**
 * @author ：蒋青江
 * @date ：2025/7/29 9:51
 * @description ：
 */
@Slf4j
public class ItemClientFallback implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public R<List<ItemDTO>> queryItemByIds(Collection<Long> ids) {
                log.error("调用商品id集合查询商品列表失败：具体参数为：{}", ids,cause);
                return R.error("调用商品id集合查询商品列表失败");
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                // 与下单的接口有关联，如果出错了，则需要回滚，需要抛出异常
                log.error("调用扣减库存失败：具体参数为：{}", items,cause);
                throw new BizIllegalException("调用扣减库存失败！");
            }

            @Override
            public R<ItemDTO> queryItemById(Long id) {
                log.error("调用商品id查询商品失败：具体参数为：{}", id,cause);
                return R.error("调用商品id查询商品失败");
            }

            @Override
            public R deductStock(Long id) {
                return R.error("调用扣减库存失败");
            }

            @Override
            public R querySeckillItemById(Long id) {
                return R.error("调用查询秒杀商品失败");
            }

            @Override
            public R restoreSeckillStock(Long itemId) {
                return R.error("调用恢复秒杀商品库存失败");
            }
        };
    }
}
