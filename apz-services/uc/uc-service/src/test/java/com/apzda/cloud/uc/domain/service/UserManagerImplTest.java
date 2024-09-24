package com.apzda.cloud.uc.domain.service;

import com.apzda.cloud.uc.proto.CreateAccountRequest;
import com.apzda.cloud.uc.proto.UpdateAccountRequest;
import com.apzda.cloud.uc.test.TestBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class UserManagerImplTest extends TestBase {

    @Autowired
    private UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager em;

    @Test
    void createAccount() {
        // given
        val builder = CreateAccountRequest.newBuilder();
        builder.setUsername("test");
        builder.setDisplayName("Test");
        builder.setConfirmPassword("123456");
        builder.setPassword("123456");
        builder.setResetPassword(true);
        builder.setPhone("18049920088");
        builder.setEmail("windywany@gmail.com");
        builder.addRoles("sa");
        builder.addRoles("admin");
        // when
        userManager.createAccount(builder.buildPartial());
        em.flush();
        em.clear();
        val user = userManager.getUserByUsername("test");

        // then
        assertThat(user).isNotNull();
        assertThat(passwordEncoder.matches("123456", user.getPasswd())).isTrue();

        val roles = user.allRoles(0L);
        assertThat(roles).isNotEmpty();
        assertThat(roles.size()).isEqualTo(2);

        val credentialsExpired = userManager.isCredentialsExpired(user);
        assertThat(credentialsExpired).isTrue();

        assertThatThrownBy(() -> {
            userManager.createAccount(builder.buildPartial());
        });
    }

    @Test
    void updateAccount() {
        createAccount();
        em.clear();
        // given
        val builder = UpdateAccountRequest.newBuilder();
        builder.setUsername("test");
        builder.setDisplayName("Test1");
        builder.setPhone("18049920098");
        builder.setEmail("windywany@gmail.com");
        builder.setPassword("123987");
        builder.setConfirmPassword("123987");
        builder.addRoles("sa");

        // when
        userManager.updateAccount(builder.buildPartial());
        em.flush();
        em.clear();
        val user = userManager.getUserByUsername("test");
        // then
        assertThat(user).isNotNull();
        assertThat(passwordEncoder.matches("123987", user.getPasswd())).isTrue();
        val roles = user.allRoles(0L);
        assertThat(roles).isNotEmpty();
        assertThat(roles.size()).isEqualTo(1);
        assertThat(roles.stream().toList().get(0).getRole()).isEqualTo("sa");
    }

}
