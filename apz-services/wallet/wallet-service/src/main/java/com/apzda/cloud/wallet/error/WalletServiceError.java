package com.apzda.cloud.wallet.error;

import com.apzda.cloud.gsvc.IServiceError;

public record WalletServiceError(WalletError error) implements IServiceError {

    @Override
    public int code() {
        return error.getCode();
    }

    @Override
    public String message() {
        return error().getMessage();
    }
}
