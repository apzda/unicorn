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
package com.apzda.cloud.uc.security.mfa;

import com.apzda.cloud.gsvc.config.Props;
import com.apzda.cloud.uc.mfa.Authenticator;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class GoogleTotpAuthenticator implements Authenticator {

    private final GoogleAuthenticator authenticator;

    public GoogleTotpAuthenticator() {
        authenticator = new GoogleAuthenticator();
    }

    @Override
    @Nonnull
    public String getType() {
        return "totp";
    }

    @Override
    public boolean verify(String code, String secretKey) {
        return authenticator.authorize(secretKey, Integer.parseInt(code));
    }

    @Override
    public String getSecretKey() {
        val credentials = authenticator.createCredentials();
        return credentials.getKey();
    }

    @Override
    public String getConfig(String username, String secretKey, Props props) {
        username = UriUtils.encodeQuery(username, StandardCharsets.UTF_8);
        val issuer = UriUtils.encode(props.getString("issuer", ""), StandardCharsets.UTF_8);
        if (StringUtils.isNotBlank(issuer)) {
            return "otpauth://totp/" + issuer + ":" + username + "?secret=" + secretKey + "&issuer=" + issuer;
        }
        else {
            return "otpauth://totp/" + username + "?secret=" + secretKey;
        }
    }

}
