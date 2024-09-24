package cn.apzda.cloud.audit.logging;

import cn.apzda.cloud.audit.AuditApp;
import cn.apzda.cloud.audit.TestVo;
import com.apzda.cloud.audit.autoconfig.AuditAutoConfiguration;
import com.apzda.cloud.audit.logging.AuditLogger;
import com.apzda.cloud.audit.proto.AuditLog;
import com.apzda.cloud.audit.proto.AuditService;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import lombok.val;
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

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
class LoggerTest {

    @MockBean
    private AuditService auditService;

    @Autowired
    private AuditLogger logger;

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
    void log() throws InterruptedException {
        // given
        val map = new HashMap<String, String>();
        given(auditService.log(any())).willAnswer((invocation) -> {
            val argument = invocation.getArgument(0, AuditLog.class);
            map.put("message", argument.getMessage());
            return GsvcExt.CommonRes.newBuilder().build();
        });

        // when
        logger.activity("test").message("hello world").log();
        TimeUnit.MILLISECONDS.sleep(500);
        // then
        assertThat(map).isNotEmpty();
        assertThat(map).containsKeys("message");
        assertThat(map.get("message")).isEqualTo("hello world");
    }

    @Test
    void sanitize_should_work() throws InterruptedException {
        // given
        val map = new HashMap<String, String>();
        val tv = new TestVo();
        tv.setPhone("13088888888");
        given(auditService.log(any())).willAnswer((invocation) -> {
            val argument = invocation.getArgument(0, AuditLog.class);
            map.put("message", argument.getMessage());
            map.put("nv", argument.getNewJsonValue());
            return GsvcExt.CommonRes.newBuilder().build();
        });

        // when
        logger.activity("test").message("hello world").newValue(tv).log();
        TimeUnit.MILLISECONDS.sleep(500);
        // then
        assertThat(map).isNotEmpty();
        assertThat(map).containsKeys("message");
        assertThat(map.get("message")).isEqualTo("hello world");
        assertThat(map.get("nv")).isEqualTo("{\"phone\":\"130****8888\"}");
    }

}
