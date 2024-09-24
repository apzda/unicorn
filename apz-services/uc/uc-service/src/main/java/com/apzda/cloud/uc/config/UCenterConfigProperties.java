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
package com.apzda.cloud.uc.config;

import com.apzda.cloud.uc.mfa.Authenticator;
import com.apzda.cloud.uc.security.mfa.GoogleTotpAuthenticator;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@ConfigurationProperties(prefix = "apzda.ucenter.server")
public class UCenterConfigProperties {

    /**
     * 登录时禁用验证码
     */
    private boolean captchaDisabled;

    /**
     * 第三方登录时是否自动创建账户，如果不创建则需要绑定到已有账户
     */
    private boolean autoCreateAccount = true;

    /**
     * 用户名(登录名)前缀
     */
    private String usernamePrefix = "";

    private Class<? extends Authenticator> authenticator = GoogleTotpAuthenticator.class;

    private final Map<String, String> props = new LinkedHashMap<>();

    private final Map<String, String> endpoint = new LinkedHashMap<>();

}
