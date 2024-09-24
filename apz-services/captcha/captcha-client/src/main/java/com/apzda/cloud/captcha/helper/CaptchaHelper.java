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
package com.apzda.cloud.captcha.helper;

import com.apzda.cloud.captcha.CaptchaException;
import com.apzda.cloud.captcha.error.CaptchaError;
import com.apzda.cloud.captcha.error.MissingCaptcha;
import com.apzda.cloud.captcha.proto.CaptchaService;
import com.apzda.cloud.captcha.proto.CheckReq;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
public class CaptchaHelper {

    private final CaptchaService captchaService;

    public void validate() {
        validate(null);
    }

    public void validate(String captchaId) {
        val uuid = StringUtils.defaultIfBlank(GsvcContextHolder.header("X-CAPTCHA-UUID"),
                GsvcContextHolder.header("UUID"));
        val id = StringUtils.defaultIfBlank(captchaId, GsvcContextHolder.header("X-CAPTCHA-ID"));

        if (StringUtils.isBlank(uuid) || StringUtils.isBlank(id)) {
            throw new CaptchaException(new MissingCaptcha());
        }
        try {
            val checked = captchaService.check(CheckReq.newBuilder().setId(id).setUuid(uuid).build());
            if (checked.getErrCode() != 0) {
                throw new CaptchaException(new CaptchaError(checked.getErrMsg()));
            }
        }
        catch (CaptchaException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CaptchaException(new CaptchaError(e.getMessage()));
        }
    }

}
