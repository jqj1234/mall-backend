package com.hmall.cart.controller;


import com.hmall.api.client.ItemClient;
import com.hmall.cart.domain.dto.CartFormDTO;
import com.hmall.cart.domain.po.Cart;
import com.hmall.cart.domain.vo.CartVO;
import com.hmall.cart.service.ICartService;
import com.hmall.common.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "购物车相关接口")
@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
public class CartController {
    private final ICartService cartService;

    @ApiOperation("添加商品到购物车")
    @PostMapping
    public R addItem2Cart(@Valid @RequestBody CartFormDTO cartFormDTO) {
        return cartService.addItem2Cart(cartFormDTO);
    }

    @ApiOperation("更新购物车数据")
    @PutMapping
    public R updateCart(@RequestBody Cart cart) {
        cartService.updateById(cart);
        return R.ok("更新成功");
    }

    @ApiOperation("删除购物车中商品")
    @DeleteMapping("{id}")
    public R deleteCartItem(@Param("购物车条目id") @PathVariable("id") Long id) {
        cartService.removeById(id);
        return R.ok("删除成功");
    }

    @ApiOperation("查询购物车列表")
    @GetMapping
    public R<List<CartVO>> queryMyCarts(@RequestHeader(value = "user-info", required = false) Long userId) {
//        System.out.println("用户id：" + userId);
        return R.ok(cartService.queryMyCarts());
    }

    @ApiOperation("批量删除购物车中商品")
    @ApiImplicitParam(name = "ids", value = "购物车条目id集合")
    @DeleteMapping
    public R deleteCartItemByIds(@RequestParam("ids") List<Long> ids) {
        cartService.removeByItemIds(ids);
        return R.ok("批量删除成功");
    }

//    @ApiOperation("测试熔断降级")
//    @GetMapping("/test")
//    public String test() {
//        return itemClient.queryItemById(317578L).toString();
//    }
}
