package com.netease.cloud.lowcode.naslstorage.config;

import com.netease.cloud.lowcode.naslstorage.interceptor.AppIdInterceptor;
import com.netease.cloud.lowcode.naslstorage.interceptor.NaslChangeRecordInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AppIdInterceptor()).addPathPatterns("/**")
                .excludePathPatterns( "/health");

        registry.addInterceptor(new NaslChangeRecordInterceptor()).addPathPatterns("/**")
                .excludePathPatterns( "/health");
    }
}
