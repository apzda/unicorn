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
package com.apzda.cloud.audit.autoconfig;

import com.apzda.cloud.audit.logging.AuditLogger;
import com.apzda.cloud.audit.logging.AuditLoggerImpl;
import com.apzda.cloud.audit.proto.AuditService;
import com.apzda.cloud.gsvc.config.EnableGsvcServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@ComponentScan({ "com.apzda.cloud.audit.aop", "com.apzda.cloud.audit.listener" })
@EnableGsvcServices({ AuditService.class })
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AuditLogger auditLogger(AuditService auditService, ObservationRegistry observationRegistry,
            ObjectMapper objectMapper) {
        return new AuditLoggerImpl(auditService, objectMapper, observationRegistry);
    }

}
