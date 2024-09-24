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
package com.apzda.cloud.config.autoconfig;

import com.apzda.cloud.config.event.SettingChangedEvent;
import com.apzda.cloud.config.listener.SettingChangedListener;
import com.apzda.cloud.config.proto.ConfigService;
import com.apzda.cloud.config.service.SettingService;
import com.apzda.cloud.config.service.impl.SettingServiceImpl;
import com.apzda.cloud.gsvc.config.EnableGsvcServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static com.apzda.cloud.config.listener.SettingChangedListener.BEAN_NAME;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration(after = RedisAutoConfiguration.class)
@Import(RedisConfiguration.class)
@EnableGsvcServices(ConfigService.class)
@Slf4j
public class ConfigAutoConfiguration {

    @Bean
    SettingService gsvcBaseSettingService(ObjectMapper objectMapper, ConfigService configService) {
        return new SettingServiceImpl(objectMapper, configService);
    }

    @Bean(BEAN_NAME)
    @ConditionalOnMissingBean(name = BEAN_NAME)
    ApplicationListener<SettingChangedEvent> configSettingChangedListener(SettingService settingService) {
        log.debug("Local Setting Changed Listener configured!");
        return new SettingChangedListener(settingService);
    }

}
