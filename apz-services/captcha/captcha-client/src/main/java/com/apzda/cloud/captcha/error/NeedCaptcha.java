package com.apzda.cloud.captcha.error;

import com.apzda.cloud.gsvc.IServiceError;

public record NeedCaptcha() implements IServiceError {
    @Override
    public String message() {
        return "Captcha is needed";
    }

    @Override
    public int code() {
        return 4;
    }
}
