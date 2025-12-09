package com.hmall.item.constants;

/**
 * @author ：蒋青江
 * @date ：2025/12/2 16:45
/**
 * @description 秒杀状态: 1-未开始 2-进行中 3-已结束 4-已取消
 */
public enum SeckillStatus {

    NOT_STARTED(1, "未开始"),
    RUNNING(2, "进行中"),
    FINISHED(3, "已结束"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String desc;

    SeckillStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 获取对应的枚举
     */
    public static SeckillStatus of(int code) {
        for (SeckillStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的秒杀状态码: " + code);
    }
}