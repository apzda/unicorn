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
package com.apzda.cloud.wallet.domain.service;

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.wallet.config.WalletConfig;
import com.apzda.cloud.wallet.domain.entity.ChangeLog;
import com.apzda.cloud.wallet.domain.entity.Transaction;
import com.apzda.cloud.wallet.domain.entity.Wallet;
import com.apzda.cloud.wallet.domain.mapper.WalletMapper;
import com.apzda.cloud.wallet.error.WalletError;
import com.apzda.cloud.wallet.proto.TradeDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService extends ServiceImpl<WalletMapper, Wallet> {

    private final TransactionService transactionService;

    private final ChangeLogService changeLogService;

    private final OutlayService outlayService;

    @Transactional(rollbackFor = Exception.class)
    public synchronized Wallet openWallet(@NonNull Long uid, @NonNull String currency) {
        WalletConfig.getCurrencyConfig(currency);

        var wallet = baseMapper.openWallet(uid, currency);

        if (wallet == null) {
            wallet = new Wallet();
            val current = DateUtil.current();
            wallet.setCreatedAt(current);
            wallet.setUpdatedAt(current);
            wallet.setCreatedBy(String.valueOf(uid));
            wallet.setUpdatedBy(wallet.getCreatedBy());
            wallet.setUid(uid);
            wallet.setCurrency(currency);
            try {
                if (!save(wallet)) {
                    throw new IllegalStateException(
                            "Cannot open wallet for user(uid: " + uid + ", currency: " + currency + ")");
                }
                wallet = baseMapper.openWallet(uid, currency);
                val changeLog = ChangeLog.init(wallet);
                wallet.setBlock(changeLog.getBlock());

                if (!updateById(wallet)) {
                    throw new IllegalStateException(
                            "Cannot update wallet block for user(uid: " + uid + ", currency: " + currency + ")");
                }

                if (!changeLogService.save(changeLog)) {
                    throw new IllegalStateException(
                            "Cannot init wallet for user(uid: " + uid + ", currency: " + currency + ")");
                }
            }
            catch (DuplicateKeyException de) {
                log.warn("Parallel open wallet of user(uid: {}, currency: {}), try open it again", uid, currency);
                wallet = baseMapper.openWallet(uid, currency);
            }
        }

        if (wallet == null) {
            WalletError.NOTFOUND.emit(uid, currency);
        }
        if (wallet.isLocked()) {
            log.error("Wallet(uid: {}, currency: {}) is locked!", uid, currency);
            WalletError.LOCKED.emit(wallet);
        }
        return wallet;
    }

    @Transactional(rollbackFor = Exception.class)
    @Valid
    public Transaction trade(TradeDTO tradeDTO) {
        val uid = tradeDTO.getUid();
        val currency = tradeDTO.getCurrency();
        // 打开用户钱包
        val wallet = openWallet(uid, currency);
        val transaction = wallet.newTransaction(tradeDTO);

        val lastLog = getLastLog(wallet);

        checkIntegrity(lastLog, wallet);

        // 钱包未开启过期机制时将交易的过期时间置为null。
        if (!wallet.isExpireAble()) {
            transaction.setExpiredAt(null);
        }
        else if (transaction.getExpiredAt() == null
                || DateUtil.date(transaction.getExpiredAt()).isBefore(DateUtil.date())) {
            WalletError.EXPIRED_TIME_INVALID.emit(wallet);
        }
        // 保存交易记录
        if (!transactionService.save(transaction)) {
            WalletError.TRADE_CANNOT_SAVE.emit(wallet);
        }

        //
        if (wallet.isExpireAble()) {
            if (transaction.isOutlay()) {
                outlayService.outlay(transaction);
            }
            else {
                outlayService.newIncome(transaction);
            }
        }

        // 生成交易日志
        ChangeLog changeLog = wallet.newChangeLog(transaction, lastLog);

        if (!changeLogService.save(changeLog)) {
            WalletError.LOG_CANNOT_SAVE.emit(wallet);
        }
        // 更新账户
        if (!updateById(wallet)) {
            WalletError.WALLET_CANNOT_UPDATE.emit(wallet);
        }

        return transaction;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(Long transactionId) {
        // 用于确认冻结
        val trans = transactionService.getById(transactionId);
        if (trans == null || !trans.isOutlay() || !trans.isNeedFrozen()) {
            return false;
        }
        val uid = trans.getUid();
        val currency = trans.getCurrency();
        val wallet = openWallet(uid, currency);

        val lastLog = getLastLog(wallet);

        checkIntegrity(lastLog, wallet);

        val amount = trans.getAmount();
        val frozen = wallet.getFrozen();
        if (amount > frozen || !frozen.equals(Objects.requireNonNull(lastLog).getFrozen())) {
            WalletError.FROZEN_AMOUNT_INVALID.emit(wallet);
        }

        val changeLog = new ChangeLog();
        changeLog.setTransactionId(transactionId);
        changeLog.setUid(uid);
        changeLog.setCurrency(currency);
        changeLog.setBiz("system");
        changeLog.setBizSubject("confirm");
        changeLog.setBizId(transactionId.toString());
        changeLog.setAmount(amount);
        changeLog.setPreBalance(wallet.getBalance());
        changeLog.setBalance(wallet.getBalance());
        changeLog.setPreFrozen(frozen);
        changeLog.setFrozen(frozen - amount);
        changeLog.setParentId(lastLog.getId());
        changeLog.setIp(GsvcContextHolder.getRemoteIp());
        // changeLog.setRemark("confirm frozen amount");
        changeLog.genBlock(wallet.getBlock());

        // 扣减冻结金额
        wallet.setFrozen(changeLog.getFrozen());
        // 重新计算总余额
        wallet.setAmount(wallet.getBalance() + wallet.getFrozen());
        // 更新区块
        wallet.setBlock(changeLog.getBlock());

        if (!updateById(wallet)) {
            WalletError.WALLET_CANNOT_UPDATE.emit(wallet);
        }

        if (!changeLogService.save(changeLog)) {
            WalletError.LOG_CANNOT_SAVE.emit(wallet);
        }

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean unfreeze(Long transactionId) {
        // 用于返还冻结金额
        val trans = transactionService.getById(transactionId);
        if (trans == null || !trans.isOutlay() || !trans.isNeedFrozen()) {
            return false;
        }
        val uid = trans.getUid();
        val currency = trans.getCurrency();
        val wallet = openWallet(uid, currency);
        val lastLog = getLastLog(wallet);
        checkIntegrity(lastLog, wallet);

        val amount = trans.getAmount();
        val frozen = wallet.getFrozen();
        if (amount > frozen || !frozen.equals(Objects.requireNonNull(lastLog).getFrozen())) {
            WalletError.FROZEN_AMOUNT_INVALID.emit(wallet);
        }
        // 这里要弄一个交易（系统交易 - ）
        val transaction = new Transaction();
        transaction.setUid(uid);
        transaction.setCurrency(currency);
        transaction.setAmount(trans.getAmount());
        transaction.setWithdrawAble(trans.isWithdrawAble());
        transaction.setIp(GsvcContextHolder.getRemoteIp());
        transaction.setBiz("system");
        transaction.setBizSubject("unfreeze");
        transaction.setBizId(transactionId.toString());
        if (!transactionService.save(transaction)) {
            WalletError.TRADE_CANNOT_SAVE.emit(wallet);
        }
        // 记录日志
        val changeLog = new ChangeLog();
        changeLog.setTransactionId(transaction.getId());
        changeLog.setUid(uid);
        changeLog.setCurrency(currency);
        changeLog.setBiz("system");
        changeLog.setBizSubject("unfreeze");
        changeLog.setBizId(transactionId.toString());
        changeLog.setAmount(amount);
        changeLog.setPreBalance(lastLog.getBalance());
        changeLog.setBalance(lastLog.getBalance() + amount);
        changeLog.setPreFrozen(frozen);
        changeLog.setFrozen(frozen - amount);
        changeLog.setParentId(lastLog.getId());
        changeLog.setIp(GsvcContextHolder.getRemoteIp());
        changeLog.genBlock(wallet.getBlock());
        if (!changeLogService.save(changeLog)) {
            WalletError.LOG_CANNOT_SAVE.emit(wallet);
        }

        wallet.setBlock(changeLog.getBlock());
        wallet.setBalance(changeLog.getBalance());
        wallet.setFrozen(changeLog.getFrozen());
        wallet.setAmount(changeLog.getBalance() + changeLog.getFrozen());
        if (trans.isWithdrawAble()) {
            wallet.setWithdrawal(wallet.getWithdrawal() + amount);
        }
        wallet.setOutlay(wallet.getOutlay() - amount);
        if (!updateById(wallet)) {
            WalletError.WALLET_CANNOT_UPDATE.emit(wallet);
        }

        return true;
    }

    @Nullable
    public ChangeLog getLastLog(@NonNull Wallet wallet) {
        return changeLogService.getLastLog(wallet.getUid(), wallet.getCurrency());
    }

    @Nullable
    public ChangeLog getLastLog(@NonNull long uid, @NonNull String currency) {
        return changeLogService.getLastLog(uid, currency);
    }

    public static void checkIntegrity(ChangeLog lastLog, Wallet wallet) {
        val uid = wallet.getUid();
        val currency = wallet.getCurrency();

        // 完整性校验
        if (lastLog == null) {
            log.error("Wallet(uid: {}, currency: {}) change log not found!", uid, currency);
            WalletError.INTEGRITY_FAILED.emit(wallet);
        }

        if (!lastLog.getBlock().equals(wallet.getBlock())) {
            log.error("Wallet(uid: {}, currency: {}) integrity verification failed: log block({}) != wallet block({})",
                    uid, currency, lastLog.getBlock(), wallet.getBlock());
            WalletError.INTEGRITY_FAILED.emit(wallet);
        }

        val balance = lastLog.getBalance();
        val balance1 = wallet.getBalance();
        if (!balance.equals(balance1)) {
            log.error(
                    "Wallet(uid: {}, currency: {}) integrity verification failed: log balance({}) != wallet balance({})",
                    uid, currency, lastLog.getBalance(), wallet.getBalance());
            WalletError.INTEGRITY_FAILED.emit(wallet);
        }

        if (!lastLog.getFrozen().equals(wallet.getFrozen())) {
            log.error(
                    "Wallet(uid: {}, currency: {}) integrity verification failed: log frozen({}) != wallet frozen({})",
                    uid, currency, lastLog.getFrozen(), wallet.getFrozen());
            WalletError.INTEGRITY_FAILED.emit(wallet);
        }

        if (!wallet.getAmount().equals(wallet.getBalance() + wallet.getFrozen())) {
            log.error(
                    "Wallet(uid: {}, currency: {}) integrity verification failed: amount{}) != balance({})+frozen({})",
                    uid, currency, wallet.getAmount(), wallet.getBalance(), wallet.getFrozen());
        }
    }

}
