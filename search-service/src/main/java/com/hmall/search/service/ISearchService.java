package com.hmall.search.service;

import com.hmall.search.domain.po.ItemDOC;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.PageVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @author ：蒋青江
 * @date ：2025/8/1 15:18
 * @description ：
 */
public interface ISearchService {

    // 保存商品
    void saveItemById(Long id);

    // 删除商品
    void deleteItemById(Long id);

    PageVO<ItemDOC> search(ItemPageQuery query);

    //搜索商品分类、品牌列表
    Map<String, List<String>> filters(ItemPageQuery query);

    PageVO<ItemDOC> imageSearch(Map<String,String> image);
}
