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
package com.apzda.cloud.sms.aop;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.sms.ISmsData;
import com.apzda.cloud.sms.error.SmsInvalidException;
import com.apzda.cloud.sms.proto.SmsService;
import com.apzda.cloud.sms.proto.Variable;
import com.apzda.cloud.sms.proto.VerifyReq;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationAdvise {

    private final SmsService smsService;

    @Before("@annotation(com.apzda.cloud.sms.aop.ValidateSms)")
    public void interceptor(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ValidateSms verificationAnnotation = method.getAnnotation(ValidateSms.class);
        val tid = verificationAnnotation.tid();
        if (StringUtils.isBlank(tid)) {
            throw new IllegalArgumentException("tid is blank");
        }
        val name = verificationAnnotation.name();
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name is blank");
        }
        val args = joinPoint.getArgs();
        val smsData = new SmsData();

        if (args.length > 0 && !BeanUtils.isSimpleProperty(args[0].getClass())) {
            BeanUtils.copyProperties(args[0], smsData);
            log.trace("Retrieve Sms data from args[0]: {}", smsData);
        }

        if (StringUtils.isBlank(smsData.getPhone())) {
            val request = GsvcContextHolder.getRequest();
            if (request.isPresent()) {
                val phone = GsvcContextHolder.header("X-SMS-PHONE");
                val code = GsvcContextHolder.header("X-SMS-CODE");
                smsData.setPhone(phone);
                smsData.setCode(code);
                log.trace("Retrieve Sms data from header: {}", smsData);
            }
        }

        if (smsData.isValid()) {
            val phone = smsData.getPhone();
            log.debug("Start to validate Sms: {}", smsData);
            val builder = Variable.newBuilder().setName(name).setValue(smsData.getCode());
            val req = VerifyReq.newBuilder().setPhone(phone).setTid(tid).addVariable(builder).build();
            val validate = smsService.verify(req);
            if (validate == null) {
                log.warn("Sms({}) is error: no response", smsData);
                throw new SmsInvalidException();
            }
            else if (validate.getErrCode() == 3) {
                log.warn("Sms({}) is invalid: {}", smsData, validate.getErrMsg());
                throw new SmsInvalidException();
            }
            return;
        }
        log.debug("Sms({}) is error: missing data", smsData);
        throw new SmsInvalidException();
    }

    @Data
    static class SmsData implements ISmsData {

        private String phone;

        private String code;

        public boolean isValid() {
            return StringUtils.isNoneBlank(phone) && StringUtils.isNotBlank(code);
        }

    }

}
