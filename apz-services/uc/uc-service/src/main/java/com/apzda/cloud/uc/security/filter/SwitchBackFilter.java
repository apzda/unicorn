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
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.security.filter.AbstractProcessingFilter;
import com.apzda.cloud.uc.error.NotSwitchedError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
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
public class SwitchBackFilter extends AbstractProcessingFilter {

    public SwitchBackFilter(String endpoint) {
        super(new AntPathRequestMatcher(endpoint, "POST"));
        log.debug("切回用户处理器: POST({})", endpoint);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        val currentUser = CurrentUserProvider.getCurrentUser();
        val runAs = currentUser.getRunAs();
        if (StringUtils.isBlank(runAs)) {
            throw new GsvcException(new NotSwitchedError());
        }

        val token = new SwitchBackToken(runAs, currentUser.getUid());

        setDetails(request, token);

        return this.getAuthenticationManager().authenticate(token);
    }

    /**
     * @author fengz (windywany@gmail.com)
     * @version 1.0.0
     * @since 1.0.0
     **/
    public static class SwitchBackToken extends AbstractAuthenticationToken {

        private final Object principal;

        @Getter
        private final String puppet;

        SwitchBackToken(Object principal, String puppet) {
            super(new ArrayList<>());
            this.principal = principal;
            this.puppet = puppet;
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
