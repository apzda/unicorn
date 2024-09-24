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
package com.apzda.cloud.sms.provider;

import cn.hutool.core.collection.CollectionUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.apzda.cloud.sms.SmsProvider;
import com.apzda.cloud.sms.config.ProviderProperties;
import com.apzda.cloud.sms.dto.Sms;
import com.apzda.cloud.sms.dto.Variable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class DayuSmsProvider implements SmsProvider {

    public static final String DAYU = "dayu";

    private boolean testMode;

    private IAcsClient acsClient;

    @Override
    public String getId() {
        return DAYU;
    }

    @Override
    public void init(ProviderProperties props) throws Exception {
        if (!props.isEnabled()) {
            this.testMode = true;
            return;
        }
        val accessKey = props.getAccessKey();
        val secretKey = props.getSecretKey();
        val regionId = props.getRegionId();
        Assert.isTrue(StringUtils.isNotBlank(accessKey), "accessKey is blank");
        Assert.isTrue(StringUtils.isNotBlank(secretKey), "secretKey is blank");
        Assert.isTrue(StringUtils.isNotBlank(regionId), "regionId is blank");
        this.testMode = props.isTestMode();
        if (this.testMode) {
            return;
        }
        IClientProfile profile = DefaultProfile.getProfile(regionId, accessKey, secretKey);
        acsClient = new DefaultAcsClient(profile);
    }

    @Override
    public boolean send(Sms sms) throws Exception {
        if (testMode) {
            return true;
        }
        val sendSmsRequest = new SendSmsRequest();
        sendSmsRequest.setPhoneNumbers(sms.getPhone());
        sendSmsRequest.setSignName(sms.getSignName());
        sendSmsRequest.setTemplateCode(sms.getTemplateId());
        if (sms.getSmsLogId() != null) {
            sendSmsRequest.setOutId(sms.getSmsLogId().toString());
        }
        val variables = sms.getVariables();
        if (CollectionUtil.isNotEmpty(variables)) {
            sendSmsRequest.setTemplateParam(Variable.toJsonStr(variables));
        }
        val sendSmsResponse = acsClient.getAcsResponse(sendSmsRequest);
        if (log.isDebugEnabled()) {
            log.debug("Dayu: {Code:{},Message:{},RequestId:{},BizId:{}}", sendSmsResponse.getCode(),
                    sendSmsResponse.getMessage(), sendSmsResponse.getRequestId(), sendSmsResponse.getBizId());
        }
        return "OK".equals(sendSmsResponse.getCode());
    }

}
