package com.apzda.cloud.uc.security;

import com.apzda.cloud.gsvc.infra.ExpiredData;
import lombok.Data;
import org.springframework.lang.NonNull;

import java.time.Duration;

@Data
public class AuthTempData implements ExpiredData {

    private boolean needCaptcha;

    private int errorCnt = 0;

    @Override
    @NonNull
    public Duration getExpireTime() {
        return Duration.ofDays(365);
    }

}
