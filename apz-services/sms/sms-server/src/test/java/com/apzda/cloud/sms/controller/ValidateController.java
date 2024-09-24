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
package com.apzda.cloud.sms.controller;

import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.sms.ISmsData;
import com.apzda.cloud.sms.aop.ValidateSms;
import com.apzda.cloud.sms.domain.repository.SmsLogRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.web.bind.annotation.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/sms")
@Slf4j
@RequiredArgsConstructor
public class ValidateController {

    private final SmsLogRepository smsLogRepository;

    @ValidateSms(tid = "login")
    @GetMapping("/biz")
    public Response<String> validate() {
        return Response.success("OK");
    }

    @ValidateSms(tid = "login")
    @PostMapping("/bizx")
    public Response<String> validate1(@RequestBody TestRequest testRequest) {
        return Response.success("OK:" + testRequest.getCode());
    }

    @GetMapping("/count")
    public Response<Long> count() {
        val count = smsLogRepository.count();
        return Response.success(count);
    }

    @Data
    public static class TestRequest implements ISmsData {

        private String phone;

        private String code;

    }

}
