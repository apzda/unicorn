package com.apzda.cloud.captcha.error;

import com.apzda.cloud.gsvc.IServiceError;

public record MissingCaptcha() implements IServiceError {
    @Override
    public String message() {
        return "Captcha is missing";
    }

    @Override
    public int code() {
        return 3;
    }
}
