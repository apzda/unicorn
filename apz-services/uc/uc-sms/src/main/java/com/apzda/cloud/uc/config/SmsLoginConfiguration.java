/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
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

import com.apzda.cloud.gsvc.security.filter.AbstractProcessingFilter;
import com.apzda.cloud.gsvc.security.filter.SecurityFilterRegistrationBean;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.apzda.cloud.sms.proto.SmsService;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.security.authentication.SmsAuthenticationProvider;
import com.apzda.cloud.uc.security.filter.SmsLoginFilter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SmsLoginConfigProperties.class)
public class SmsLoginConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "apzda.ucenter.server.endpoint", name = "sms-login", matchIfMissing = true)
    SecurityFilterRegistrationBean<AbstractProcessingFilter> smsLoginFilter(
            UCenterConfigProperties ucenterConfigProperties) {
        val endpoint = ucenterConfigProperties.getEndpoint().getOrDefault("sms-login", "/ucenter/sms-login");
        val url = "/" + StringUtils.strip(StringUtils.defaultIfBlank(endpoint, "/ucenter/sms-login"), "/");

        return new SecurityFilterRegistrationBean<>(new SmsLoginFilter(url));
    }

    @Bean
    AuthenticationProvider smsLoginAuthenticationProvider(SmsLoginConfigProperties smsLoginConfigProperties,
            UserManager userManager, UserDetailsMetaRepository userDetailsMetaRepository, SmsService smsService) {
        return new SmsAuthenticationProvider(smsLoginConfigProperties, userManager, userDetailsMetaRepository,
                smsService);
    }

}
