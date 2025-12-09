package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.dto.ImageEmbeddingDTO;
import com.hmall.api.dto.NameEmbeddingDTO;
import com.hmall.api.fallback.CartClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * @author ：蒋青江
 * @date ：2025/8/22 14:45
 * @description ：向量嵌入接口
 */
@FeignClient(value = "embedding-service",fallbackFactory = CartClientFallback.class, configuration = DefaultFeignConfig.class)
public interface EmbeddingClient {
    /**
     * 获取商品名称向量
     * @param params 包含name字段的参数Map
     * @return 名称向量的浮点列表
     */
    @PostMapping("/name_embedding")
    NameEmbeddingDTO getNameEmbedding(@RequestBody Map<String, String> params);

    /**
     * 获取商品图片向量
     * @param params 包含image字段的参数Map
     * @return 图片向量的浮点列表
     */
    @PostMapping("/image_embedding")
    ImageEmbeddingDTO getImageEmbedding(@RequestBody Map<String, String> params);
}
