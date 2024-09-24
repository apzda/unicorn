package com.apzda.cloud.uc.service;

import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.uc.domain.repository.PrivilegeRepository;
import com.apzda.cloud.uc.domain.repository.RoleRepository;
import com.apzda.cloud.uc.domain.repository.SecurityResourceRepository;
import com.apzda.cloud.uc.properties.SecurityConfigureProperties;
import com.apzda.cloud.uc.proto.ConfigureService;
import com.apzda.cloud.uc.proto.SyncRequest;
import com.apzda.cloud.uc.test.TestBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@TestPropertySource(properties = { "apzda.ucenter.security.auto-sync=false" })
class ConfigureServiceImplTest extends TestBase {

    @Autowired
    private ConfigureService configureService;

    @Autowired
    private SecurityConfigureProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private SecurityResourceRepository securityResourceRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @Transactional
    void syncConfiguration() throws JsonProcessingException {
        // given
        val config = objectMapper.writeValueAsString(properties);
        System.out.println("config = " + config);
        val request = SyncRequest.newBuilder().setConfiguration(config).build();

        // when
        val res = configureService.syncConfiguration(request);
        entityManager.flush();
        entityManager.clear();
        // then
        thenCheck(res);

        // when
        val res1 = configureService.syncConfiguration(request);
        entityManager.flush();
        entityManager.clear();
        // then
        thenCheck(res1);

        // delete test
        // delete unit-test role
        properties.getRoles().get("unit-test").setDeleted(true);
        // delete privilege
        properties.getResources().get("test").getPrivileges().get("*:test.*").setDeleted(true);
        // delete resource
        properties.getResources().get("test").getChildren().get("child1").setDeleted(true);
        val child21 = new SecurityConfigureProperties.ResourceNode();
        child21.setName("Child 2-1");
        properties.getResources().get("test").getChildren().get("child2").getChildren().put("child2-1", child21);
        val config2 = objectMapper.writeValueAsString(properties);
        System.out.println("config2 = " + config2);
        val request2 = SyncRequest.newBuilder().setConfiguration(config2).build();

        // when
        val res2 = configureService.syncConfiguration(request2);
        entityManager.flush();
        entityManager.clear();

        thenCheck2(res2);
    }

    void thenCheck(GsvcExt.CommonRes res) {
        // then
        assertThat(res.getErrCode()).isEqualTo(0);

        // check resource - test
        val resource = securityResourceRepository.findByRid("test");
        assertThat(resource).isPresent();
        assertThat(resource.get().getRid()).isEqualTo("test");
        assertThat(resource.get().getName()).isEqualTo("Unit Test");
        val children = resource.get().getChildren();
        assertThat(children).isNotEmpty();
        assertThat(children.size()).isEqualTo(2); // child1 and child2
        // check child1
        val child1 = children.stream().filter((r) -> r.getRid().equals("child1")).findFirst();
        assertThat(child1).isPresent();
        assertThat(child1.get().getRid()).isEqualTo("child1");
        assertThat(child1.get().getName()).isEqualTo("Child 1");
        val childrenOfChild1 = child1.get().getChildren();
        assertThat(childrenOfChild1).isNotEmpty();
        assertThat(childrenOfChild1.size()).isEqualTo(1); // child1-1
        val child11 = childrenOfChild1.get(0);
        assertThat(child11.getRid()).isEqualTo("child1-1");
        assertThat(child11.getName()).isEqualTo("Child 1 -1");
        // check child2
        val child2 = children.stream().filter((r) -> r.getRid().equals("child2")).findFirst();
        assertThat(child2).isPresent();
        assertThat(child2.get().getRid()).isEqualTo("child2");
        assertThat(child2.get().getName()).isEqualTo("Child 2");
        val childrenOfChild2 = child2.get().getChildren();
        assertThat(childrenOfChild2).isEmpty();

        // check resource - test
        val test1 = securityResourceRepository.findByRid("test1");
        assertThat(test1).isPresent();
        assertThat(test1.get().getRid()).isEqualTo("test1");
        assertThat(test1.get().getName()).isEqualTo("Test1");
        val children1 = test1.get().getChildren();
        assertThat(children1).isEmpty();

        // check roles
        val role = roleRepository.findByRole("unit-test");
        assertThat(role).isPresent();
        assertThat(role.get().getName()).isEqualTo("Test Role");
        // check permission
        val permission = privilegeRepository.findByPermission("RUNAS");
        assertThat(permission).isPresent();
        assertThat(permission.get().getBuiltin()).isTrue();
        assertThat(permission.get().getType()).isEqualTo("authority");

        val p1 = privilegeRepository.findByPermission("*:test.*");
        assertThat(p1).isPresent();
        assertThat(p1.get().getBuiltin()).isTrue();
        assertThat(p1.get().getType()).isEqualTo("resource");
        val p2 = privilegeRepository.findByPermission("r:test.*");
        assertThat(p2).isPresent();
        assertThat(p2.get().getBuiltin()).isTrue();
        val p3 = privilegeRepository.findByPermission("r:user.*");
        assertThat(p3).isPresent();
        assertThat(p3.get().getBuiltin()).isTrue();

        val user = securityResourceRepository.findByRid("user");
        assertThat(user).isPresent();
        assertThat(user.get().getExplorer()).isEqualTo("com.apzda.cloud.uc.resource.UserIdExplorer");

        entityManager.clear();
    }

    void thenCheck2(GsvcExt.CommonRes res) {
        // then
        assertThat(res.getErrCode()).isEqualTo(0);

        // check resource - test
        val resource = securityResourceRepository.findByRid("test");
        assertThat(resource).isPresent();
        assertThat(resource.get().getRid()).isEqualTo("test");
        assertThat(resource.get().getName()).isEqualTo("Unit Test");
        assertThat(resource.get().isDeleted()).isFalse();
        val children = resource.get().getChildren();
        assertThat(children).isNotEmpty();
        assertThat(children.size()).isEqualTo(1); // child2
        // check child1
        val child1 = children.stream().filter((r) -> r.getRid().equals("child1")).findFirst();
        assertThat(child1).isNotPresent();
        // check child2
        val child2 = children.stream().filter((r) -> r.getRid().equals("child2")).findFirst();
        assertThat(child2).isPresent();
        assertThat(child2.get().getRid()).isEqualTo("child2");
        assertThat(child2.get().getName()).isEqualTo("Child 2");
        val childrenOfChild2 = child2.get().getChildren();
        assertThat(childrenOfChild2).isNotEmpty();
        assertThat(childrenOfChild2.size()).isEqualTo(1); // child2-1

        // check resource - test
        val test1 = securityResourceRepository.findByRid("test1");
        assertThat(test1).isPresent();
        assertThat(test1.get().getRid()).isEqualTo("test1");
        assertThat(test1.get().getName()).isEqualTo("Test1");
        val children1 = test1.get().getChildren();
        assertThat(children1).isEmpty();

        // check roles
        val role = roleRepository.findByRole("unit-test");
        assertThat(role).isNotPresent();
        // check permission
        val permission = privilegeRepository.findByPermission("RUNAS");
        assertThat(permission).isPresent();

        val p1 = privilegeRepository.findByPermission("*:test.*");
        assertThat(p1).isNotPresent();
        val p2 = privilegeRepository.findByPermission("r:test.*");
        assertThat(p2).isPresent();
        val p3 = privilegeRepository.findByPermission("r:user.*");
        assertThat(p3).isPresent();
    }

}
