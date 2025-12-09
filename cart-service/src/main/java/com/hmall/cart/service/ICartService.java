package com.hmall.cart.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.cart.domain.dto.CartFormDTO;
import com.hmall.cart.domain.po.Cart;
import com.hmall.cart.domain.vo.CartVO;
import com.hmall.common.domain.R;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 订单详情表 服务类
 * </p>
 */
public interface ICartService extends IService<Cart> {

    R addItem2Cart(CartFormDTO cartFormDTO);

   List<CartVO> queryMyCarts();

    void removeByItemIds(Collection<Long> itemIds);
}
