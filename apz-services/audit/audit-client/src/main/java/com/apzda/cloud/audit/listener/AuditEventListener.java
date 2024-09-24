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
package com.apzda.cloud.audit.listener;

import com.apzda.cloud.audit.logging.AuditLogger;
import com.apzda.cloud.audit.proto.Arg;
import com.apzda.cloud.gsvc.event.AuditEvent;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener implements ApplicationListener<AuditEvent> {

    private final AuditLogger auditLogger;

    @Override
    public boolean supportsAsyncExecution() {
        return false;
    }

    @Override
    public void onApplicationEvent(@Nonnull AuditEvent event) {
        log.trace("Received AuditEvent {}", event);

        val audit = event.getAudit();
        val timestamp = event.getTimestamp();

        val activity = StringUtils.defaultIfBlank(audit.getActivity(), "audit");
        val logger = auditLogger.activity(activity);
        if (StringUtils.isNotBlank(audit.getUserId())) {
            logger.userId(audit.getUserId());
        }
        if (StringUtils.isNotBlank(audit.getTenantId())) {
            logger.tenantId(audit.getTenantId());
        }
        logger.template(Boolean.TRUE.equals(audit.getTemplate()));
        logger.timestamp(timestamp);
        if (StringUtils.isNotBlank(audit.getRunas())) {
            logger.runas(audit.getRunas());
        }
        logger.level(audit.getLevel());

        if (StringUtils.isNotBlank(audit.getIp())) {
            logger.ip(audit.getIp());
        }
        if (StringUtils.isNotBlank(audit.getDevice())) {
            logger.device(audit.getDevice());
        }
        logger.message(audit.getMessage());
        List<String> args = audit.getArgs();

        if (!CollectionUtils.isEmpty(args)) {
            var i = 0;
            for (String arg : args) {
                logger.arg(Arg.newBuilder().setValue(arg).setIndex(i++));
            }
        }
        logger.replace(audit.getOldValue(), audit.getNewValue());
        logger.log();
    }

}
