package com.apzda.cloud.captcha.storage;

import com.apzda.cloud.captcha.Captcha;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class LocalCaptchaStorageTest {

    @Test
    void local_storage_should_be_work_ok() throws Exception {
        // given
        val captchaStorage = new LocalCaptchaStorage(Duration.ofSeconds(10));
        String uuid = "123";
        val captcha = new Captcha();
        captcha.setId("12");
        // when
        val ca = captchaStorage.load(uuid, captcha);
        // then
        assertThat(ca).isNull();

        // given
        captcha.setExpireTime(5);
        captcha.setCode("7890");
        captchaStorage.save(uuid, captcha);

        // when
        val loaded = captchaStorage.load(uuid, captcha);
        // then
        assertThat(loaded).isNotNull();
        assertThat(loaded.getCode()).isEqualTo("7890");

        // when
        captchaStorage.remove(uuid, captcha);
        val loaded2 = captchaStorage.load(uuid, captcha);
        assertThat(loaded2).isNull();
    }

}
