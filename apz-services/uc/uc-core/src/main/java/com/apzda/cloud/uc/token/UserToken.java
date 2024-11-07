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
package com.apzda.cloud.uc.token;

import com.apzda.cloud.gsvc.security.token.JwtToken;
import com.apzda.cloud.uc.vo.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserToken implements JwtToken, Serializable {

    @Serial
    private static final long serialVersionUID = -2763131228048354173L;

    private String uid;

    private String name;

    private String runAs;

    private String provider;

    private String displayName;

    private String firstName;

    private String lastName;

    private String avatar;

    private String phone;

    private String phonePrefix;

    private String email;

    private String status;

    private String timezone;

    private String theme;

    private String lang;

    private String accessToken;

    private String refreshToken;

    private Long lastLoginTime;

    private String lastLoginIp;

    private Tenant tenant;

    private Organization organization;

    private Department department;

    private Job job;

    private String mfa;

    private String landingUrl;

    private boolean locked;

    private boolean credentialsExpired;

    private List<String> authorities;

    private Map<String, Tenant> tenants;

    private List<Role> roles;

}
