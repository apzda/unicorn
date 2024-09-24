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
package com.apzda.cloud.uc.controller;

import cn.hutool.core.util.RandomUtil;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.sms.proto.SendReq;
import com.apzda.cloud.sms.proto.SendRes;
import com.apzda.cloud.sms.proto.SmsService;
import com.apzda.cloud.sms.proto.Variable;
import com.apzda.cloud.uc.dto.SendLoginCodeDto;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    /**
     * 发送登录验证码.
     */
    @PostMapping("${apzda.ucenter.server.endpoint.send-login-code:/ucenter/send-login-code}")
    public Response<SendRes> sendLoginSms(@RequestBody @NotNull SendLoginCodeDto dto) {
        val builder = SendReq.newBuilder();
        builder.addPhone(dto.getPhone());
        builder.setTid("login");
        val code = RandomUtil.randomString("0123456789", 6);
        builder.addVariable(Variable.newBuilder().setName("code").setValue(code).build());
        return Response.wrap(smsService.send(builder.build()));
    }

}
