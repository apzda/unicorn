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
import com.apzda.cloud.wallet.domain.entity.Outlay;
import com.apzda.cloud.wallet.domain.entity.Transaction;
import com.apzda.cloud.wallet.domain.mapper.OutlayMapper;
import com.apzda.cloud.wallet.error.WalletError;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Slf4j
public class OutlayService extends ServiceImpl<OutlayMapper, Outlay> {

    @Transactional
    public void newIncome(Transaction transaction) {
        val outlay = new Outlay();
        outlay.setUid(transaction.getUid());
        outlay.setCurrency(transaction.getCurrency());
        outlay.setOutlayTransactionId(0L);
        outlay.setTransactionId(transaction.getId());
        outlay.setAmount(0L);
        outlay.setUseAmount(0L);
        outlay.setIncome(transaction.getAmount());
        outlay.setBalance(transaction.getAmount());
        outlay.setMargin(0L);
        outlay.setExpiredAt(transaction.getExpiredAt());

        if (!save(outlay)) {
            WalletError.OUTLAY_CANNOT_SAVE.emit(transaction.getUid(), transaction.getCurrency());
        }
    }

    @Transactional
    public void outlay(Transaction transaction) {
        var amount = transaction.getAmount();
        val uid = transaction.getUid();
        val currency = transaction.getCurrency();
        val outs = new ArrayList<Outlay>();
        val newOutlays = new ArrayList<Outlay>();

        while (amount > 0) {
            outs.clear();
            val incomes = availableTransactions(uid, currency);

            if (incomes.isEmpty()) {
                log.error("支出: {}, 余额不足, 差额: {}", transaction.getId(), amount);
                WalletError.INSUFFICIENT_BALANCE.emit(uid, currency);
            }

            for (Outlay income : incomes) {
                val out = new Outlay();
                val balance = income.getBalance();

                out.setUid(uid);
                out.setCurrency(currency);
                out.setTransactionId(income.getTransactionId());
                out.setOutlayTransactionId(transaction.getId());
                out.setIncome(0L);
                out.setBalance(0L);
                out.setAmount(transaction.getAmount());
                out.setExpiredAt(income.getExpiredAt());

                outs.add(income);
                newOutlays.add(out);

                if (balance >= amount) {
                    income.setBalance(income.getBalance() - amount);
                    out.setUseAmount(amount);
                    out.setMargin(0L);
                    amount = 0L;
                    break;
                }
                else {
                    out.setUseAmount(income.getBalance());
                    amount = amount - income.getBalance();
                    out.setMargin(amount);
                    income.setBalance(0L);
                }
            }

            if ((amount > 0 && incomes.size() <= 10)) {
                log.error("支出: {}, 余额不足, 差额: {}", transaction.getId(), amount);
                WalletError.INSUFFICIENT_BALANCE.emit(uid, currency);
            }

            if (!updateBatchById(outs)) {
                WalletError.LOG_CANNOT_SAVE.emit(uid, currency);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("支出:{}, 一共使用{}条收入: {}", transaction.getId(), newOutlays.size(),
                    newOutlays.stream().map(Outlay::getTransactionId).collect(Collectors.toList()));
        }

        if (!newOutlays.isEmpty() && !saveBatch(newOutlays)) {
            WalletError.LOG_CANNOT_SAVE.emit(uid, currency);
        }
    }

    public List<Outlay> availableTransactions(long uid, String currency) {
        val con = Wrappers.lambdaQuery(Outlay.class);
        con.eq(Outlay::getUid, uid);
        con.eq(Outlay::getCurrency, currency);
        con.gt(Outlay::getBalance, 0);
        con.ge(Outlay::getExpiredAt, DateUtil.current());
        con.orderByAsc(Outlay::getExpiredAt);
        con.last("LIMIT 10");

        return list(con);
    }

    public List<Outlay> listByOutlayTransactionId(long transactionId) {
        val con = Wrappers.lambdaQuery(Outlay.class);
        con.eq(Outlay::getOutlayTransactionId, transactionId);
        con.orderByAsc(Outlay::getExpiredAt);
        return list(con);
    }

    public List<Outlay> listByTransactionId(long transactionId) {
        val con = Wrappers.lambdaQuery(Outlay.class);
        con.eq(Outlay::getTransactionId, transactionId);
        return list(con);
    }

}
