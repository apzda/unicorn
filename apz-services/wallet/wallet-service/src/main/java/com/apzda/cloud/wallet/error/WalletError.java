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
package com.apzda.cloud.wallet.error;

import com.apzda.cloud.wallet.domain.entity.Wallet;
import com.apzda.cloud.wallet.exception.BizException;
import com.apzda.cloud.wallet.exception.WalletException;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.springframework.lang.NonNull;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
public enum WalletError {

    // @formatter:off
    TRADE_NOT_ALLOWED(90399,"trade is not allowed"),
    NOTFOUND(90300,"Wallet not found"),
    LOCKED(90301, "Wallet is locked"),
    INTEGRITY_FAILED(90302, "Integrity Verification Failed"),
    BIZ_SUBJECT_NOT_FOUND(90303,"biz subject not found"),
    OUTLAY_NOT_ALLOWED(90304,"outlay is not allowed"),
    INCOME_NOT_ALLOWED(90305,"income is not allowed"),
    TRADE_CANNOT_SAVE(90306,"transaction can not be saved"),
    LOG_CANNOT_SAVE(90307,"chang log can not be saved"),
    WALLET_CANNOT_UPDATE(30908,"wallet cannot be updated"),
    INSUFFICIENT_BALANCE(90309,"balance is not enough"),
    INSUFFICIENT_BALANCE1(90310,"withdrawal is not enough"),
    EXPIRED_TIME_INVALID(90310,"expire time is invalid"),
    OUTLAY_CANNOT_SAVE(90311,"outlay log cannot be saved"),
    FROZEN_AMOUNT_INVALID(90312,"frozen amount is invalid")
    ;
    // @formatter:on

    @JsonValue
    private final int code;

    private final String message;

    WalletError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public void emit(Long uid, String currency) {
        throw new WalletException(this, uid, currency);
    }

    public void emit(@NonNull Wallet wallet) {
        throw new WalletException(this, wallet.getUid(), wallet.getCurrency());
    }

    public void emit(@NonNull Wallet wallet, Throwable e) {
        throw new WalletException(this, wallet.getUid(), wallet.getCurrency(), e);
    }

    public void emitBizError(String currency, String biz, String subject, Throwable e) {
        throw new BizException(this, biz, subject, currency, e);
    }

    public void emitBizError(String currency, String biz, String subject) {
        throw new BizException(this, biz, subject, currency);
    }

}
