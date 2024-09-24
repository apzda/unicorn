package com.apzda.cloud.captcha.storage;

import com.apzda.cloud.captcha.Captcha;
import com.apzda.cloud.captcha.TestApp;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@JsonTest
@ContextConfiguration(classes = TestApp.class)
@ImportAutoConfiguration(RedisAutoConfiguration.class)
class RedisCaptchaStorageTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void load() throws Exception {
        // given
        val captchaStorage = new RedisCaptchaStorage(stringRedisTemplate, objectMapper);
        String uuid = "123";
        val captcha = new Captcha();
        captcha.setId("12");
        // when
        val ca = captchaStorage.load(uuid, captcha);
        // then
        assertThat(ca).isNull();

        // given
        captcha.setExpireTime(System.currentTimeMillis() + Duration.ofSeconds(5).toMillis());
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
        // then
        assertThat(loaded2).isNull();
    }

}
