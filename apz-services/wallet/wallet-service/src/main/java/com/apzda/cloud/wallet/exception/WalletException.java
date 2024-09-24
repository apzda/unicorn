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
package com.apzda.cloud.wallet.exception;

import com.apzda.cloud.gsvc.IServiceError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.wallet.error.WalletError;
import com.apzda.cloud.wallet.error.WalletServiceError;
import lombok.Getter;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
public class WalletException extends GsvcException {

    private final Long userId;

    private final String currency;

    public WalletException(IServiceError error, Long userId, String currency, Throwable e) {
        super(error, e);
        this.userId = userId;
        this.currency = currency;
    }

    public WalletException(WalletError error, Long userId, String currency, Throwable e) {
        this(new WalletServiceError(error), userId, currency, e);
    }

    public WalletException(WalletError error, Long userId, String currency) {
        this(new WalletServiceError(error), userId, currency, null);
    }

}
