package com.apzda.cloud.captcha.provider;

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.captcha.Captcha;
import com.apzda.cloud.captcha.ValidateStatus;
import com.apzda.cloud.captcha.storage.CaptchaStorage;
import com.apzda.cloud.gsvc.config.Props;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.HashMap;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class SliderCaptchaProviderTest {

    @Test
    void create() throws Exception {
        // given
        val captchaStorage = Mockito.mock(CaptchaStorage.class);
        val sp = new SliderCaptchaProvider();

        // when
        sp.init(captchaStorage, new Props(new HashMap<>() {
            {
                put("watermark", "GSVC");
            }
        }));
        val captcha = sp.create("12345", 0, 0, Duration.ofSeconds(7200));

        // then
        assertThat(captcha).isNotNull();
        val code = captcha.getCode();
        assertThat(code).contains("&&");
        assertThat(code).containsPattern(Pattern.compile(".+&&\\d+$"));
    }

    @Test
    void validate() throws Exception {
        // given
        val captcha = new Captcha();
        captcha.setId("1");
        captcha.setCode("56");
        captcha.setExpireTime(DateUtil.currentSeconds() + Duration.ofMinutes(2).toSeconds());

        val captchaStorage = Mockito.mock(CaptchaStorage.class);
        given(captchaStorage.load(any(String.class), any(Captcha.class))).willReturn(captcha);

        val sp = new SliderCaptchaProvider();

        // when
        sp.init(captchaStorage, new Props(new HashMap<>() {
            {
                put("tolerant", "5");
            }
        }));
        val validate = sp.validate("12345", "1", "50,1", false);
        // then
        assertThat(validate).isEqualTo(ValidateStatus.ERROR);

        // when
        val validate1 = sp.validate("12345", "1", "72,1", false);
        // then
        assertThat(validate1).isEqualTo(ValidateStatus.ERROR);

        // when
        val validate2 = sp.validate("12345", "1", "53,1", false);
        // then
        assertThat(validate2).isEqualTo(ValidateStatus.OK);

        // when
        val validate3 = sp.validate("12345", "1", "61,1", false);
        // then
        assertThat(validate3).isEqualTo(ValidateStatus.OK);
    }

}
