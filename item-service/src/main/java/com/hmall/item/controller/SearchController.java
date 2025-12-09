package com.hmall.item.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.domain.PageDTO;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.query.ItemPageQuery;
import com.hmall.item.service.IItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final IItemService itemService;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) {
        // TODO 根据条件分页查询，默认根据更新时间降序排序
        LambdaQueryChainWrapper<Item> queryChainWrapper = itemService.lambdaQuery()
                .like(StrUtil.isNotBlank(query.getKey()), Item::getName, query.getKey())
                .eq(StrUtil.isNotBlank(query.getCategory()), Item::getCategory, query.getCategory())
                .eq(StrUtil.isNotBlank(query.getBrand()), Item::getBrand, query.getBrand())
                .ge(query.getMinPrice() != null, Item::getPrice, query.getMinPrice())
                .le(query.getMaxPrice() != null, Item::getPrice, query.getMaxPrice());
        if (query.getSortBy() != null) {
            switch (query.getSortBy()) {
                case "price":
                    queryChainWrapper.orderBy(true, query.getIsAsc(), Item::getPrice);
                    break;
                case "sold":
                    queryChainWrapper.orderBy(true, query.getIsAsc(), Item::getSold);
                    break;
                default:
                    queryChainWrapper.orderBy(true, query.getIsAsc(), Item::getUpdateTime);
                    break;
            }
        } else {
            queryChainWrapper.orderBy(true, query.getIsAsc(), Item::getUpdateTime);
        }

        Page<Item> page = queryChainWrapper.page(new Page<>(query.getPageNo(), query.getPageSize()));

        return PageDTO.of(page, ItemDTO.class);
    }
}
