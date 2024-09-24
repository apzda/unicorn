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
import com.apzda.cloud.gsvc.security.exception.TokenException;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.apzda.cloud.uc.UserMetas;
import com.apzda.cloud.uc.domain.entity.Oauth;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.security.filter.SwitchBackFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class SwitchBackAuthenticationProvider implements AuthenticationProvider {

    private final UserManager userManager;

    private final UserDetailsService userDetailsService;

    private final UserDetailsMetaRepository userDetailsMetaRepository;

    @Override
    @AuditLog(activity = "runas", template = "switch back from '{}' successfully",
            errorTpl = "switch back from '{}' failure: {}", args = { "#authentication.puppet", "#throwExp?.message" })
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        val token = (SwitchBackFilter.SwitchBackToken) authentication;
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken tk) {
            val udOpt = tk.getUserDetails();
            if (udOpt.isEmpty()) {
                throw TokenException.EXPIRED;
            }
            // 清空RUNAS元数据
            val ud = udOpt.get();
            val username = (String) token.getPrincipal();
            val keptUsername = ud.get(UserMetas.RUNNING_AS, tk);
            if (!Objects.equals(username, keptUsername)) {
                throw TokenException.EXPIRED;
            }
            else {
                ud.remove(UserMetas.RUNNING_AS, tk);
            }

            // 重新登录原用户
            val userDetails = userDetailsService.loadUserByUsername(username);
            UserDetailsMeta.checkUserDetails(userDetails);

            val userDetailsMeta = userDetailsMetaRepository.create(userDetails);
            val authed = JwtAuthenticationToken.authenticated(userDetailsMeta, userDetails.getPassword());
            userDetailsMeta.remove(UserDetailsMeta.AUTHORITY_META_KEY);
            userDetailsMeta.remove(UserMetas.RUNNING_AS, authed);
            userDetailsMeta.set(UserDetailsMeta.LOGIN_TIME_META_KEY, authed, 0L);
            userDetailsMeta.setOpenId(userDetailsMeta.getOpenId());
            userDetailsMeta.setUnionId(userDetailsMeta.getUnionId());
            userDetailsMeta.setProvider(userDetailsMeta.getProvider());
            // this is a transit Oauth Object!!!
            val oauth = new Oauth();
            oauth.setOpenId(userDetailsMeta.getOpenId());
            oauth.setUnionId(userDetailsMeta.getUnionId());
            oauth.setProvider(userDetailsMeta.getProvider());
            oauth.setRemark("switch back from " + ud.getUsername());
            userManager.onAuthenticated(authed, oauth);

            return authed;
        }

        throw TokenException.EXPIRED;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SwitchBackFilter.SwitchBackToken.class.isAssignableFrom(authentication);
    }

}
