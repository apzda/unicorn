package com.apzda.cloud.uc.config;

import cn.hutool.core.io.resource.ResourceUtil;
import jakarta.servlet.Filter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "com.alibaba.druid.pool.DruidDataSource")
@ConditionalOnProperty(name = "spring.datasource.druid.stat-view-servlet.enabled", havingValue = "true")
public class DruidConfig {

    @Bean
    FilterRegistrationBean<Filter> removeDruidAdFilter(
            @Value("${spring.datasource.druid.stat-view-servlet.url-pattern:/druid/*}") String pattern) {
        // 获取common.js内容
        final String text = ResourceUtil.readUtf8Str("support/http/resources/js/common.js");
        // 屏蔽 this.buildFooter(); 直接替换为空字符串,让js没机会调用
        final String newJs = text.replace("this.buildFooter()", "");

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.addUrlPatterns(StringUtils.stripEnd(pattern, "*") + "js/common.js");

        registration.setFilter((servletRequest, servletResponse, filterChain) -> {
            filterChain.doFilter(servletRequest, servletResponse);
            // 重置缓冲区，响应头不会被重置
            servletResponse.resetBuffer();
            // 把改造过的common.js文件内容写入到浏览器
            servletResponse.getWriter().write(newJs);
        });

        return registration;
    }

}
