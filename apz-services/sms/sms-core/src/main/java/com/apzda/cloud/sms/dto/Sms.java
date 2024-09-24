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
package com.apzda.cloud.sms.dto;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
public class Sms {

    private Long smsLogId;

    /**
     * 业务ID
     */
    private String tid;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 运营商处短信模板ID
     */
    private String templateId;

    /**
     * 签名
     */
    private String signName;

    /**
     * 短信提供商
     */
    private String vendor;

    /**
     * 是否同步发送
     */
    private boolean sync;

    /**
     * 变量
     */
    private List<Variable> variables;

    /**
     * 短信正文
     */
    private String content;

    /**
     * 发送间隔，单位: 秒
     */
    private int intervals;

    /**
     * 有效期，单位: 秒
     */
    private int timeout;

    private String original;

    private boolean testMode;

    public int getIntervals() {
        return Math.max(30, intervals);
    }

    public List<Variable> getVariables() {
        return CollectionUtils.isEmpty(variables) ? new ArrayList<>() : variables;
    }

}
