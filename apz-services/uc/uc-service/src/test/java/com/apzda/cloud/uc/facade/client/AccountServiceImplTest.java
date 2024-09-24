package com.apzda.cloud.uc.facade.client;

import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.test.TestBase;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class AccountServiceImplTest extends TestBase {

    @Autowired
    private UserManager userManager;

    @Test
    void getUserInfo() {
        // given
        String username = "admin";
        // when
        val user = userManager.getUserByUsername(username);
        // then
        assertThat(user).isNotNull();
    }

}
