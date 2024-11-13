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
package com.apzda.cloud.captcha.controller;

import com.apzda.cloud.captcha.proto.CaptchaService;
import com.apzda.cloud.captcha.proto.CreateReq;
import com.apzda.cloud.captcha.proto.CreateRes;
import com.apzda.cloud.captcha.proto.ValidateReq;
import com.apzda.cloud.gsvc.dto.Response;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.bind.annotation.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping("/create")
    public Response<CreateRes> create(@RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height) {
        val builder = CreateReq.newBuilder();
        if (width != null) {
            builder.setWidth(width);
        }
        if (height != null) {
            builder.setHeight(height);
        }
        return Response.wrap(captchaService.create(builder.build()));
    }

    @PostMapping("/validate")
    public Response<?> validate(@RequestBody ValidateReq req) {
        return Response.wrap(captchaService.validate(req));
    }

}
