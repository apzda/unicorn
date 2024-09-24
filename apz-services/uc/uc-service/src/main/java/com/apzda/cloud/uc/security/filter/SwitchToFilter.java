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

import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.error.NotBlankError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.security.filter.AbstractProcessingFilter;
import com.apzda.cloud.gsvc.security.utils.SecurityUtils;
import com.apzda.cloud.uc.error.AlreadySwitchedError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class SwitchToFilter extends AbstractProcessingFilter {

    public SwitchToFilter(String endpoint) {
        super(new AntPathRequestMatcher(endpoint, "POST"));
        log.debug("切换账户处理器: POST({})", endpoint);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        val permit = SecurityUtils.security().hasAuthority("RUNAS");
        if (!permit) {
            throw new AccessDeniedException("Access denied");
        }

        val currentUser = CurrentUserProvider.getCurrentUser();
        val runAs = currentUser.getRunAs();
        if (StringUtils.isNotBlank(runAs)) {
            throw new GsvcException(new AlreadySwitchedError());
        }

        val param = readRequestBody(request, UsernameAndCode.class);
        val username = param.getUsername();
        if (StringUtils.isBlank(username)) {
            throw new GsvcException(new NotBlankError("username"));
        }
        val code = param.getCode();
        if (StringUtils.isBlank(code)) {
            throw new GsvcException(new NotBlankError("code"));
        }

        val token = new SwitchToToken(username, code);

        token.setRunAs(currentUser.getUid());

        setDetails(request, token);

        return this.getAuthenticationManager().authenticate(token);
    }

    @Data
    static class UsernameAndCode {

        private String code;// 授权码

        private String username;// 用户名

    }

    /**
     * @author fengz (windywany@gmail.com)
     * @version 1.0.0
     * @since 1.0.0
     **/
    public static class SwitchToToken extends AbstractAuthenticationToken {

        private final Object principal;

        @Getter
        @Setter
        private String runAs;

        @Getter
        private final String grantCode;

        SwitchToToken(Object principal, String grantCode) {
            super(new ArrayList<>());
            this.principal = principal;
            this.grantCode = grantCode;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

    }

}
