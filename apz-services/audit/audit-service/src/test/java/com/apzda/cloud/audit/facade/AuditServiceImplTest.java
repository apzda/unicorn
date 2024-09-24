package com.apzda.cloud.audit.facade;

import com.apzda.cloud.audit.TestConfig;
import com.apzda.cloud.audit.logging.AuditLogger;
import com.apzda.cloud.audit.proto.Arg;
import com.apzda.cloud.audit.proto.AuditLog;
import com.apzda.cloud.audit.proto.AuditService;
import com.apzda.cloud.audit.proto.Query;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = TestConfig.class)
@ActiveProfiles({ "test" })
@Testcontainers(parallel = true)
class AuditServiceImplTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogger logger;

    @Test
    @Rollback
    void log() {
        // given
        val builder = AuditLog.newBuilder();
        builder.setTimestamp(System.currentTimeMillis());
        builder.setUserid("1");
        builder.setActivity("test");
        builder.setIp("127.0.0.1");
        builder.setMessage("hello world!");
        // when
        val rest = auditService.log(builder.build());
        // then
        assertThat(rest.getErrCode()).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "admin", authorities = { "r:auditlog" })
    void logs() throws InterruptedException {
        // when
        val req = Query.newBuilder()
            .setPager(GsvcExt.Pager.newBuilder()
                .setPageNumber(0)
                .setPageSize(1)
                .setSort(GsvcExt.Sorter.newBuilder()
                    .addOrder(GsvcExt.Sorter.Order.newBuilder()
                        .setField("logTime")
                        .setDirection(GsvcExt.Sorter.Direction.DESC))
                    .build())
                .build())
            .build();

        logger.activity("test")
            .message("hello world")
            .arg(Arg.newBuilder().setIndex(0).setValue("1"))
            .arg(Arg.newBuilder().setIndex(1).setValue("2"))
            .replace(new String[] { "1" }, new String[] { "2" })
            .log();
        TimeUnit.MILLISECONDS.sleep(500);
        val logs = auditService.logs(req);
        // then
        assertThat(logs.getErrCode()).isEqualTo(0);
        assertThat(logs.getLogCount()).isEqualTo(1);
        val log = logs.getLog(0);
        assertThat(log.getArgCount()).isEqualTo(2);
        assertThat(log.getOldJsonValue()).isEqualTo("[\"1\"]");
        assertThat(log.getNewJsonValue()).isEqualTo("[\"2\"]");
        assertThat(log.getRunas()).isEqualTo("123");
        assertThat(log.getDevice()).isEqualTo("test");
    }

}
