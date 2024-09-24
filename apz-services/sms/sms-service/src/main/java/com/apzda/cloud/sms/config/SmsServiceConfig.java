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
package com.apzda.cloud.sms.config;

import cn.hutool.core.util.ServiceLoaderUtil;
import com.apzda.cloud.gsvc.config.Props;
import com.apzda.cloud.gsvc.i18n.MessageSourceNameResolver;
import com.apzda.cloud.sms.SmsProvider;
import com.apzda.cloud.sms.SmsSender;
import com.apzda.cloud.sms.domain.repository.SmsLogRepository;
import com.apzda.cloud.sms.listener.SmsEventListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SmsConfigProperties.class)
@EnableJpaRepositories("com.apzda.cloud.sms.domain.repository")
@EntityScan("com.apzda.cloud.sms.domain.entity")
@RequiredArgsConstructor
@Getter
public class SmsServiceConfig implements InitializingBean, SmartLifecycle {

    private final SmsConfigProperties properties;

    private final Map<String, SmsProvider> enabledSmsProviders = new HashMap<>();

    private final Map<String, ProviderProperties> providerProperties = new HashMap<>();

    private SmsSender smsSender;

    private SmsProvider smsProvider;

    private volatile boolean running = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        val sender = properties.getSender();
        if (StringUtils.isBlank(sender)) {
            throw new Exception("Sms Sender not specified!");
        }

        val providers = properties.getProviders();

        if (providers.isEmpty()) {
            throw new Exception("No Sms Provider specified!");
        }

        val enabledProviders = providers.stream().filter(ProviderProperties::isEnabled).peek(ep -> {
            providerProperties.put(ep.getId(), ep);
        }).toList();

        if (enabledProviders.isEmpty()) {
            throw new Exception("All Sms Provider are disabled!");
        }

        val defaultProvider = StringUtils.defaultIfBlank(properties.getDefaultProvider(),
                enabledProviders.get(0).getId());

        // 初始化服务商
        val smsProviders = ServiceLoaderUtil.loadList(SmsProvider.class, this.getClass().getClassLoader());
        if (smsProviders.isEmpty()) {
            throw new Exception("no Sms Provider found in class path!");
        }

        for (SmsProvider provider : smsProviders) {
            val pid = provider.getId();
            if (providerProperties.containsKey(pid)) {
                val pp = providerProperties.get(pid);
                if (properties.isTestMode()) {
                    pp.setTestMode(true);
                }
                provider.init(pp);
                enabledSmsProviders.put(pid, provider);
            }

            if (defaultProvider.equals(pid)) {
                smsProvider = provider;
                enabledSmsProviders.put("default", smsProvider);
            }
        }
        if (smsProvider == null) {
            throw new Exception("Sms Provider '" + defaultProvider + "' not found");
        }

        // 初始化发送器
        val smsSenders = ServiceLoaderUtil.loadList(SmsSender.class, this.getClass().getClassLoader());
        if (smsSenders.isEmpty()) {
            throw new Exception("no Sms Sender found in class path!");
        }
        val optionalSmsSender = smsSenders.stream().filter(s -> sender.equals(s.getId())).findFirst();
        if (optionalSmsSender.isPresent()) {
            smsSender = optionalSmsSender.get();
            smsSender.init(new Props(properties.getProps()), enabledSmsProviders);
        }
        else {
            throw new Exception("Sms Sender '" + sender + "' not found in class path!");
        }

    }

    @Bean("sms.MessageSourceNameResolver")
    MessageSourceNameResolver messageSourceNameResolver() {
        return () -> "messages-sms";
    }

    @Bean
    SmsEventListener smsEventListener(SmsLogRepository smsLogRepository) {
        return new SmsEventListener(smsLogRepository);
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        smsSender.stop();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
