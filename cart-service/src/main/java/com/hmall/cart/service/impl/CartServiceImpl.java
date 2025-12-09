package com.hmall.cart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.cart.config.CartProperties;
import com.hmall.cart.domain.dto.CartFormDTO;
import com.hmall.cart.domain.po.Cart;
import com.hmall.cart.domain.vo.CartVO;
import com.hmall.cart.mapper.CartMapper;
import com.hmall.cart.service.ICartService;
import com.hmall.common.domain.R;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(CartProperties.class)
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    // private final IItemService itemService;

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;

    private final ItemClient itemClient;
    private final CartProperties cartProperties;

    @Override
    public R addItem2Cart(CartFormDTO cartFormDTO) {
        //TODO 如果商品已经添加过则购买数量添加即可；如果未添加过则新增一条购物车记录。一个用户最多放置购物车商品为10
        if (checkItemExists(cartFormDTO.getItemId(), UserContext.getUser())) {
            // 添加过该商品，购买数量+1
            // update cart set num = num + 1 where item_id = ? and user_id = ?
            lambdaUpdate()
                    .setSql("num = num + 1")
                    .eq(Cart::getItemId, cartFormDTO.getItemId())
                    .eq(Cart::getUserId, UserContext.getUser())
                    .update();
        } else {
            // 未添加过该商品，如果当前这个用户的购物车商品没有超过10个的话；可以新增一条购物车记录
            Long count = lambdaQuery()
                    .eq(Cart::getUserId, UserContext.getUser()).count();
            if (count >= cartProperties.getMaxAmount()) {
//                throw new BizIllegalException(StrUtil.format("用户购物车商品数量不能超过{}", cartProperties.getMaxAmount()));
                return R.error("购物车商品数量不能超过"+cartProperties.getMaxAmount());
            }

            Cart cart = BeanUtil.copyProperties(cartFormDTO, Cart.class);
            cart.setUserId(UserContext.getUser());
            cart.setNum(1);
            cart.setCreateTime(LocalDateTime.now());
            cart.setUpdateTime(LocalDateTime.now());
            save(cart);

        }
        return R.ok("添加成功");

    }

    /**
     * 检查当前用户购物车中是否存在该商品
     *
     * @param itemId 商品id
     * @param userId 用户id
     * @return
     */
    private boolean checkItemExists(Long itemId, Long userId) {
        // select count(*) from cart where user_id = ? and item_id = ?
        Long count = lambdaQuery().eq(Cart::getItemId, itemId)
                .eq(Cart::getUserId, userId)
                .count();
        return count > 0;
    }

    @Override
    public List<CartVO> queryMyCarts() {
        //TODO 查询当前登录用户的购物车列表；需要将Cart转换为CartVO；且CartVO中需要包含商品的最新价格、状态、库存等信息。
        List<Cart> cartList = lambdaQuery().eq(Cart::getUserId, UserContext.getUser()).list();
//        List<Cart> cartList = lambdaQuery().eq(Cart::getUserId, 1L).list();

        if (!CollUtils.isEmpty(cartList)) {
            List<CartVO> cartVOS = BeanUtil.copyToList(cartList, CartVO.class);
            //2、设置商品的最新价格、状态、库存等信息
            //2.1、收集商品id集合
            List<Long> itemIdList = cartVOS.stream().map(CartVO::getItemId).collect(Collectors.toList());

            //根据商品id集合批量查询商品
//            List<ItemDTO> items = itemService.listByIds(itemIdList);

//            List<ItemDTO> items = null;
//
//            // 获取注册中心item-service
//            List<ServiceInstance> instanceList = discoveryClient.getInstances("item-service");
//            // 从服务列表中获取item-service
//            ServiceInstance serviceInstance = instanceList.get(RandomUtil.randomInt(instanceList.size()));
//            // 获取item-service的url
//            String url = serviceInstance.getUri() + "/items?ids={ids}";
//            ResponseEntity<List<ItemDTO>> response = restTemplate.exchange(url,
//                    HttpMethod.GET,
//                    null,
//                    new ParameterizedTypeReference<List<ItemDTO>>() {
//                    },
//                    Map.of("ids", CollUtils.join(itemIdList, ",")));
//
//            if (response.getStatusCode().is2xxSuccessful()) {
//                items = response.getBody();
//            }
//
//            if (CollUtils.isEmpty(items)) {
//                return CollUtils.emptyList();
//            }


            // 使用feign
            R<List<ItemDTO>> r = itemClient.queryItemByIds(itemIdList);
            List<ItemDTO> items = r.getData();
            if (CollUtils.isEmpty(items)) {
                return CollUtils.emptyList();
            }
            // map<商品id,商品>
            Map<Long, ItemDTO> itemMap = items.stream().collect(Collectors.toMap(ItemDTO::getId, Function.identity()));

            //遍历每个购物车商品，设置商品属性
            cartVOS.forEach(cartVO -> {
                ItemDTO item = itemMap.get(cartVO.getItemId());
                cartVO.setNewPrice(item.getPrice());
                cartVO.setStatus(item.getStatus());
                cartVO.setStock(item.getStock());
            });

            return cartVOS;
        }
        return CollUtils.emptyList();
    }

    @Override
    public void removeByItemIds(Collection<Long> itemIds) {
        // 1.构建删除条件，userId和itemId
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<Cart>();
        queryWrapper.lambda()
                .eq(Cart::getUserId, UserContext.getUser())
                .in(Cart::getItemId, itemIds);
        // 2.删除
        remove(queryWrapper);
    }
}
