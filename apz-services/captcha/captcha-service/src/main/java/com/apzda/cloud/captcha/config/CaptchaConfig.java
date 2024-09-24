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
package com.apzda.cloud.captcha.config;

import cn.hutool.core.util.ServiceLoaderUtil;
import com.apzda.cloud.captcha.provider.CaptchaProvider;
import com.apzda.cloud.captcha.storage.CaptchaStorage;
import com.apzda.cloud.captcha.storage.LocalCaptchaStorage;
import com.apzda.cloud.gsvc.config.Props;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CaptchaConfigProperties.class)
@Import({ RedisConfiguration.class })
@RequiredArgsConstructor
@Slf4j
public class CaptchaConfig implements InitializingBean {

    private final CaptchaConfigProperties properties;

    private final CaptchaStorage captchaStorage;

    @Value("${apzda.cloud.captcha.provider:image}")
    private String provider;

    @Getter
    private CaptchaProvider captchaProvider;

    @Override
    public void afterPropertiesSet() throws Exception {
        val captchaProviders = ServiceLoaderUtil.loadList(CaptchaProvider.class, this.getClass().getClassLoader());
        val props = new Props(properties.getProps());
        for (CaptchaProvider captchaProvider : captchaProviders) {
            if (provider.equals(captchaProvider.getId())) {
                captchaProvider.init(captchaStorage, props);
                this.captchaProvider = captchaProvider;
                log.info("Use Captcha Provider: {}", captchaProvider.getClass().getCanonicalName());
                break;
            }
        }
    }

    @Bean
    @ConditionalOnMissingClass("org.springframework.data.redis.core.StringRedisTemplate")
    static CaptchaStorage captchaStorage(CaptchaConfigProperties properties) {
        log.trace("CaptchaStorage class: {}", LocalCaptchaStorage.class.getCanonicalName());
        return new LocalCaptchaStorage(properties.getTimeout().plus(Duration.ofSeconds(3600)));
    }

}
