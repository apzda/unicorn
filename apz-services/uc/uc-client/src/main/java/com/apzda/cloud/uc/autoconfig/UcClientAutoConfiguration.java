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
package com.apzda.cloud.uc.autoconfig;

import com.apzda.cloud.gsvc.config.EnableGsvcServices;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.i18n.MessageSourceNameResolver;
import com.apzda.cloud.gsvc.security.config.GsvcSecurityAutoConfiguration;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaService;
import com.apzda.cloud.uc.FakeAuthenticationProvider;
import com.apzda.cloud.uc.ProxiedUserDetailsService;
import com.apzda.cloud.uc.UserDetailsMetaServiceImpl;
import com.apzda.cloud.uc.context.UCenterTenantManager;
import com.apzda.cloud.uc.properties.SecurityConfigureProperties;
import com.apzda.cloud.uc.proto.ConfigureService;
import com.apzda.cloud.uc.proto.SyncRequest;
import com.apzda.cloud.uc.proto.UcenterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.concurrent.CompletableFuture;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration(before = GsvcSecurityAutoConfiguration.class)
@EnableMethodSecurity
@EnableGsvcServices({ UcenterService.class, ConfigureService.class })
@ComponentScan("com.apzda.cloud.uc.mapper")
@EnableConfigurationProperties(SecurityConfigureProperties.class)
@Slf4j
public class UcClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TenantManager<String> tenantManager() {
        return new UCenterTenantManager();
    }

    @Bean("uc.MessageSourceNameResolver")
    MessageSourceNameResolver messageSourceNameResolver() {
        return () -> "messages-uc";
    }

    @Bean
    @ConditionalOnMissingBean
    UserDetailsService userDetailsService(UcenterService ucenterService) {
        return new ProxiedUserDetailsService(ucenterService);
    }

    @Bean
    @ConditionalOnMissingBean
    UserDetailsMetaService userDetailsMetaService(UcenterService ucenterService, ObjectMapper objectMapper) {
        return new UserDetailsMetaServiceImpl(ucenterService, objectMapper);
    }

    @Bean("defaultAuthenticationProvider")
    @ConditionalOnMissingBean
    AuthenticationProvider defaultAuthenticationProvider() {
        return new FakeAuthenticationProvider();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "apzda.ucenter.security", name = "auto-sync", havingValue = "true",
            matchIfMissing = true)
    @Slf4j
    static class SecurityConfigureSyncer implements SmartLifecycle {

        private final ConfigureService configureService;

        private final ObjectMapper objectMapper;

        private final SecurityConfigureProperties properties;

        private volatile boolean running = false;

        SecurityConfigureSyncer(ConfigureService configureService, ObjectMapper objectMapper,
                SecurityConfigureProperties properties) {
            this.configureService = configureService;
            this.objectMapper = objectMapper;
            this.properties = properties;
        }

        @Override
        public void start() {
            if (!running) {
                running = true;
                CompletableFuture.runAsync(() -> {
                    try {
                        if (properties.getResources().isEmpty() && properties.getRoles().isEmpty()
                                && properties.getPrivileges().isEmpty()) {
                            return;
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Starting sync security configuration:  {}", properties);
                        }

                        val request = SyncRequest.newBuilder()
                            .setConfiguration(objectMapper.writeValueAsString(properties))
                            .build();

                        val response = configureService.syncConfiguration(request);
                        if (response.getErrCode() != 0) {
                            throw new Exception(response.getErrMsg());
                        }
                        else if (log.isDebugEnabled()) {
                            log.debug("Sync security configuration completed");
                        }
                    }
                    catch (Exception e) {
                        log.warn("Cannot sync security configuration to ucenter server: {}", e.getMessage());
                    }
                });
            }
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

    }

}
