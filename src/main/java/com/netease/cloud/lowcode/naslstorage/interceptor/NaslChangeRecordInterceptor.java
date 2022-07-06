package com.netease.cloud.lowcode.naslstorage.interceptor;

import com.netease.cloud.lowcode.naslstorage.backend.BackendStore;
import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.enums.ChangedNaslType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author pingerchen
 * 记录前端Nasl 更改或后端Nasl 更改的时间，用于发布时判断是否需要重新生成代码和cicd
 */
public class NaslChangeRecordInterceptor implements HandlerInterceptor {
    private static final String CHANGED_NASLTYPE = "ChangedNASLType";

    @Resource
    private BackendStore backendStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        /**
         * 理论上Nasl 操作成功后在更新Nasl 更改时间的，但切面和具体Nasl 操作的逻辑不好整合在一个事务里
         * 更新前先设置Nasl 更新时间影响较小，挺多就是Nasl 没变更成功，但是还是重新生成代码，没功能问题，体验不好
         */
        String changedNaslType = request.getHeader(CHANGED_NASLTYPE);
        String appId = request.getHeader(Consts.HEADER_APPID);
        if (StringUtils.hasLength(appId)) {
            ChangedNaslType changedNaslTypeEnum = ChangedNaslType.from(changedNaslType);
            backendStore.recordAppNaslChanged(appId, changedNaslTypeEnum);
        }

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
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
