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
package com.apzda.cloud.uc.security.filter;

import com.apzda.cloud.gsvc.security.filter.AbstractProcessingFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class SmsLoginFilter extends AbstractProcessingFilter {

    public SmsLoginFilter(String endpoint) {
        super(new AntPathRequestMatcher(endpoint, "POST"));
        log.debug("短信验证码登录处理器: POST({})", endpoint);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        val token = readRequestBody(request, SmsLoginToken.class);

        setDetails(request, token);
        return this.getAuthenticationManager().authenticate(token);
    }

    @Getter
    public static class SmsLoginToken extends AbstractAuthenticationToken {

        private final String phone;

        private final String code;

        private final String recCode;

        private final String templateId;

        SmsLoginToken(String phone, String code, String recCode) {
            super(new ArrayList<>());
            this.phone = phone;
            this.code = code;
            this.recCode = recCode;
            this.templateId = "login";
        }

        @Override
        public Object getCredentials() {
            return code;
        }

        @Override
        public Object getPrincipal() {
            return phone;
        }

    }

}
