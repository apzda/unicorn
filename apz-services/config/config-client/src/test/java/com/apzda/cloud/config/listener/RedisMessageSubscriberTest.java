package com.apzda.cloud.config.listener;

import com.apzda.cloud.config.TestApplication;
import com.apzda.cloud.config.autoconfig.ConfigAutoConfiguration;
import com.apzda.cloud.config.exception.SettingUnavailableException;
import com.apzda.cloud.config.proto.ConfigService;
import com.apzda.cloud.config.proto.KeyReq;
import com.apzda.cloud.config.proto.LoadRes;
import com.apzda.cloud.config.service.SettingService;
import com.apzda.cloud.config.setting.TestSetting;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@JsonTest
@ContextConfiguration(classes = TestApplication.class)
@ImportAutoConfiguration({ RedisAutoConfiguration.class, ConfigAutoConfiguration.class })
@TestPropertySource(properties = { "logging.level.com.apzda=debug" })
@ActiveProfiles("test")
@Testcontainers(parallel = true)
class RedisMessageSubscriberTest {

    @MockBean
    private ConfigService configService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("configChangedMessageTopic")
    private ChannelTopic topic;

    @Autowired
    private SettingService settingService;

    @Test
    void onMessage() throws SettingUnavailableException, JsonProcessingException, InterruptedException {
        // given
        val testSetting = new TestSetting();
        testSetting.setName("gsvc");
        testSetting.setAge(18);

        var builder = LoadRes.newBuilder();
        builder.setErrCode(0);
        builder.setSetting(ByteString.copyFrom(objectMapper.writeValueAsBytes(testSetting)));
        given(configService.load(any(KeyReq.class))).willReturn(builder.build());
        // when
        var setting = settingService.load(TestSetting.class);
        // then
        assertThat(setting.getAge()).isEqualTo(18);
        assertThat(setting.getName()).isEqualTo("gsvc");

        // given
        testSetting.setAge(19);
        testSetting.setName("gsvc2");

        builder = LoadRes.newBuilder();
        builder.setErrCode(0);
        builder.setSetting(ByteString.copyFrom(objectMapper.writeValueAsBytes(testSetting)));
        given(configService.load(any(KeyReq.class))).willReturn(builder.build());
        // when
        stringRedisTemplate.convertAndSend(topic.getTopic(), TestSetting.class.getCanonicalName());
        TimeUnit.SECONDS.sleep(2);
        setting = settingService.load(TestSetting.class);
        // then
        assertThat(setting.getAge()).isEqualTo(19);
        assertThat(setting.getName()).isEqualTo("gsvc2");
    }

}
