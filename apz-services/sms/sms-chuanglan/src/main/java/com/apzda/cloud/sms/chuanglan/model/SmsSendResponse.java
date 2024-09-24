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
package com.apzda.cloud.sms.chuanglan.model;

import lombok.Data;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
public class SmsSendResponse {

    /**
     * 响应时间
     */
    private String time;

    /**
     * 消息id
     */
    private String msgId;

    /**
     * 状态码说明（成功返回空）
     */
    private String errorMsg;

    /**
     * 失败的个数
     */
    private String failNum;

    /**
     * 成功的个数
     */
    private String successNum;

    /**
     * 状态码（详细参考提交响应状态码）
     */
    private String code;

    @Override
    public String toString() {
        return "SmsSendResponse [time=" + time + ", msgId=" + msgId + ", errorMsg=" + errorMsg + ", code=" + code + "]";
    }

}
