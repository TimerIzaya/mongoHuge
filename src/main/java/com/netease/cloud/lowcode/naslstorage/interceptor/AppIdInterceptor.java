package com.netease.cloud.lowcode.naslstorage.interceptor;

import com.netease.cloud.lowcode.naslstorage.context.AppIdContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class AppIdInterceptor implements HandlerInterceptor {

    private static final String APP_ID = "appId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String appId = request.getHeader(APP_ID);
        if (!StringUtils.hasLength(appId)) {
            log.error("appId in header is required");
            throw new Exception("appId in header is required");
        }
        AppIdContext.set(appId);
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        AppIdContext.clear();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
