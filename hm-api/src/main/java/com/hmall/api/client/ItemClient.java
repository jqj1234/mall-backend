package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.fallback.ItemClientFallback;
import com.hmall.common.domain.R;
import com.hmall.common.utils.BeanUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

/**
 * @author ：蒋青江
 * @date ：2025/7/27 10:48
 * @description ：FeignClient
 */
// 标注是一个Feign客户端，指定微服务名称，这样的话就会从注册中心获取对应的服务信息
// 并基于负载均衡访问服务实例
@FeignClient(value = "item-service",fallbackFactory = ItemClientFallback.class, configuration = DefaultFeignConfig.class)
public interface ItemClient {
    @GetMapping("/items")
    public R<List<ItemDTO>> queryItemByIds(@RequestParam("ids") Collection<Long> ids);

    @PutMapping("/items/stock/deduct")
    public void deductStock(@RequestBody List<OrderDetailDTO> items);

    /**
     * 根据id查询商品
     * @param id 商品id
     */
    @GetMapping("/items/{id}")
    public R<ItemDTO> queryItemById(@PathVariable("id") Long id);

    /**
     * 扣减秒杀商品库存
     * @param
     */
    @PostMapping("/seckill/deductStock/{id}")
    public R deductStock(@PathVariable Long id);

    /**
     * 查询秒杀商品价格
     * @param id
     * @return
     */
    @GetMapping("/seckill/query/{id}")
    public R<Integer> querySeckillItemById(@PathVariable Long id);

    /**
     * 恢复秒杀商品库存
     * @param id 商品id
     * @return
     */
    @PostMapping("/seckill/restoreStock/{id}")
    public R restoreSeckillStock(@PathVariable Long id);

}
