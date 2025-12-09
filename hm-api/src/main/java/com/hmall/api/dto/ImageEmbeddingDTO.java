package com.hmall.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 名称向量响应DTO
 * 对应Flask接口返回的JSON结构
 */
@Data
public class ImageEmbeddingDTO {
    // 字段名必须与Flask返回的JSON键名一致（nameVector）
    private List<Float> imageVector;
}