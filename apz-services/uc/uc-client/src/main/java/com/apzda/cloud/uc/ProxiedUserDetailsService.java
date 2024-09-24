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
package com.apzda.cloud.uc;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.MD5;
import com.apzda.cloud.uc.proto.Request;
import com.apzda.cloud.uc.proto.UcenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class ProxiedUserDetailsService implements UserDetailsService {

    private final UcenterService ucenterService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;

        try {
            // todo 优化用户加载, 应该总是返回null才对！！！
            val userInfo = ucenterService.getUserInfo(Request.newBuilder().setUsername(username).build());
            if (userInfo.getErrCode() == 0) {
                user = new User(username, userInfo.getPassword(), userInfo.getEnabled(),
                        userInfo.getAccountNonExpired(), userInfo.getCredentialsNonExpired(),
                        userInfo.getAccountNonLocked(), Collections.emptyList());
            }
            else if (userInfo.getErrCode() == 404) {
                throw new UsernameNotFoundException(username);
            }
            else {
                throw new IllegalStateException(userInfo.getErrMsg());
            }
        }
        catch (Exception e) {
            log.error("Cannot load user info({}), use disabled anonymous instead - {}", username, e.getMessage());
            user = new User("anonymous", MD5.create().digestHex(DateUtil.now()), false, true, true, true,
                    Collections.emptyList());
        }

        return user;
    }

}
