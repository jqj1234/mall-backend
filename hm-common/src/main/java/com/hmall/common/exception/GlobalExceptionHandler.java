package com.hmall.common.exception;

import com.hmall.common.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author ：蒋青江
 * @date ：2025/11/28 11:32
 * @description ：全局异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 捕获所有 CommonException 及其子类（包括 BizIllegalException）
     */
    @ExceptionHandler(CommonException.class)
    public R handleCommonException(CommonException e) {
        log.warn("业务异常：{}", e.getMessage());
        return R.error(e.getMessage()); // 返回给前端的 msg 就是 throw 时传入的内容
    }

    /**
     * （可选）兜底：捕获其他未处理的异常
     */
    @ExceptionHandler(Exception.class)
    public R handleUnexpectedException(Exception e) {
        log.error("系统内部错误", e);
        return R.error("系统繁忙，请稍后重试");
    }
}
