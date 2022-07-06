package com.netease.cloud.lowcode.naslstorage.config;

import com.netease.cloud.lowcode.naslstorage.interceptor.AppIdInterceptor;
import com.netease.cloud.lowcode.naslstorage.interceptor.NaslChangeRecordInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public NaslChangeRecordInterceptor naslChangeRecordInterceptor() {
        return new NaslChangeRecordInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AppIdInterceptor()).addPathPatterns("/**")
                .excludePathPatterns("/health");

        registry.addInterceptor(naslChangeRecordInterceptor()).addPathPatterns("/**")
                .excludePathPatterns("/health");
    }
}
