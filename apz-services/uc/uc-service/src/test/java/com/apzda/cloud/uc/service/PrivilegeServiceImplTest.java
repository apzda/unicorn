package com.apzda.cloud.uc.service;

import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.uc.proto.*;
import com.apzda.cloud.uc.test.TestBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Transactional
@WithMockUser(authorities = { "*:privilege" })
class PrivilegeServiceImplTest extends TestBase {

    @Autowired
    private PrivilegeService privilegeService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void crud_api_should_be_ok() {
        // given
        val builder = PrivilegeDto.newBuilder();
        builder.setName("Test1");
        builder.setType("authority");
        builder.setDescription("Test1 Authority Description");
        builder.setPermission("TEST1");
        // when
        val res1 = privilegeService.create(builder.buildPartial());
        entityManager.flush();
        entityManager.clear();
        // then
        assertThat(res1.getErrCode()).isEqualTo(0);

        // given query
        val q = PrivilegeQuery.newBuilder().setQuery("Test1").buildPartial();
        // when
        val res2 = privilegeService.query(q);
        entityManager.clear();
        // then
        assertThat(res2.getErrCode()).isEqualTo(0);
        assertThat(res2.getTotalRecord()).isEqualTo(1);
        assertThat(res2.getResults(0).getId()).isNotBlank();
        assertThat(res2.getResults(0).getType()).isEqualTo("authority");
        assertThat(res2.getResults(0).getName()).isEqualTo("Test1");
        assertThat(res2.getResults(0).getPermission()).isEqualTo("TEST1");
        assertThat(res2.getResults(0).getDescription()).isEqualTo("Test1 Authority Description");

        // given - update
        val b2 = PrivilegeDto.newBuilder();
        b2.setId(res2.getResults(0).getId());
        b2.setName("Test2");
        b2.setType("resource");
        b2.setDescription("Test2 Authority Description");
        b2.setPermission("TEST2");

        // when
        val res3 = privilegeService.update(b2.buildPartial());
        entityManager.flush();
        entityManager.clear();
        // then
        assertThat(res3.getErrCode()).isEqualTo(0);

        // given query
        val q2 = PrivilegeQuery.newBuilder().setQuery("Test2").buildPartial();
        // when
        val res4 = privilegeService.query(q2);
        // then
        assertThat(res4.getErrCode()).isEqualTo(0);
        assertThat(res4.getTotalRecord()).isEqualTo(1);
        assertThat(res4.getResults(0).getId()).isNotBlank();
        assertThat(res4.getResults(0).getType()).isEqualTo("authority");
        assertThat(res4.getResults(0).getName()).isEqualTo("Test2");
        assertThat(res4.getResults(0).getPermission()).isEqualTo("TEST2");
        assertThat(res4.getResults(0).getDescription()).isEqualTo("Test2 Authority Description");

        // given - delete
        val d1 = PrivilegeId.newBuilder().setId(res4.getResults(0).getId()).build();
        // when
        val res5 = privilegeService.delete(d1);
        // then
        assertThat(res5.getErrCode()).isEqualTo(0);
        entityManager.flush();
        entityManager.clear();
        // given query
        val q3 = PrivilegeQuery.newBuilder().setQuery("Test2").buildPartial();
        // when
        val res6 = privilegeService.query(q3);
        entityManager.clear();
        // then
        assertThat(res6.getErrCode()).isEqualTo(0);
        assertThat(res6.getTotalRecord()).isEqualTo(0);

        assertThatThrownBy(() -> {
            // given - delete
            val d2 = PrivilegeId.newBuilder().setId("1").build();
            // when
            val res7 = privilegeService.delete(d2);
        }).isInstanceOf(GsvcException.class);
    }

    @Test
    void resource_api_should_be_ok() {
        // given
        val builder = ResourceReq.newBuilder().setPid(0);

        // when
        val resource = privilegeService.resource(builder.build());

        // then
        assertThat(resource.getErrCode()).isEqualTo(0);
        assertThat(resource.getResourceCount()).isGreaterThanOrEqualTo(3);
        val ids = resource.getResourceList().stream().map(ResourceVo::getId);
        assertThat(ids).contains("user", "role", "privilege");
    }

}
