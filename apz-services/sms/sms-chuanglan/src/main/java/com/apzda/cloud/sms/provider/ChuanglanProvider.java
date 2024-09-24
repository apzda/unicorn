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
package com.apzda.cloud.sms.provider;

import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.apzda.cloud.sms.SmsProvider;
import com.apzda.cloud.sms.chuanglan.model.SmsSendRequest;
import com.apzda.cloud.sms.chuanglan.model.SmsSendResponse;
import com.apzda.cloud.sms.chuanglan.model.SmsVariableRequest;
import com.apzda.cloud.sms.chuanglan.utils.DES3Utils;
import com.apzda.cloud.sms.config.ProviderProperties;
import com.apzda.cloud.sms.dto.Sms;
import com.apzda.cloud.sms.dto.Variable;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class ChuanglanProvider implements SmsProvider {

    public static final String CHUANGLAN = "chuanglan";

    private static final String SEND_SAME_SMS = "/send/encrypt/json";

    private static final String SEND_VARIABLE_SMS = "/variable/encrypt/json";

    private static final TypeReference<TreeMap<String, Object>> TYPE_REFERENCE = new TypeReference<>() {
    };

    private String username;

    private String password;

    private String passwordMd5;

    private boolean testMode;

    private RestClient client;

    @Override
    public String getId() {
        return CHUANGLAN;
    }

    @Override
    public void init(ProviderProperties props) throws Exception {
        if (!props.isEnabled()) {
            this.testMode = true;
            return;
        }
        this.testMode = props.isTestMode();
        if (this.testMode) {
            return;
        }
        this.username = props.getUsername();
        Assert.isTrue(StringUtils.isNotBlank(username), "username cannot be blank");
        this.password = props.getPassword();
        Assert.isTrue(StringUtils.isNotBlank(password), "password cannot be blank");
        this.passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        val endpoint = props.getEndpoint();
        Assert.isTrue(StringUtils.isNotBlank(endpoint), "endpoint cannot be blank");

        this.client = RestClient.builder()
            .baseUrl(StringUtils.stripEnd(endpoint, "/") + "/")
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .defaultRequest((spec) -> {
                spec.acceptCharset(StandardCharsets.UTF_8).accept(MediaType.APPLICATION_JSON);
            })
            .build();
    }

    @Override
    public boolean send(Sms sms) throws Exception {
        if (this.testMode) {
            return true;
        }

        RestClient.RequestBodySpec requestBodySpec;
        if (!CollectionUtils.isEmpty(sms.getVariables())) {
            val variables = new ArrayList<>(sms.getVariables());
            variables.add(new Variable("phone", sms.getPhone(), -1));
            val sb = new StringBuilder();
            variables.stream().sorted(Comparator.comparingInt(Variable::getIndex)).forEach(variable -> {
                sb.append(variable.getValue()).append(",");
            });
            val params = sb.deleteCharAt(sb.length() - 1).toString();
            val smsSendRequest = new SmsVariableRequest(username, null, sms.getOriginal(),
                    DES3Utils.encryptBase64(params, passwordMd5));
            smsSendRequest.setTimestamp(String.valueOf(System.currentTimeMillis()));
            val paramMap = ResponseUtils.OBJECT_MAPPER.convertValue(smsSendRequest, TYPE_REFERENCE);
            smsSendRequest.setSign(sign(paramMap));
            requestBodySpec = this.client.post().uri(SEND_VARIABLE_SMS).body(smsSendRequest);
        }
        else {
            val phone = DES3Utils.encryptBase64(sms.getPhone(), passwordMd5);
            val smsSendRequest = new SmsSendRequest(username, null, sms.getContent(), phone);
            smsSendRequest.setTimestamp(String.valueOf(System.currentTimeMillis()));
            val paramMap = ResponseUtils.OBJECT_MAPPER.convertValue(smsSendRequest, TYPE_REFERENCE);
            smsSendRequest.setSign(sign(paramMap));
            requestBodySpec = this.client.post().uri(SEND_SAME_SMS).body(smsSendRequest);
        }

        val response = requestBodySpec.retrieve().toEntity(SmsSendResponse.class);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            val body = response.getBody();
            if (body != null && "0".equals(body.getCode())) {
                return true;
            }
            else if (body != null) {
                throw new IllegalStateException(String.format("[%s]%s", body.getCode(), body.getErrorMsg()));
            }
            else {
                throw new IllegalStateException("创蓝无响应");
            }
        }
        throw new IllegalStateException("创蓝响应错误: " + response.getStatusCode());
    }

    private String sign(TreeMap<String, Object> params) {
        StringBuffer sb = new StringBuffer();
        params.values().forEach(obj -> {
            sb.append(obj.toString());
        });
        sb.append(passwordMd5);
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

}
