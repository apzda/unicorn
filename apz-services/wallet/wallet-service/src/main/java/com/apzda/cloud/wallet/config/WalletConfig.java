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
package com.apzda.cloud.wallet.config;

import com.apzda.cloud.gsvc.mybatis.MybatisCustomizer;
import lombok.val;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.util.Set;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration
@EnableConfigurationProperties(WalletProperties.class)
@MapperScan(basePackages = { "com.apzda.cloud.wallet.domain.mapper" })
public class WalletConfig implements ApplicationContextAware {

    private static WalletProperties properties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        properties = applicationContext.getBean(WalletProperties.class);
    }

    @NonNull
    public static WalletProperties.CurrencyConfig getCurrencyConfig(String currency) {
        if (properties == null) {
            throw new IllegalStateException("Cannot get Currency Configuration before WalletConfig Bean initialized!");
        }

        val currencyConfig = properties.getCurrency().get(currency);

        if (currencyConfig == null) {
            throw new IllegalStateException("Configuration of '" + currency + "' not found, please configure it");
        }

        if (currencyConfig.isEnabledExpire()) {
            val defaultExpireSubject = new WalletProperties.BizSubject();
            defaultExpireSubject.setName("expire");
            val bizConfig = currencyConfig.getBiz().getOrDefault("system", new WalletProperties.BizConfig());
            val expire = bizConfig.getSubjects().getOrDefault("expire", defaultExpireSubject);
            expire.setOutlay(true);
            expire.setNeedFrozen(false);
            expire.setWithdrawAble(false);
            bizConfig.getSubjects().put("expire", expire);
            currencyConfig.getBiz().put("system", bizConfig);
        }

        return currencyConfig;
    }

    @Bean
    MybatisCustomizer mybatisCustomizer() {
        return new MybatisCustomizer() {
            @Override
            public void addLocation(@NonNull Set<String> locations) {
                locations.add("classpath*:/com/apzda/cloud/**/*Mapper.xml");
            }

            @Override
            public void addTenantIgnoreTable(@NonNull Set<String> tables) {
                tables.add("wallet");
                tables.add("wallet_transaction");
                tables.add("wallet_outlay_log");
            }
        };
    }

}
