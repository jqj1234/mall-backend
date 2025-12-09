package com.hmall.api.fallback;


import com.hmall.api.client.EmbeddingClient;
import com.hmall.api.dto.ImageEmbeddingDTO;
import com.hmall.api.dto.NameEmbeddingDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Map;

/**
 * @author ：蒋青江
 * @date ：2025/8/22 14:46
 * @description ：向量嵌入fallback
 */
@Slf4j
public class EmbeddingClientFallback implements FallbackFactory<EmbeddingClient> {
    @Override
    public EmbeddingClient create(Throwable cause) {
        return new EmbeddingClient() {
            @Override
            public NameEmbeddingDTO getNameEmbedding(Map map) {
                return new NameEmbeddingDTO();
            }

            @Override
            public ImageEmbeddingDTO getImageEmbedding(Map map) {
                return new ImageEmbeddingDTO();
            }
        };
    }
}
