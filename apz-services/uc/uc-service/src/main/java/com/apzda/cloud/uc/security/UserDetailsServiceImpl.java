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
package com.apzda.cloud.uc.security;

import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserManager userManager;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        val user = userManager.getUserByUsername(username);
        val status = user.getStatus();

        return User.withUsername(user.getUsername())
            .accountLocked(status == UserStatus.LOCKED)
            .accountExpired(status == UserStatus.EXPIRED)
            .disabled(!(status == UserStatus.ACTIVATED || status == UserStatus.PENDING))
            .credentialsExpired(userManager.isCredentialsExpired(user))
            .password(user.getPasswd())
            .build();
    }

}
