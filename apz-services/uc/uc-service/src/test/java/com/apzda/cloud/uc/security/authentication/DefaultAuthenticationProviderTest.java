package com.apzda.cloud.uc.security.authentication;

import com.apzda.cloud.gsvc.security.event.AuthenticationCompleteEvent;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.token.SimpleJwtToken;
import com.apzda.cloud.uc.domain.entity.Oauth;
import com.apzda.cloud.uc.domain.repository.OauthRepository;
import com.apzda.cloud.uc.domain.repository.OauthSessionRepository;
import com.apzda.cloud.uc.test.TestBase;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Transactional
class DefaultAuthenticationProviderTest extends TestBase {

    @Autowired
    @Qualifier("defaultAuthenticationProvider")
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private OauthRepository oauthRepository;

    @Autowired
    private OauthSessionRepository oauthSessionRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void authenticate_must_be_ok() {
        // given
        val token = UsernamePasswordAuthenticationToken.unauthenticated("admin", "123456");

        // when
        val authed = authenticationProvider.authenticate(token);

        // then
        assertThat(authed).isNotNull();

        // when
        val oauth = oauthRepository.findByOpenIdAndProvider("admin", Oauth.SIMPLE);
        // then
        assertThat(oauth.isPresent()).isTrue();
        assertThat(oauth.get().getLastDevice()).isEqualTo("pc");

        // given
        val jwtToken = SimpleJwtToken.builder().accessToken("123").provider(Oauth.SIMPLE).build();
        ((JwtAuthenticationToken) authed).setJwtToken(jwtToken);
        val event = new AuthenticationCompleteEvent(authed, jwtToken);
        // when
        eventPublisher.publishEvent(event);
        // then
        val session = oauthSessionRepository.getFirstByOauthOrderByCreatedAtDesc(oauth.get());
        assertThat(session.isPresent()).isTrue();
        assertThat(session.get().getAccessToken()).isEqualTo("123");
    }

    @Test
    void authenticate_must_be_failure() {
        // given
        val token = UsernamePasswordAuthenticationToken.unauthenticated("admin", "123457");

        // when
        assertThatThrownBy(() -> {
            authenticationProvider.authenticate(token);
        }).hasMessage("Username or Password is incorrect");

    }

}
