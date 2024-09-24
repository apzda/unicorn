package com.apzda.cloud.captcha.facade;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import com.apzda.cloud.captcha.TestApp;
import com.apzda.cloud.captcha.proto.CaptchaService;
import com.apzda.cloud.captcha.proto.CheckReq;
import com.apzda.cloud.captcha.proto.CreateReq;
import com.apzda.cloud.captcha.proto.ValidateReq;
import com.apzda.cloud.test.autoconfig.GsvcTest;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@GsvcTest
@ContextConfiguration(classes = TestApp.class)
@ImportAutoConfiguration({ RedisAutoConfiguration.class })
class CaptchaServiceImplTest {

    @Autowired
    private CaptchaService captchaService;

    @Test
    void create() {
        try (val mocked = Mockito.mockStatic(CaptchaUtil.class)) {
            // given
            val lineCaptcha = Mockito.mock(LineCaptcha.class);
            given(lineCaptcha.getCode()).willReturn("abcd");

            mocked.when(() -> CaptchaUtil.createLineCaptcha(anyInt(), anyInt(), any(CodeGenerator.class), anyInt()))
                .thenReturn(lineCaptcha);

            val req = CreateReq.newBuilder().setUuid("1235").build();

            // when
            val res = captchaService.create(req);

            // then
            assertThat(res).isNotNull();
            assertThat(res.getErrCode()).isEqualTo(0);

            // given - validate
            val req1 = ValidateReq.newBuilder().setUuid("1235").setId(res.getId()).setCode("abcd").build();

            // when
            val validate = captchaService.validate(req1);

            // then
            assertThat(validate).isNotNull();
            assertThat(validate.getErrCode()).isEqualTo(0);

            // given - check
            val req3 = CheckReq.newBuilder().setUuid("1235").setId(res.getId()).build();
            // when
            val res3 = captchaService.check(req3);
            // then
            assertThat(res3.getErrCode()).isEqualTo(0);
        }
    }

}
