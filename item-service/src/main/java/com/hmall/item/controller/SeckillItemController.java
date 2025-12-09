package com.hmall.item.controller;

import com.hmall.common.domain.R;
import com.hmall.item.domain.dto.SeckillItemDTO;
import com.hmall.item.domain.po.SeckillItem;
import com.hmall.item.service.ISeckillItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author ：蒋青江
 * @date ：2025/10/10 14:13
 * @description ：秒杀商品控制器
 */
@Api(tags = "秒杀商品管理相关接口")
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillItemController {
    private final ISeckillItemService seckillItemService;
    @ApiOperation("添加秒杀商品")
    @PostMapping()
    public R addSeckillItem(@RequestBody SeckillItemDTO seckillItemDTO) {
        return seckillItemService.addSeckillItem(seckillItemDTO);
    }

    @ApiOperation("游标分页查询正在秒杀的商品")
    @GetMapping("/cursor")
    public R listSeckillItemByCursor(
            @RequestParam(required = false) Long lastBeginTime,
            @RequestParam(required = false) Long lastItemId,
            @RequestParam(defaultValue = "10") Integer limit) {

        if (limit == null || limit < 1 || limit > 50) {
            return R.error("limit 必须在 1~50 之间");
        }

        return seckillItemService.getActiveSeckillItemsWithCursor(lastBeginTime, lastItemId, limit);
    }

    @ApiOperation("根据商品id查询秒杀商品")
    @GetMapping("/{id}")
    public R getSeckillItemById(@PathVariable Long id) {
        return seckillItemService.getSeckillItemById(id);
    }

    @ApiOperation("根据秒杀商品id扣减库存")
    @PostMapping("/deductStock/{id}")
    public R deductStock(@PathVariable Long id) {
        return seckillItemService.deductStock(id);
    }

    /**
     * 恢复秒杀商品库存
     *
     * @param id 秒杀商品id
     * @return 恢复结果
     */
    @ApiOperation("恢复秒杀商品的库存")
    @PostMapping("/restoreStock/{id}")
    public R restoreSeckillStock(@PathVariable Long id) {
        return seckillItemService.restoreSeckillStock(id);
    }

    @ApiOperation("根据秒杀商品id查询秒杀商品价格")
    @GetMapping("/query/{id}")
    public R<Integer> querySeckillItemById(@PathVariable Long id) {
        return seckillItemService.querySeckillItemPriceById(id);
    }
}
