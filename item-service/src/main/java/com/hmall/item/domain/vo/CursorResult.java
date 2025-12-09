package com.hmall.item.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ：蒋青江
 * @date ：2025/12/1 20:10
 * @description ：
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorResult<T> {
    private List<T> data; // 当前页数据
    private Long nextLastBeginTime; // 下一页游标：开始时间戳
    private Long nextLastItemId; // 下一页游标：商品ID
}
