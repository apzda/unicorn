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
package com.apzda.cloud.sms.facade;

import com.apzda.cloud.gsvc.domain.PagerUtils;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.infra.Counter;
import com.apzda.cloud.gsvc.infra.TempStorage;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import com.apzda.cloud.sms.SmsProvider;
import com.apzda.cloud.sms.SmsSender;
import com.apzda.cloud.sms.SmsTemplate;
import com.apzda.cloud.sms.config.ProviderProperties;
import com.apzda.cloud.sms.config.SmsConfigProperties;
import com.apzda.cloud.sms.config.SmsServiceConfig;
import com.apzda.cloud.sms.config.TemplateProperties;
import com.apzda.cloud.sms.domain.SmsStatus;
import com.apzda.cloud.sms.domain.entity.SmsLog;
import com.apzda.cloud.sms.domain.repository.SmsLogRepository;
import com.apzda.cloud.sms.dto.Variable;
import com.apzda.cloud.sms.exception.TooManySmsException;
import com.apzda.cloud.sms.proto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService, InitializingBean {

    private final SmsLogRepository smsLogRepository;

    private final SmsServiceConfig serviceConfig;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final ObjectMapper objectMapper;

    private final TempStorage storage;

    private final Counter counter;

    private final SmsConfigProperties smsConfigProperties;

    private SmsSender smsSender;

    private String vendor;

    @Override
    public void afterPropertiesSet() throws Exception {
        smsSender = serviceConfig.getSmsSender();
        SmsProvider smsProvider = serviceConfig.getSmsProvider();
        vendor = smsProvider.getId();
    }

    @Override
    public SendRes send(SendReq request) {
        val builder = SendRes.newBuilder();
        val phones = request.getPhoneList();
        val variables = request.getVariableList();
        val templateId = request.getTid();
        var sync = request.getSync();

        if (request.getPhoneCount() > 1) {
            sync = false;
        }

        val template = getSmsTemplate(templateId);
        if (template == null) {
            log.warn("Sms Template({}) is not available", templateId);
            builder.setErrCode(1);
            builder.setErrMsg(I18nUtils.t("sms.template.not.found", new String[] { templateId }));
            return builder.build();
        }

        val params = variables.stream()
            .map(variable -> new Variable(variable.getName(), variable.getValue(), variable.getIndex()))
            .toList();

        for (String phone : phones) {
            try {
                checkLimit(phone, templateId, template.getProperties());
                val sms = template.create(phone, params);
                sms.setSync(sync);
                sms.setVendor(vendor);
                template.beforeSend(sms, storage);
                val smsLog = new SmsLog();
                smsLog.setPhone(sms.getPhone());
                smsLog.setTid(sms.getTid());
                smsLog.setVendor(vendor);
                smsLog.setIntervals(sms.getIntervals());
                smsLog.setContent(sms.getContent());
                smsLog.setParams(Variable.toJsonStr(params));
                smsLogRepository.save(smsLog);
                sms.setSmsLogId(smsLog.getId());
                smsSender.send(sms, applicationEventPublisher);
                template.onSent(sms, storage);
            }
            catch (Exception e) {
                log.warn("Cannot send sms({},{},{}) - {}", phone, templateId, params, e.getMessage());
                builder.setErrCode(500);
                builder.setErrMsg(I18nUtils.t("sms.send.failed"));
                return builder.build();
            }
        }

        builder.setErrCode(0);
        builder.setInterval((int) template.getProperties().getInterval().toSeconds());
        return builder.build();
    }

    @Override
    public GsvcExt.CommonRes verify(VerifyReq request) {
        val builder = GsvcExt.CommonRes.newBuilder();
        val templateId = request.getTid();
        val phone = request.getPhone();
        val variables = request.getVariableList();

        val template = getSmsTemplate(templateId);
        if (template == null) {
            log.warn("Sms Template({}) is not available", templateId);
            builder.setErrCode(3);
            builder.setErrMsg(I18nUtils.t("sms.template.not.found", new String[] { templateId }));
            return builder.build();
        }

        val params = variables.stream()
            .map(variable -> new Variable(variable.getName(), variable.getValue(), variable.getIndex()))
            .toList();

        val sms = template.create(phone, params);
        sms.setTestMode(smsConfigProperties.isTestMode());

        if (template.verify(sms, storage)) {
            builder.setErrCode(0);
        }
        else {
            builder.setErrCode(3);
            builder.setErrMsg(I18nUtils.t("sms.invalid"));
        }
        return builder.build();
    }

    @Override
    public QueryRes logs(Query request) {
        val pager = request.getPager();
        val pr = PagerUtils.of(pager);

        val logs = smsLogRepository.findAll((Specification<SmsLog>) (root, query, builder) -> {
            val cons = new ArrayList<Predicate>();
            cons.add(builder.equal(root.<Boolean>get("deleted"), false));
            if (request.hasPhone()) {
                cons.add(builder.equal(root.<String>get("phone"), request.getPhone()));
            }
            if (request.hasVendor()) {
                cons.add(builder.equal(root.<String>get("vendor"), request.getVendor()));
            }
            if (request.hasTid()) {
                cons.add(builder.equal(root.<String>get("tid"), request.getTid()));
            }
            if (request.hasTenantId()) {
                cons.add(builder.equal(root.<String>get("tenantId"), request.getTenantId()));
            }
            if (request.hasStatus()) {
                cons.add(builder.equal(root.<String>get("status"), request.getStatus()));
            }
            if (request.hasStartTime()) {
                cons.add(builder.ge(root.<Long>get("createTime"), request.getStartTime()));
            }
            if (request.hasEndTime()) {
                cons.add(builder.lt(root.<Long>get("createTime"), request.getEndTime()));
            }
            return builder.and(cons.toArray(new Predicate[0]));
        }, pr);

        val pageInfo = PagerUtils.of(logs);
        val builder = QueryRes.newBuilder();
        builder.setPager(pageInfo);
        // ^_^
        builder.addAllLogs(logs.getContent().stream().map((lg) -> objectMapper.convertValue(lg, Log.class)).toList());

        return builder.build();
    }

    @Override
    public ConfigRes config(Empty request) {
        val builder = ConfigRes.newBuilder();
        builder.setErrCode(0);
        val enabledSmsProviders = serviceConfig.getProviderProperties();
        for (ProviderProperties value : enabledSmsProviders.values()) {
            val id = value.getId();
            val name = StringUtils.defaultIfBlank(value.getName(), id);
            builder.addVendors(com.apzda.cloud.sms.proto.Variable.newBuilder().setValue(name).setName(id));
        }
        val templates = serviceConfig.getProperties().getTemplates();
        val tpl = enabledSmsProviders.get(vendor).templates(templates);
        for (SmsTemplate value : tpl.values()) {
            val id = value.getId();
            val name = value.getName();
            builder.addTemplates(com.apzda.cloud.sms.proto.Variable.newBuilder().setName(id).setValue(name));
        }
        for (SmsStatus value : SmsStatus.values()) {
            val name = value.name();
            builder.addStatus(com.apzda.cloud.sms.proto.Variable.newBuilder()
                .setName(name)
                .setValue(I18nUtils.t("sms.status." + name.toLowerCase(), name)));
        }
        return builder.build();
    }

    private void checkLimit(String phone, String tid, TemplateProperties properties) {
        val ac = counter.count("sms.ld." + phone, Duration.ofDays(1).toSeconds());
        val pac = serviceConfig.getProperties().getMaxCount();
        if (ac > pac) {
            throw new TooManySmsException(String.format("%s had send %d sms today, limit is %d", phone, ac, pac));
        }
        val dc = counter.count("sms.ld." + tid + "." + phone, Duration.ofDays(1).toSeconds());
        val pdc = properties.theCountD();
        if (dc > pdc) {
            throw new TooManySmsException(
                    String.format("%s had send %d sms(%s) today, limit is %d", phone, dc, tid, pdc));
        }
        val hc = counter.count("sms.lh." + tid + "." + phone, Duration.ofHours(1).toSeconds());
        val phc = properties.theCountH();
        if (hc > phc) {
            throw new TooManySmsException(
                    String.format("%s had send %d sms(%s)/hour, limit is %d", phone, hc, tid, phc));
        }
        val mc = counter.count("sms.lm." + tid + "." + phone, 60);
        val pmc = properties.theCountH();
        if (mc > pmc) {
            throw new TooManySmsException(
                    String.format("%s had send %d sms(%s)/minute, limit is %d", phone, mc, tid, pmc));
        }
    }

    private SmsTemplate getSmsTemplate(String templateId) {
        val properties = serviceConfig.getProperties();
        val templates = properties.getTemplates();
        val providerProperties = serviceConfig.getProviderProperties();
        val pp = providerProperties.get(vendor);
        val tpl = pp.templates(templates);
        return tpl.get(templateId);
    }

}
