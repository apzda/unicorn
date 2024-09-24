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
package com.apzda.cloud.uc.security.realm;

import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.uc.domain.entity.Oauth;
import com.apzda.cloud.uc.realm.AuthenticatingRealm;
import com.apzda.cloud.uc.realm.RealmUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Component("dbAuthenticatingRealm")
@RequiredArgsConstructor
@Slf4j
public class InternalAuthenticatingRealm implements AuthenticatingRealm {

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    @Override
    public String getName() {
        return Oauth.SIMPLE;
    }

    @Override
    public RealmUser authenticate(@NonNull Authentication authentication) throws AuthenticationException {
        // 加载用户数据
        val username = (String) authentication.getPrincipal();
        val credentials = authentication.getCredentials();
        val userDetails = userDetailsService.loadUserByUsername(username);
        UserDetailsMeta.checkUserDetails(userDetails);

        if (passwordEncoder.matches((CharSequence) credentials, userDetails.getPassword())) {
            return new InternalRealmUser(null, userDetails);
        }

        return null;
    }

    /**
     * @author fengz (windywany@gmail.com)
     * @version 1.0.0
     * @since 1.0.0
     **/
    @Setter
    @Getter
    public static class InternalRealmUser extends User implements RealmUser {

        private String domain;

        public InternalRealmUser(String domain, UserDetails userDetails) {
            super(userDetails.getUsername(), userDetails.getPassword(), userDetails.isEnabled(),
                    userDetails.isAccountNonExpired(), userDetails.isCredentialsNonExpired(),
                    userDetails.isAccountNonLocked(), userDetails.getAuthorities());
            this.domain = domain;
        }

    }

}
