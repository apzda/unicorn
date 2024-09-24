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
package com.apzda.cloud.audit.aop;

import com.apzda.cloud.audit.ValueSanitizer;
import com.apzda.cloud.audit.proto.AuditService;
import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;
import java.util.concurrent.CompletableFuture;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Component
@Aspect
@Slf4j
@Order
@RequiredArgsConstructor
public class AuditLogAdvisor implements InitializingBean {

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    private final ParserContext parserContext = new TemplateParserContext();

    private final AuditService auditService;

    private final ObjectMapper objectMapper;

    private final ObservationRegistry observationRegistry;

    private final ObjectProvider<ValueSanitizer<?>> valueSanitizerProvider;

    @Override
    public void afterPropertiesSet() throws Exception {
        AuditContextHolder.valueSanitizerProvider = valueSanitizerProvider;
    }

    @Around("@annotation(com.apzda.cloud.audit.aop.AuditLog)")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        val args = pjp.getArgs();
        val signature = (MethodSignature) pjp.getSignature();
        val method = signature.getMethod();
        val ann = method.getAnnotation(AuditLog.class);
        val activity = StringUtils.defaultIfBlank(ann.value(), ann.activity());
        val oldContext = AuditContextHolder.getContext();
        try {
            if (StringUtils.isBlank(activity)) {
                throw new IllegalArgumentException("activity is blank");
            }
            AuditContextHolder.create();
            Object returnObj = null;
            Exception lastEx = null;
            try {
                returnObj = pjp.proceed(args);
            }
            catch (Exception e) {
                lastEx = e;
            }
            val ctx = AuditContextHolder.getContext();
            val currentUser = CurrentUserProvider.getCurrentUser();
            val userId = StringUtils.defaultIfBlank(ctx.getUsername(),
                    StringUtils.defaultIfBlank(currentUser.getUid(), "0"));
            val tenantId = StringUtils.defaultIfBlank(ctx.getTenantId(), TenantManager.tenantId("0"));
            val ip = GsvcContextHolder.getRemoteIp();
            val device = currentUser.getDevice();
            val builder = com.apzda.cloud.audit.proto.AuditLog.newBuilder();

            builder.setTimestamp(System.currentTimeMillis());
            builder.setUserid(userId);
            builder.setActivity(activity);
            builder.setTenantId(tenantId);
            builder.setIp(ip);
            if (device != null) {
                builder.setDevice(device);
            }
            if (lastEx == null) {
                builder.setLevel(StringUtils.defaultIfBlank(ann.level(), "info"));
            }
            else {
                builder.setLevel("error");
            }
            val runAs = currentUser.getRunAs();
            if (runAs != null) {
                builder.setRunas(runAs);
            }
            val context = new StandardEvaluationContext(pjp.getTarget());
            context.setVariable("returnObj", returnObj);
            context.setVariable("throwExp", transform(lastEx));
            context.setVariable("isThrow", lastEx != null);
            context.setVariable("newValue", ctx.getNewValue());
            context.setVariable("oldValue", ctx.getOldValue());
            context.setVariable("cData", ctx.getData());
            if (ctx.getNewValue() != null) {
                builder.setNewJsonValue(ctx.getNewValueAsString());
            }
            if (ctx.getOldValue() != null) {
                builder.setOldJsonValue(ctx.getOldValueAsString());
            }
            val parameters = method.getParameters();
            var i = 0;
            for (Parameter parameter : parameters) {
                val name = parameter.getName();
                context.setVariable(name, args[i++]);
            }
            if (ann.async()) {
                val gsvcContext = GsvcContextHolder.getContext();
                val throwObj = lastEx;
                CompletableFuture.runAsync(() -> {
                    try {
                        gsvcContext.restore();
                        val observation = Observation.createNotStarted("async", this.observationRegistry);
                        observation.observe(() -> {
                            audit(ann, context, builder, throwObj);
                        });
                    }
                    finally {
                        GsvcContextHolder.clear();
                    }
                });
            }
            else {
                audit(ann, context, builder, lastEx);
            }
            if (lastEx != null) {
                throw lastEx;
            }
            return returnObj;
        }
        finally {
            AuditContextHolder.restore(oldContext);
        }
    }

    private void audit(AuditLog ann, StandardEvaluationContext context,
            com.apzda.cloud.audit.proto.AuditLog.Builder builder, Exception lastEx) {
        try {
            var template = ann.template();
            var message = ann.message();
            if (lastEx != null) {
                val error = ann.error();
                if (StringUtils.isBlank(error)) {
                    val errTpl = ann.errorTpl();
                    if (StringUtils.isBlank(errTpl)) {
                        message = lastEx.getMessage();
                    }
                    else {
                        template = errTpl;
                    }
                }
                else {
                    message = error;
                }
            }

            if (StringUtils.isNotBlank(message)) {
                val evaluate = message.startsWith("#{") && message.endsWith("}");
                if (evaluate) {
                    try {
                        val expression = expressionParser.parseExpression(message, parserContext);
                        val msg = expression.getValue(context, String.class);
                        builder.setMessage(msg);
                    }
                    catch (Exception e) {
                        builder.setLevel("warn");
                        builder.setMessage(e.getMessage());
                    }
                }
                else {
                    builder.setMessage(message);
                }
            }
            else if (StringUtils.isNotBlank(template)) {
                builder.setTemplate(true);
                builder.setMessage(template);
                val arg = ann.args();
                var idx = 0;
                var argVal = "";
                for (String value : arg) {
                    if (value.startsWith("#")) {
                        try {
                            val expression = expressionParser.parseExpression(value);
                            argVal = expression.getValue(context, String.class);
                        }
                        catch (Exception e) {
                            log.warn("Cannot parse arg: {}", value);
                            argVal = value;
                        }
                    }
                    else {
                        argVal = value;
                    }
                    argVal = StringUtils.defaultIfBlank(argVal, "");
                    builder.addArg(com.apzda.cloud.audit.proto.Arg.newBuilder().setIndex(idx++).setValue(argVal));
                }
            }

            com.apzda.cloud.audit.logging.Logger.log(auditService, objectMapper, builder);
        }
        catch (Exception e) {
            try {
                log.warn("Cannot send audit log: {} - {}", objectMapper.writeValueAsString(builder.build()),
                        e.getMessage(), e);
            }
            catch (Exception ignored) {
            }
        }
    }

    private Throwable transform(Throwable e) {
        if (e == null) {
            return e;
        }
        do {
            val message = e.getMessage();
            if (StringUtils.isNotBlank(message)) {
                return e;
            }
            if (e.getCause() == null) {
                return e;
            }
            e = e.getCause();
        }
        while (true);
    }

}
