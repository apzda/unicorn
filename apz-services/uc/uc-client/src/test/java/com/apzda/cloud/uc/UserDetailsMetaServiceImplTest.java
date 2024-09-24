package com.apzda.cloud.uc;

import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.uc.proto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@JsonTest
@ActiveProfiles("test")
@TestPropertySource(properties = { "apzda.ucenter.security.auto-sync=false" })
class UserDetailsMetaServiceImplTest {

    @MockBean
    private UcenterService ucenterService;

    @MockBean
    private IServiceCaller serviceCaller;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void get_object_meta_should_be_ok() throws JsonProcessingException {
        // given
        val cu = CurrentUser.builder().uid("test").build();

        val user = User.withUsername("test").password("123").authorities("aaa").build();

        val builder = UserMetaResp.newBuilder();
        builder.setErrCode(0)
            .addMeta(UserMeta.newBuilder()
                .setType(MetaValueType.OBJECT)
                .setName("whatever")
                .setValue(objectMapper.writeValueAsString(cu))
                .build());
        given(ucenterService.getMetas(any(Request.class))).willReturn(builder.build());

        val userDetailsMetaService = new UserDetailsMetaServiceImpl(ucenterService, objectMapper);

        // when
        val value = userDetailsMetaService.getMetaData(user, "whatever", CurrentUser.class);

        // then
        assertThat(value.isPresent()).isTrue();
        assertThat(value.get().getUid()).isEqualTo(cu.getUid());
    }

    @Test
    void get_string_meta_should_be_ok() {
        // given
        val user = User.withUsername("test").password("123").authorities("aaa").build();

        val builder = UserMetaResp.newBuilder();
        builder.setErrCode(0)
            .addMeta(
                    UserMeta.newBuilder().setType(MetaValueType.STRING).setName("whatever").setValue("string").build());
        given(ucenterService.getMetas(any(Request.class))).willReturn(builder.build());

        val userDetailsMetaService = new UserDetailsMetaServiceImpl(ucenterService, objectMapper);

        // when
        val value = userDetailsMetaService.getMetaData(user, "whatever", String.class);

        // then
        assertThat(value.isPresent()).isTrue();
        assertThat(value.get()).isEqualTo("string");
    }

    @Test
    void get_long_meta_should_be_ok() {
        // given
        val user = User.withUsername("test").password("123").authorities("aaa").build();

        val builder = UserMetaResp.newBuilder();
        builder.setErrCode(0)
            .addMeta(UserMeta.newBuilder().setType(MetaValueType.LONG).setName("whatever").setValue("1000").build());
        given(ucenterService.getMetas(any(Request.class))).willReturn(builder.build());

        val userDetailsMetaService = new UserDetailsMetaServiceImpl(ucenterService, objectMapper);

        // when
        val value = userDetailsMetaService.getMetaData(user, "whatever", Long.class);

        // then
        assertThat(value.isPresent()).isTrue();
        assertThat(value.get()).isEqualTo(1000L);
    }

    @Test
    void get_string_metas_should_be_ok() {
        // given
        val user = User.withUsername("test").password("123").authorities("aaa").build();

        val builder = UserMetaResp.newBuilder();
        builder.setErrCode(0)
            .addMeta(UserMeta.newBuilder().setType(MetaValueType.STRING).setName("whatever").setValue("1000").build());
        given(ucenterService.getMetas(any(Request.class))).willReturn(builder.build());

        val userDetailsMetaService = new UserDetailsMetaServiceImpl(ucenterService, objectMapper);

        val typeReference = new TypeReference<Collection<String>>() {
        };

        // when
        val value = userDetailsMetaService.getMultiMetaData(user, "whatever", typeReference);

        // then
        assertThat(value.isPresent()).isTrue();
        val longs = value.get();
        assertThat(longs).isNotEmpty();
        assertThat(longs.toArray(new String[] {})[0]).isEqualTo("1000");
    }

    @Test
    void get_long_metas_should_be_ok() {
        // given
        val user = User.withUsername("test").password("123").authorities("aaa").build();

        val builder = UserMetaResp.newBuilder();
        builder.setErrCode(0)
            .addMeta(UserMeta.newBuilder().setType(MetaValueType.LONG).setName("whatever").setValue("1000").build());
        given(ucenterService.getMetas(any(Request.class))).willReturn(builder.build());

        val userDetailsMetaService = new UserDetailsMetaServiceImpl(ucenterService, objectMapper);

        val typeReference = new TypeReference<Collection<Long>>() {
        };

        // when
        val value = userDetailsMetaService.getMultiMetaData(user, "whatever", typeReference);

        // then
        assertThat(value.isPresent()).isTrue();
        val longs = value.get();
        assertThat(longs).isNotEmpty();
        assertThat(longs.toArray(new Long[] {})[0]).isEqualTo(1000L);
    }

}
