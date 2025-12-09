package com.hmall.item.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.domain.R;
import com.hmall.common.utils.AliOssUtil;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Api(tags = "商品管理相关接口")
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final IItemService itemService;

    private final AliOssUtil aliOssUtil;

    @ApiOperation("分页查询商品")
    @GetMapping("/page")
    public R queryItemByPage(PageQuery query) {
        // 1.分页查询
        Page<Item> result = itemService.page(query.toMpPage("update_time", false));
        // 2.封装并返回
        return R.ok(PageDTO.of(result, ItemDTO.class));
    }

    @ApiOperation("根据id批量查询商品")
    @GetMapping
    public R queryItemByIds(@RequestParam("ids") List<Long> ids) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return R.ok(itemService.queryItemByIds(ids));
    }

    @ApiOperation("根据id查询商品")
    @GetMapping("/{id}")
    public R queryItemById(@PathVariable("id") Long id) throws InterruptedException {
//        Thread.sleep(500);
        return R.ok(BeanUtils.copyBean(itemService.getById(id), ItemDTO.class));
    }

    @ApiOperation("新增商品")
    @PostMapping
    public R saveItem(@RequestBody ItemDTO item) {
        // 新增
        itemService.save(BeanUtils.copyBean(item, Item.class));
        return R.ok();
    }

    @ApiOperation("更新商品状态")
    @PutMapping("/status/{id}/{status}")
    public R updateItemStatus(@PathVariable("id") Long id, @PathVariable("status") Integer status) {
        itemService.updateItemStatus(id, status);
        return R.ok();
    }

    @ApiOperation("更新商品")
    @PutMapping
    public R updateItem(@RequestBody ItemDTO item) {
        // 不允许修改商品状态，所以强制设置为null，更新时，就会忽略该字段
        item.setStatus(null);
        // 更新
        itemService.updateById(BeanUtils.copyBean(item, Item.class));
        return R.ok("更新成功");
    }

    @ApiOperation("根据id删除商品")
    @DeleteMapping("{id}")
    public R deleteItemById(@PathVariable("id") Long id) {
        itemService.removeById(id);
        return R.ok("删除成功");
    }

    @ApiOperation("批量扣减库存")
    @PutMapping("/stock/deduct")
    public R deductStock(@RequestBody List<OrderDetailDTO> items) {
        itemService.deductStock(items);
        return R.ok("扣减库存成功");
    }

    @ApiOperation("图片上传")
    @PostMapping("/upload")
    public R uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            log.info("原始文件名：{}", originalFilename);

            //截取文件拓展名
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //构建新文件名
            String objectName = UUID.randomUUID().toString() + extension;

            //文件的请求路径
            String url = aliOssUtil.upload(file.getBytes(), objectName);
            return R.ok(url);

        } catch (IOException e) {
            log.error("文件上传失败：{ }", e);
            return null;
        }

    }
}
