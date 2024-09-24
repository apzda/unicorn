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
package com.apzda.cloud.audit.logging;

import com.apzda.cloud.audit.proto.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
public class AuditLoggerImpl implements AuditLogger {

    private final AuditService auditService;

    private final ObjectMapper objectMapper;

    private final ObservationRegistry observationRegistry;

    @Override
    public Logger activity(String activity) {
        if (StringUtils.isBlank(activity)) {
            throw new IllegalArgumentException("activity is blank");
        }
        return new Logger(auditService, objectMapper, activity, observationRegistry);
    }

}
