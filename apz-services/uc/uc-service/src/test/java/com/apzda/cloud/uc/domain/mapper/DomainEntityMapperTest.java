package com.apzda.cloud.uc.domain.mapper;

import com.apzda.cloud.uc.domain.entity.User;
import com.apzda.cloud.uc.domain.entity.UserMeta;
import com.apzda.cloud.uc.domain.vo.MetaType;
import com.apzda.cloud.uc.proto.MetaValueType;
import com.apzda.cloud.uc.test.TestBase;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class DomainEntityMapperTest extends TestBase {

    @Autowired
    private DomainEntityMapper domainEntityMapper;

    @Test
    void userInfoAndUserMapperShouldOk() {
        // given
        val um1 = new UserMeta();
        um1.setName("m1");
        um1.setValue("1");
        um1.setType(MetaType.S);
        val user = Mockito.spy(new User());
        when(user.getMetas()).thenReturn(List.of(um1));
        user.setUsername("123");
        user.setPasswd("2222");

        // when
        val userInfo = domainEntityMapper.fromUserEntity(user);

        // then
        assertThat(userInfo.getUid()).isEqualTo("123");
        assertThat(userInfo.getMetaList()).isNotEmpty();
        assertThat(userInfo.getMetaList().get(0).getType()).isEqualTo(MetaValueType.STRING);

        // when
        val u = domainEntityMapper.fromUserInfo(userInfo);
        // then
        assertThat(u.getUsername()).isEqualTo("123");
        assertThat(u.getPasswd()).isEqualTo("2222");
        assertThat(u.getMetas()).isNull();
    }

}
