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
package com.apzda.cloud.uc.security.handler;

import com.apzda.cloud.audit.aop.AuditContextHolder;
import com.apzda.cloud.audit.aop.AuditLog;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.handler.DefaultAuthenticationHandler;
import com.apzda.cloud.gsvc.security.token.JwtTokenCustomizer;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.apzda.cloud.uc.event.AccountEvent;
import com.apzda.cloud.uc.event.EventType;
import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;

import java.io.IOException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class UCenterAuthenticationHandler extends DefaultAuthenticationHandler {

    private final String tokenName;

    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void setApplicationEventPublisher(@Nonnull ApplicationEventPublisher applicationEventPublisher) {
        super.setApplicationEventPublisher(applicationEventPublisher);
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public UCenterAuthenticationHandler(SecurityConfigProperties properties, TokenManager tokenManager,
            ObjectProvider<JwtTokenCustomizer> customizers) {
        super(properties, tokenManager, customizers);
        tokenName = properties.getTokenName();
    }

    @Override
    @AuditLog(activity = "logout", message = "logout successfully")
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        if (authentication != null) {
            val name = authentication.getName();
            AuditContextHolder.getContext().setUsername(name);

            if (StringUtils.isNotBlank(tokenName)) {
                GsvcContextHolder.headers().remove(tokenName);
            }

            if (this.applicationEventPublisher != null) {
                this.applicationEventPublisher.publishEvent(new AccountEvent(name, EventType.LOGOUT));
            }
        }

        super.onLogoutSuccess(request, response, authentication);
    }

}
