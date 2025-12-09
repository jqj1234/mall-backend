package com.hmall.common.domain;

import com.hmall.common.utils.CollUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MultiDelayMessage<T> {
    //消息内容
    private T data;
    //记录延迟时间的集合
    private List<Integer> delayMillis;

    public MultiDelayMessage(T data, List<Integer> delayMillis) {
        this.data = data;
        this.delayMillis = delayMillis;
    }

    public static <T> MultiDelayMessage<T> of(T data, Integer... delayMillis) {
        return new MultiDelayMessage<>(data, CollUtils.newArrayList(delayMillis));
    }

    /**
     * 获取并移除下一个延迟时间
     */
    public Integer removeNextDelay() {
        return this.delayMillis.remove(0);
    }
    /**
     * 判断是否还有下一个延迟时间
     */
    public boolean hasNextDelay() {
        return !delayMillis.isEmpty();
    }
}