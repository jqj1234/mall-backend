package com.hmall.search.controller;

import com.hmall.common.domain.R;
import com.hmall.search.domain.po.ItemDOC;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.PageVO;
import com.hmall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author ：蒋青江
 * @date ：2025/8/1 15:16
 * @description ：搜索控制器
 */
@Api(tags = "搜索控制器")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
    private final ISearchService searchService;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public R<PageVO<ItemDOC>> search(ItemPageQuery query) {
        PageVO<ItemDOC> itemDOCPageVO = searchService.search(query);
        return R.ok(itemDOCPageVO);
    }

    @ApiOperation("搜索商品分类、品牌列表")
    @PostMapping("/filters")
    public R<Map<String, List<String>>> filters(@RequestBody ItemPageQuery query) {
        Map<String, List<String>> filters = searchService.filters(query);
        return R.ok(filters);
    }

    @ApiOperation("图像搜索商品")
    @PostMapping("/image")
    public R<PageVO<ItemDOC>> imageSearch(@RequestBody Map<String, String> image) {
        PageVO<ItemDOC> itemDOCPageVO = searchService.imageSearch(image);
        return R.ok(itemDOCPageVO);
    }
}
