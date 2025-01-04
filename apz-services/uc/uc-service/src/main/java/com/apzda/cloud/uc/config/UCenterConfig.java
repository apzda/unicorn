/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.uc.config;

import com.apzda.cloud.captcha.helper.CaptchaHelper;
import com.apzda.cloud.config.service.SettingService;
import com.apzda.cloud.gsvc.config.EnableGsvcServices;
import com.apzda.cloud.gsvc.infra.TempStorage;
import com.apzda.cloud.gsvc.io.YamlPropertySourceFactory;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.filter.AbstractProcessingFilter;
import com.apzda.cloud.gsvc.security.filter.SecurityFilterRegistrationBean;
import com.apzda.cloud.gsvc.security.handler.AuthenticationHandler;
import com.apzda.cloud.gsvc.security.token.JwtTokenCustomizer;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaService;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.mapper.JwtTokenMapper;
import com.apzda.cloud.uc.mfa.Authenticator;
import com.apzda.cloud.uc.proto.AccountService;
import com.apzda.cloud.uc.proto.PrivilegeService;
import com.apzda.cloud.uc.proto.RoleService;
import com.apzda.cloud.uc.security.UserDetailsServiceImpl;
import com.apzda.cloud.uc.security.authentication.DefaultAuthenticationProvider;
import com.apzda.cloud.uc.security.authentication.RefreshAuthenticationProvider;
import com.apzda.cloud.uc.security.authentication.SwitchBackAuthenticationProvider;
import com.apzda.cloud.uc.security.authentication.SwitchToAuthenticationProvider;
import com.apzda.cloud.uc.security.filter.RefreshTokenFilter;
import com.apzda.cloud.uc.security.filter.SwitchBackFilter;
import com.apzda.cloud.uc.security.filter.SwitchToFilter;
import com.apzda.cloud.uc.security.filter.UsernameAndPasswordFilter;
import com.apzda.cloud.uc.security.handler.UCenterAuthenticationHandler;
import com.apzda.cloud.uc.security.mfa.GoogleTotpAuthenticator;
import com.apzda.cloud.uc.security.token.TokenCustomizer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@PropertySources({ @PropertySource(name = "ucenter-default-cfg", value = "classpath:/ucenter-config.yml",
        factory = YamlPropertySourceFactory.class), @PropertySource("classpath:/apzda.uc.service.properties") })
@EnableGsvcServices({ AccountService.class, RoleService.class, PrivilegeService.class })
@EnableConfigurationProperties(UCenterConfigProperties.class)
@EnableJpaRepositories("com.apzda.cloud.uc.domain.repository")
@EntityScan("com.apzda.cloud.uc.domain.entity")
@Slf4j
public class UCenterConfig implements ApplicationContextAware {

    private final UCenterConfigProperties properties;

    private ApplicationContext applicationContext;

    public UCenterConfig(UCenterConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 多因素认证器.
     * @return 多因素认证器实现.
     */
    @Nullable
    public Authenticator getAuthenticator() {
        val authenticatorClz = properties.getAuthenticator();
        if (authenticatorClz == null) {
            return null;
        }
        try {
            applicationContext.getBean(authenticatorClz);
        }
        catch (Exception e) {
            return null;
        }
        return applicationContext.getBean(authenticatorClz);
    }

    @Configuration(proxyBeanMethods = false)
    static class SecurityConfigure {

        @Bean
        UserDetailsService userDetailsService(UserManager userManager) {
            // 自定义用户明细服务实现
            return new UserDetailsServiceImpl(userManager);
        }

        @Bean
        AuthenticationHandler authenticationHandler(SecurityConfigProperties properties, TokenManager tokenManager,
                ObjectProvider<JwtTokenCustomizer> customizers) {
            return new UCenterAuthenticationHandler(properties, tokenManager, customizers);
        }

        @Bean("defaultAuthenticationProvider")
        AuthenticationProvider defaultAuthenticationProvider(UserManager userManager,
                UserDetailsMetaRepository userDetailsMetaRepository, CaptchaHelper captchaHelper,
                SettingService settingService, TempStorage tempStorage) {
            // 自定义用户名/密码认证器
            return new DefaultAuthenticationProvider(userManager, userDetailsMetaRepository, captchaHelper,
                    settingService, tempStorage);
        }

        @Bean
        AuthenticationProvider switchToAuthenticationProvider(UserManager userManager,
                UserDetailsService userDetailsService, UserDetailsMetaRepository userDetailsMetaRepository) {
            return new SwitchToAuthenticationProvider(userManager, userDetailsService, userDetailsMetaRepository);
        }

        @Bean
        AuthenticationProvider switchBackAuthenticationProvider(UserManager userManager,
                UserDetailsService userDetailsService, UserDetailsMetaRepository userDetailsMetaRepository) {
            return new SwitchBackAuthenticationProvider(userManager, userDetailsService, userDetailsMetaRepository);
        }

        @Bean
        AuthenticationProvider refreshAuthenticationProvider(TokenManager tokenManager) {
            return new RefreshAuthenticationProvider(tokenManager);
        }

        @Bean
        @ConditionalOnProperty(prefix = "apzda.ucenter.server.endpoint", name = "username-password",
                matchIfMissing = true)
        SecurityFilterRegistrationBean<AbstractProcessingFilter> usernameAndPasswordFilter(
                UCenterConfigProperties ucenterConfigProperties) {
            val endpoint = ucenterConfigProperties.getEndpoint().getOrDefault("username-password", "ucenter/login");
            val url = "/" + StringUtils.strip(StringUtils.defaultIfBlank(endpoint, "login"), "/");

            return new SecurityFilterRegistrationBean<>(new UsernameAndPasswordFilter(url));
        }

        @Bean
        @ConditionalOnProperty(prefix = "apzda.ucenter.server.endpoint", name = "switch-to", matchIfMissing = true)
        SecurityFilterRegistrationBean<AbstractProcessingFilter> switchToFilter(
                UCenterConfigProperties ucenterConfigProperties) {
            val endpoint = ucenterConfigProperties.getEndpoint().getOrDefault("switch-to", "ucenter/switch-to");
            val url = "/" + StringUtils.strip(StringUtils.defaultIfBlank(endpoint, "switch-to"), "/");

            return new SecurityFilterRegistrationBean<>(new SwitchToFilter(url));
        }

        @Bean
        @ConditionalOnProperty(prefix = "apzda.ucenter.server.endpoint", name = "switch-back", matchIfMissing = true)
        SecurityFilterRegistrationBean<AbstractProcessingFilter> switchBackFilter(
                UCenterConfigProperties ucenterConfigProperties) {
            val endpoint = ucenterConfigProperties.getEndpoint().getOrDefault("switch-back", "ucenter/switch-back");
            val url = "/" + StringUtils.strip(StringUtils.defaultIfBlank(endpoint, "switch-back"), "/");

            return new SecurityFilterRegistrationBean<>(new SwitchBackFilter(url));
        }

        @Bean
        @ConditionalOnProperty(prefix = "apzda.ucenter.server.endpoint", name = "refresh-token", matchIfMissing = true)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        SecurityFilterRegistrationBean<AbstractProcessingFilter> refreshTokenFilter(
                UCenterConfigProperties ucenterConfigProperties) {
            val endpoint = ucenterConfigProperties.getEndpoint().getOrDefault("refresh-token", "ucenter/refresh-token");
            val url = "/" + StringUtils.strip(StringUtils.defaultIfBlank(endpoint, "refresh-token"), "/");

            return new SecurityFilterRegistrationBean<>(new RefreshTokenFilter(url));
        }

        @Bean
        JwtTokenCustomizer ucenterTokenCustomizer(JwtTokenMapper jwtTokenMapper, UserManager userManager,
                UserDetailsMetaService userDetailsMetaService, SettingService settingService,
                UCenterConfig uCenterConfig) {
            return new TokenCustomizer(userManager, jwtTokenMapper, userDetailsMetaService, settingService,
                    uCenterConfig);
        }

        @Bean
        Authenticator googleTotpAuthenticator() {
            return new GoogleTotpAuthenticator();
        }

    }

}
