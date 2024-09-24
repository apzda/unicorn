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
package com.apzda.cloud.uc.security.authentication;

import com.apzda.cloud.audit.aop.AuditLog;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.apzda.cloud.uc.UserMetas;
import com.apzda.cloud.uc.domain.entity.Oauth;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.error.GrantCodeNotMatchError;
import com.apzda.cloud.uc.security.filter.SwitchToFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class SwitchToAuthenticationProvider implements AuthenticationProvider {

    private final UserManager userManager;

    private final UserDetailsService userDetailsService;

    private final UserDetailsMetaRepository userDetailsMetaRepository;

    @Override
    @AuditLog(activity = "runas", template = "switch to '{}' with code '{}' successfully",
            errorTpl = "switch to '{}' with code '{}' failure: {}",
            args = { "#authentication.principal", "#authentication.grantCode", "#throwExp?.message" })
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        val token = (SwitchToFilter.SwitchToToken) authentication;
        val username = (String) token.getPrincipal();
        val runAs = token.getRunAs();
        val userDetails = userDetailsService.loadUserByUsername(username);
        UserDetailsMeta.checkUserDetails(userDetails);
        if (!userDetails.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException(username);
        }

        val userDetailsMeta = userDetailsMetaRepository.create(userDetails);
        val authed = JwtAuthenticationToken.authenticated(userDetailsMeta, userDetails.getPassword());
        val grantCode = token.getGrantCode();
        val code = userDetailsMeta.get(UserMetas.RUNNING_GT, authed);
        if (!Objects.equals(grantCode, code)) {
            throw new GsvcException(new GrantCodeNotMatchError());
        }
        // 授权码只能使用一次
        userDetailsMeta.remove(UserMetas.RUNNING_GT, authed);
        userDetailsMeta.remove(UserDetailsMeta.AUTHORITY_META_KEY);
        userDetailsMeta.set(UserDetailsMeta.LOGIN_TIME_META_KEY, authed, 0L);
        userDetailsMeta.set(UserMetas.RUNNING_AS, authed, runAs);
        userDetailsMeta.setOpenId(userDetailsMeta.getOpenId());
        userDetailsMeta.setUnionId(userDetailsMeta.getUnionId());
        userDetailsMeta.setProvider(userDetailsMeta.getProvider());
        // this is a transit Oauth Object!!!
        val oauth = new Oauth();
        oauth.setOpenId(userDetailsMeta.getOpenId());
        oauth.setUnionId(userDetailsMeta.getUnionId());
        oauth.setProvider(userDetailsMeta.getProvider());
        oauth.setRemark("switch to " + username);
        userManager.onAuthenticated(authed, oauth);

        return authed;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SwitchToFilter.SwitchToToken.class.isAssignableFrom(authentication);
    }

}
