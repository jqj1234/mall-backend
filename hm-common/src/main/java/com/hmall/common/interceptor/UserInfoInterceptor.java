package com.hmall.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ：蒋青江
 * @date ：2025/7/28 11:10
 * @description ：拦截器
 */
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头user-info
        String userId = request.getHeader("user-info");
        // 如果有值的话设置到线程副本ThreadLocal中
        if(StrUtil.isNotBlank(userId)){
            UserContext.setUser(Long.parseLong(userId));
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 清理用户
        UserContext.removeUser();
    }
}
