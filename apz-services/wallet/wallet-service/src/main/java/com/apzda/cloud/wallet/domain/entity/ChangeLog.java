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

import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@Data
@TableName("wallet_change_log")
@Slf4j
public class ChangeLog implements Serializable {

    @Serial
    private static final long serialVersionUID = -5787777442476314765L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @NotNull
    private Long transactionId;

    @NotNull
    private Long uid;

    @NotNull
    private String currency;

    @NotNull
    private String biz;

    @NotNull
    private String bizSubject;

    @NotNull
    private String bizId;

    @NotNull
    private Long amount;

    @NotNull
    private Long preBalance;

    @NotNull
    private Long balance;

    @NotNull
    private Long preFrozen;

    @NotNull
    private Long frozen;

    private boolean outlay;

    private boolean needFrozen;

    private boolean withdrawAble;

    private Long expiredAt;

    @NotNull
    @Min(0)
    private Long parentId;

    @NotNull
    private String ip;

    @NotNull
    private String block;

    private String remark;

    public void genBlock(String preBlock) {
        this.block = genBlock(this, preBlock);
    }

    public static ChangeLog init(Wallet wallet) {
        val changelog = new ChangeLog();
        if (!wallet.getBlock().equals("00000000000000000000000000000000")) {
            throw new IllegalStateException("The wallet(uid: " + wallet.getUid() + ", currency: " + wallet.getCurrency()
                    + ") had been initialized, Cannot initialize it again!");
        }
        changelog.createdAt = wallet.getCreatedAt();
        changelog.updatedAt = changelog.createdAt;
        changelog.createdBy = wallet.getCreatedBy();
        changelog.updatedBy = changelog.createdBy;
        changelog.uid = wallet.getUid();
        changelog.currency = wallet.getCurrency();
        changelog.transactionId = 0L;
        changelog.preBalance = 0L;
        changelog.preFrozen = 0L;
        changelog.amount = 0L;
        changelog.balance = 0L;
        changelog.frozen = 0L;
        changelog.outlay = false;
        changelog.needFrozen = false;
        changelog.biz = "init";
        changelog.bizSubject = "init";
        changelog.bizId = String.valueOf(wallet.getId());
        changelog.ip = "127.0.0.1";
        changelog.remark = "Initialize";
        changelog.parentId = 0L;
        changelog.block = genBlock(changelog, wallet.getBlock());
        return changelog;
    }

    public static String genBlock(ChangeLog changeLog, String preBlock) {
        // @formatter:off
        val blockStr = preBlock + ","
            + changeLog.createdAt+ ","
            + changeLog.createdBy + ","
            + changeLog.updatedAt+ ","
            + changeLog.updatedBy + ","
            + changeLog.uid + ","
            + changeLog.currency + ","
            + changeLog.transactionId+","
            + changeLog.outlay + ","
            + changeLog.needFrozen + ","
            + changeLog.withdrawAble + ","
            + changeLog.biz + ","
            + changeLog.bizSubject + ","
            + changeLog.bizId + ","
            + changeLog.amount+","
            + changeLog.preBalance+","
            + changeLog.balance+","
            + changeLog.preFrozen+","
            + changeLog.frozen+","
            + changeLog.expiredAt+","
            + changeLog.parentId+","
            + changeLog.ip;
        // @formatter:on

        val block = MD5.create().digestHex(blockStr);

        log.debug("Generated change log block({}): [{}]", block, blockStr);

        return block;
    }

}
