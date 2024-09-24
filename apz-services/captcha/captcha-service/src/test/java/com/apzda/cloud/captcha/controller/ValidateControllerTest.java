package com.apzda.cloud.captcha.controller;

import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = "logging.level.com.apzda.cloud.captcha=trace")
class ValidateControllerTest {

    private WebClient webClient;

    static {
        ResponseUtils.config();
    }

    @BeforeEach
    public void setUp() {
        webClient = WebClient.builder().baseUrl("http://localhost:8080").build();
    }

    @Test
    void validate() throws Exception {
        // when
        assertThatThrownBy(() -> {
            val body = webClient.get()
                .uri("/captcha/biz")
                .accept(MediaType.APPLICATION_JSON)
                .header("X-CAPTCHA-UUID", "1234")
                .header("X-CAPTCHA-ID", "1")
                .retrieve()
                .bodyToMono(Response.class)
                .block();
            // then
            assertThat(body).isNotNull();
            assertThat(body.getErrCode()).isEqualTo(1);
        }).isInstanceOf(WebClientResponseException.ServiceUnavailable.class);

    }

    @Test
    void validate1() throws Exception {
        // given
        val testRequest = new ValidateController.TestRequest();
        testRequest.setCaptchaId("1");
        testRequest.setCaptchaUuid("1234");
        // when
        assertThatThrownBy(() -> {
            val body = webClient.post()
                .uri("/captcha/bizx")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(testRequest)
                .retrieve()
                .bodyToMono(Response.class)
                .block();
            // then
            assertThat(body).isNotNull();
            assertThat(body.getErrCode()).isEqualTo(1);
        }).isInstanceOf(WebClientResponseException.ServiceUnavailable.class);

    }

}
