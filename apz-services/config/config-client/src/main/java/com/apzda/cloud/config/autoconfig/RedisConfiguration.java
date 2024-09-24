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

import com.apzda.cloud.config.listener.RedisMessageSubscriber;
import com.apzda.cloud.config.service.SettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisConnectionFactory.class)
@Slf4j
class RedisConfiguration {

    @Bean
    @Qualifier("configRedisMessageListenerAdapter")
    MessageListenerAdapter configRedisMessageListenerAdapter(SettingService settingService) {
        return new MessageListenerAdapter(new RedisMessageSubscriber(settingService));
    }

    @Bean
    @Qualifier("configRedisMessageListenerContainer")
    RedisMessageListenerContainer configRedisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory,
            @Qualifier("configRedisMessageListenerAdapter") MessageListenerAdapter configRedisMessageListenerAdapter,
            @Qualifier("configChangedMessageTopic") ChannelTopic topic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(configRedisMessageListenerAdapter, topic);
        return container;
    }

    @Bean
    @Qualifier("configChangedMessageTopic")
    ChannelTopic configChangedMessageTopic() {
        log.debug("The Config Topic is: configChangedMessageQueue");
        return new ChannelTopic("configChangedMessageQueue");
    }

}
