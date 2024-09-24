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

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.wallet.config.WalletConfig;
import com.apzda.cloud.wallet.config.WalletProperties;
import com.apzda.cloud.wallet.error.WalletError;
import com.apzda.cloud.wallet.proto.TradeDTO;
import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@TableName("wallet")
@Data
public class Wallet implements Serializable {

    @Serial
    private static final long serialVersionUID = 1215576796903417299L;

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

    @TableLogic(delval = "1", value = "0")
    private boolean deleted;

    @NotNull
    private Long uid;

    @NotNull
    private String currency;

    @NotNull
    private Long amount;

    @NotNull
    private Long balance;

    @NotNull
    private Long withdrawal;

    @NotNull
    private Long frozen;

    @NotNull
    private Long outlay;

    private boolean locked;

    private String block;

    public boolean isExpireAble() {
        val config = WalletConfig.getCurrencyConfig(currency);
        return config.isEnabledExpire();
    }

    public double getRate() {
        val config = WalletConfig.getCurrencyConfig(currency);
        return config.getRate();
    }

    public short getScale() {
        val config = WalletConfig.getCurrencyConfig(currency);
        return config.getScale();
    }

    public short getPrecision() {
        val config = WalletConfig.getCurrencyConfig(currency);
        return config.getPrecision();
    }

    public boolean isWithdrawAble() {
        val config = WalletConfig.getCurrencyConfig(currency);
        return config.isWithdrawAble();
    }

    public ChangeLog newChangeLog(@NonNull Transaction transaction, ChangeLog lastLog) {
        val amount = transaction.getAmount();
        val outlay = transaction.isOutlay();
        val withdrawAble = transaction.isWithdrawAble();
        val needFrozen = transaction.isNeedFrozen();

        val changeLog = new ChangeLog();
        changeLog.setUid(transaction.getUid());
        changeLog.setCurrency(transaction.getCurrency());
        changeLog.setParentId(lastLog.getId());
        changeLog.setTransactionId(transaction.getId());
        changeLog.setBiz(transaction.getBiz());
        changeLog.setBizSubject(transaction.getBizSubject());
        changeLog.setBizId(transaction.getBizId());
        changeLog.setIp(transaction.getIp());
        changeLog.setRemark(transaction.getRemark());
        changeLog.setAmount(amount);// 交易金额
        changeLog.setPreBalance(lastLog.getBalance());// 交易前余额
        changeLog.setPreFrozen(lastLog.getFrozen());// 交易前冻结金额
        changeLog.setOutlay(outlay);
        changeLog.setNeedFrozen(needFrozen);
        changeLog.setWithdrawAble(withdrawAble);
        changeLog.setExpiredAt(transaction.getExpiredAt());

        // 1. 检测交易
        if (outlay) {
            // 支出
            changeLog.setBalance(lastLog.getBalance() - amount);
            if (changeLog.getBalance() < 0) {
                WalletError.INSUFFICIENT_BALANCE.emit(this);
            }

            if (withdrawAble && this.withdrawal - amount < 0) {
                WalletError.INSUFFICIENT_BALANCE1.emit(this);
            }
            this.withdrawal = Math.max(0, this.withdrawal - amount);

            if (needFrozen) { // 冻结
                changeLog.setFrozen(lastLog.getFrozen() + amount);
            }
            else {
                changeLog.setFrozen(lastLog.getFrozen());
            }

            this.outlay = this.outlay + amount;
        }
        else {
            // 收入
            changeLog.setBalance(lastLog.getBalance() + amount);
            if (withdrawAble) {// 可提现
                this.withdrawal = this.withdrawal + amount;
            }
            changeLog.setFrozen(lastLog.getFrozen());
        }

        changeLog.genBlock(lastLog.getBlock());
        this.balance = changeLog.getBalance();
        this.frozen = changeLog.getFrozen();
        this.amount = this.balance + this.frozen;
        this.block = changeLog.getBlock();
        return changeLog;
    }

    public Transaction newTransaction(@NonNull TradeDTO tradeDTO) {
        if (uid != tradeDTO.getUid() || !currency.equals(tradeDTO.getCurrency())) {
            WalletError.TRADE_NOT_ALLOWED.emit(this);
        }
        val config = WalletConfig.getCurrencyConfig(currency);

        WalletProperties.BizSubject bizSubject = config.getBizSubject(currency, tradeDTO.getBiz(),
                tradeDTO.getBizSubject());

        val outlay = bizSubject.isOutlay();
        val transaction = new Transaction();
        val current = DateUtil.current();
        transaction.setCreatedAt(current);
        transaction.setUpdatedAt(current);
        transaction.setUid(uid);
        transaction.setCurrency(currency);
        val amount = longValue(tradeDTO.getAmount());
        transaction.setAmount(amount);
        transaction.setOutlay(outlay);
        transaction.setBizId(tradeDTO.getBizId());
        transaction.setBiz(tradeDTO.getBiz());
        transaction.setBizSubject(tradeDTO.getBizSubject());
        transaction.setIp(GsvcContextHolder.getRemoteIp());

        if (outlay) {// 支出判断是否需要冻结
            transaction.setNeedFrozen(bizSubject.isNeedFrozen());
        }

        // 特别注意：提现时withdrawAble应为true。
        transaction.setWithdrawAble(bizSubject.isWithdrawAble());

        if (tradeDTO.hasExpiredAt()) {
            transaction.setExpiredAt(tradeDTO.getExpiredAt());
        }
        if (tradeDTO.hasRemark()) {
            transaction.setRemark(tradeDTO.getRemark());
        }
        return transaction;
    }

    public long longValue(double amount) {
        val pre = StringUtils.rightPad("1", getPrecision() + 1, "0");
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(Long.parseLong(pre))).toBigInteger().longValue();
    }

    public double doubleValue(long amount) {
        val pre = StringUtils.rightPad("1", getPrecision() + 1, "0");
        return BigDecimal.valueOf(amount)
            .divide(BigDecimal.valueOf(Long.parseLong(pre)), new MathContext(getPrecision(), RoundingMode.DOWN))
            .doubleValue();
    }

}
