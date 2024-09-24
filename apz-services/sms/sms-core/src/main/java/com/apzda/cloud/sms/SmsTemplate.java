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
package com.apzda.cloud.sms;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.gsvc.infra.TempStorage;
import com.apzda.cloud.sms.config.TemplateProperties;
import com.apzda.cloud.sms.data.SmsInfo;
import com.apzda.cloud.sms.dto.Sms;
import com.apzda.cloud.sms.dto.Variable;
import com.apzda.cloud.sms.exception.TooManySmsException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@RequiredArgsConstructor
@Getter
public class SmsTemplate {

    protected final String id;

    protected TemplateProperties properties;

    public String getName() {
        return StringUtils.defaultIfBlank(properties.getName(), id);
    }

    public Sms create(String phone, List<Variable> variables) {
        val sms = new Sms();
        sms.setTid(id);
        sms.setPhone(phone);
        sms.setTemplateId(properties.getTemplateId());
        val interval = properties.getInterval();
        if (interval != null) {
            sms.setIntervals((int) interval.toSeconds());
        }
        val timeout = properties.getTimeout();
        if (timeout != null) {
            sms.setTimeout((int) timeout.toSeconds());
        }
        sms.setSignName(properties.getSignName());
        sms.setVariables(variables);
        val content = properties.getContent();
        sms.setOriginal(content);
        if (StringUtils.isNotBlank(content) && !CollectionUtils.isEmpty(variables)) {
            sms.setContent(parseContent(content, variables));
        }
        return sms;
    }

    public String parseContent(String content, List<Variable> variables) {
        for (Variable variable : variables) {
            content = content.replace("${" + variable.getName() + "}", variable.getValue());
        }
        for (Variable variable : variables) {
            content = content.replace("{" + variable.getName() + "}", variable.getValue());
        }
        return content;
    }

    public void beforeSend(Sms sms, TempStorage storage) throws Exception {
        val phone = sms.getPhone();
        val intervals = sms.getIntervals();//
        val id = genId(phone);
        val info = storage.load(id, SmsInfo.class);
        if (info.isPresent()) {
            val smsInfo = info.get();
            val sentTime = smsInfo.getSentTime();
            if ((DateUtil.currentSeconds() - intervals < sentTime)) {
                // 发送太快
                throw new TooManySmsException("too fast!");
            }
        }
    }

    public void onSent(Sms sms, TempStorage storage) throws Exception {
        val phone = sms.getPhone();
        val smsInfo = new SmsInfo();
        smsInfo.setExpireTime(Duration.ofSeconds(sms.getTimeout()));
        smsInfo.setSentTime(System.currentTimeMillis() / 1000);
        smsInfo.setVariables(sms.getVariables());
        storage.save(genId(phone), smsInfo);
    }

    public boolean verify(Sms sms, TempStorage storage) {
        if (sms.isTestMode()) {
            return true;
        }
        val phone = sms.getPhone();
        val id = genId(phone);
        val info = storage.load(id, SmsInfo.class);
        if (info.isPresent()) {
            val smsInfo = info.get();
            val variables = smsInfo.getVariables();
            if (!CollectionUtils.isEmpty(variables)) {
                if (CollectionUtil.isEqualList(variables, sms.getVariables())) {
                    storage.remove(id);
                    return true;
                }
            }
        }
        return false;
    }

    public String genId(String phone) {
        return "sms." + phone + "." + id;
    }

    public void setProperties(TemplateProperties properties) {
        if (this.properties == null) {
            this.properties = properties;
        }
        else {
            BeanUtil.copyProperties(properties, this.properties, CopyOptions.create().ignoreNullValue());
        }
    }

    public void setSignName(String signName) {
        if (StringUtils.isNotBlank(signName) && StringUtils.isBlank(this.getProperties().getSignName())) {
            this.getProperties().setSignName(signName);
        }
    }

}
