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
package com.apzda.cloud.wallet.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@Data
@TableName("wallet_transaction")
@Slf4j
public class Transaction implements Serializable {

    @Serial
    private static final long serialVersionUID = -5787777442476314765L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableLogic(value = "0", delval = "1")
    private boolean deleted;

    @NotNull
    private Long uid;

    /**
     * 币种
     */
    @NotNull
    private String currency;

    /**
     * 业务线
     */
    @NotNull
    private String biz;

    /**
     * 业务活动
     */
    @NotNull
    private String bizSubject;

    /**
     * 业务订单ID
     */
    @NotNull
    private String bizId;

    /**
     * 交易额
     */
    @NotNull
    private Long amount;

    /**
     * 是否是支出交易
     */
    private boolean outlay;

    /**
     * 是否冻结金额，仅当outlay=true时有效.
     */
    private boolean needFrozen;

    /**
     * 是否可提现，为true时，outlay=false
     */
    private boolean withdrawAble;

    /**
     * 交易额过期时间（针对有时效性的收入交易）
     */
    private Long expiredAt;

    /**
     * 交易交生时的IP
     */
    @NotNull
    private String ip;

    /**
     * 备注
     */
    private String remark;

}
