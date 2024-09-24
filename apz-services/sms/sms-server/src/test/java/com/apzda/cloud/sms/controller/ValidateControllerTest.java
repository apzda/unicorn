package com.apzda.cloud.sms.controller;

import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.apzda.cloud.sms.proto.SendReq;
import com.apzda.cloud.sms.proto.SmsService;
import com.apzda.cloud.sms.proto.Variable;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = "logging.level.com.apzda.cloud.sms=trace")
@ActiveProfiles({ "flyway", "sms-dev" })
@Testcontainers()
class ValidateControllerTest {

    @Autowired
    private SmsService smsService;

    private WebClient webClient;

    static {
        ResponseUtils.config();
    }

    @BeforeEach
    public void setUp() {
        webClient = WebClient.builder().baseUrl("http://localhost:38083").build();
    }

    @Test
    void validate() throws Exception {
        // given
        val req = SendReq.newBuilder()
            .addPhone("12345678901")
            .setTid("login")
            .setSync(true)
            .addVariable(Variable.newBuilder().setName("code").setValue("1234"))
            .build();
        // when
        val res = smsService.send(req);
        // then
        assertThat(res.getErrCode()).isEqualTo(0);
        // when
        val body = webClient.get()
            .uri("/sms/biz")
            .accept(MediaType.APPLICATION_JSON)
            .header("X-SMS-PHONE", "12345678901")
            .header("X-SMS-CODE", "1234")
            .retrieve()
            .bodyToMono(Response.class)
            .block();
        // then
        assertThat(body).isNotNull();
        assertThat(body.getErrCode()).isEqualTo(0);

    }

    @Test
    void validate1() throws Exception {
        // given
        val req = SendReq.newBuilder()
            .addPhone("12345678902")
            .setTid("login")
            .setSync(true)
            .addVariable(Variable.newBuilder().setName("code").setValue("1234"))
            .build();
        // when
        val res = smsService.send(req);
        // then
        assertThat(res.getErrCode()).isEqualTo(0);
        // given
        val testRequest = new ValidateController.TestRequest();
        testRequest.setPhone("12345678902");
        testRequest.setCode("1234");
        // when
        var body = webClient.post()
            .uri("/sms/bizx")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(testRequest)
            .retrieve()
            .bodyToMono(Response.class)
            .block();
        // then
        // System.out.println("body = " + body);
        assertThat(body).isNotNull();
        assertThat(body.getErrCode()).isEqualTo(0);

        // when
        val count = webClient.get()
            .uri("/sms/count")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Response.class)
            .block();
        // then
        assertThat(count).isNotNull();
        assertThat(count.getData()).isNotNull();
    }

}
