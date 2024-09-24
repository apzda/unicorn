/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.captcha.aop;

import com.apzda.cloud.captcha.CaptchaException;
import com.apzda.cloud.captcha.ICaptchaData;
import com.apzda.cloud.captcha.error.CaptchaError;
import com.apzda.cloud.captcha.error.CaptchaExpired;
import com.apzda.cloud.captcha.proto.CaptchaService;
import com.apzda.cloud.captcha.proto.CheckReq;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationInterceptor {

    private final CaptchaService captchaService;

    @Before("@annotation(com.apzda.cloud.captcha.aop.ValidateCaptcha)")
    public void interceptor(JoinPoint joinPoint) {
        val args = joinPoint.getArgs();
        val captchaData = new CaptchaData();
        if (args.length > 0 && !BeanUtils.isSimpleProperty(args[0].getClass())) {
            BeanUtils.copyProperties(args[0], captchaData);
            log.trace("Retrieve Captcha data from args[0]: {}", captchaData);
        }

        if (StringUtils.isBlank(captchaData.getCaptchaId())) {
            val request = GsvcContextHolder.getRequest();
            if (request.isPresent()) {
                val uuid = StringUtils.defaultIfBlank(GsvcContextHolder.header("X-CAPTCHA-UUID"),
                        GsvcContextHolder.header("UUID"));
                val id = GsvcContextHolder.header("X-CAPTCHA-ID");
                captchaData.setCaptchaId(id);
                captchaData.setCaptchaUuid(uuid);
                log.trace("Retrieve Captcha data from header: {}", captchaData);
            }
        }

        if (StringUtils.isNotBlank(captchaData.getCaptchaId())) {
            val uuid = captchaData.getCaptchaUuid();
            val id = captchaData.getCaptchaId();
            log.debug("Start to validate captcha: uuid({}), id({}))", uuid, id);
            val req = CheckReq.newBuilder().setUuid(uuid).setId(id).build();
            val validate = captchaService.check(req);
            if (validate == null) {
                log.warn("Captcha(uuid:{}, id:{}) is error: no response", uuid, id);
                throw new CaptchaException(new CaptchaError(I18nUtils.t("captcha.invalid")));
            }
            else if (validate.getErrCode() == 1) {
                log.warn("Captcha(uuid:{}, id:{}) is invalid: {}", uuid, id, validate.getErrMsg());
                throw new CaptchaException(new CaptchaError(validate.getErrMsg()));
            }
            else if (validate.getErrCode() == 2) {
                log.warn("Captcha(uuid:{}, id:{}) is expired: {}", uuid, id, validate.getErrMsg());
                throw new CaptchaException(new CaptchaExpired(validate.getErrMsg()));
            }
            return;
        }
        log.debug("Captcha not supported!");
        throw new CaptchaException(new CaptchaError(I18nUtils.t("captcha.not.support")));
    }

    @Data
    static class CaptchaData implements ICaptchaData {

        private String captchaUuid;

        private String captchaId;

    }

}
