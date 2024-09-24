package com.apzda.cloud.captcha.facade;

import com.apzda.cloud.captcha.TestApp;
import com.apzda.cloud.captcha.proto.CaptchaService;
import com.apzda.cloud.captcha.proto.CreateReq;
import com.apzda.cloud.captcha.proto.ValidateReq;
import com.apzda.cloud.test.autoconfig.GsvcTest;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@GsvcTest
@ContextConfiguration(classes = TestApp.class)
@ImportAutoConfiguration({ RedisAutoConfiguration.class })
@TestPropertySource(properties = { "apzda.cloud.captcha.timeout=2s" })
class CaptchaServiceImplExpireTest {

    @Autowired
    private CaptchaService captchaService;

    @Test
    void create() throws InterruptedException {
        // given
        val req = CreateReq.newBuilder().setUuid("1234").build();

        // when
        val res = captchaService.create(req);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getErrCode()).isEqualTo(0);

        // given
        val req1 = ValidateReq.newBuilder().setUuid("1234").setId(res.getId()).build();
        TimeUnit.SECONDS.sleep(3);
        // when
        val validate = captchaService.validate(req1);

        // then
        assertThat(validate).isNotNull();
        assertThat(validate.getErrCode()).isEqualTo(2);
        assertThat(validate.getReload()).isTrue();
    }

}
