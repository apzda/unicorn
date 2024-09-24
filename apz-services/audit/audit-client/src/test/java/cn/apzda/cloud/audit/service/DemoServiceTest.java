package cn.apzda.cloud.audit.service;

import cn.apzda.cloud.audit.AuditApp;
import com.apzda.cloud.audit.autoconfig.AuditAutoConfiguration;
import com.apzda.cloud.audit.proto.AuditService;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@SpringBootTest
@ContextConfiguration(classes = AuditApp.class)
@ImportAutoConfiguration({ AuditAutoConfiguration.class, AopAutoConfiguration.class, SecurityAutoConfiguration.class,
        ObservationAutoConfiguration.class })
@ComponentScan(basePackages = { "cn.apzda.cloud.audit" })
@TestPropertySource(properties = { "logging.level.com.apzda.cloud=trace" })
class DemoServiceTest {

    @MockBean
    private AuditService auditService;

    @Autowired
    private DemoService demoService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        ResponseUtils.config();
    }

    @BeforeEach
    void setupMapper() {
        objectMapper.registerModule(new ProtobufModule());
    }

    @Test
    void publishEventTest() throws InterruptedException {
        // given
        when(auditService.log(any())).thenReturn(GsvcExt.CommonRes.newBuilder().setErrCode(0).build());
        // when
        demoService.publishEvent();
        TimeUnit.SECONDS.sleep(1);
        // then
        verify(auditService, times(1)).log(any());
    }

}
