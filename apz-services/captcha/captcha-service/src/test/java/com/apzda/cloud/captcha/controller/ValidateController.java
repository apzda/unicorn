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
package com.apzda.cloud.captcha.controller;

import com.apzda.cloud.captcha.ICaptchaData;
import com.apzda.cloud.captcha.aop.ValidateCaptcha;
import com.apzda.cloud.gsvc.dto.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/captcha")
@Slf4j
public class ValidateController {

    @ValidateCaptcha
    @GetMapping("/biz")
    public Response<String> validate() {
        log.info("理论上到不了这里");
        return Response.success("OK");
    }

    @ValidateCaptcha
    @PostMapping("/bizx")
    public Response<String> validate1(@RequestBody TestRequest testRequest) {
        log.info("理论上到不了这里");
        return Response.success("OK");
    }

    @Data
    public static class TestRequest implements ICaptchaData {

        private String captchaUuid;

        private String captchaId;

        private String name;

    }

}
