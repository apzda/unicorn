/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.uc.security.authentication;

import cn.hutool.core.util.RandomUtil;
import com.apzda.cloud.audit.aop.AuditContextHolder;
import com.apzda.cloud.audit.aop.AuditLog;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.security.authentication.DeviceAuthenticationDetails;
import com.apzda.cloud.gsvc.security.exception.AuthenticationError;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.apzda.cloud.sms.proto.SmsService;
import com.apzda.cloud.sms.proto.Variable;
import com.apzda.cloud.sms.proto.VerifyReq;
import com.apzda.cloud.uc.UserMetas;
import com.apzda.cloud.uc.config.SmsLoginConfigProperties;
import com.apzda.cloud.uc.domain.entity.Oauth;
import com.apzda.cloud.uc.domain.entity.User;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.domain.vo.UserStatus;
import com.apzda.cloud.uc.error.AccountNotFoundError;
import com.apzda.cloud.uc.error.PhoneMissingError;
import com.apzda.cloud.uc.error.SmsCodeError;
import com.apzda.cloud.uc.security.filter.SmsLoginFilter;
import com.apzda.cloud.uc.security.realm.InternalAuthenticatingRealm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class SmsAuthenticationProvider implements AuthenticationProvider, ApplicationContextAware {

    public static final String PROVIDER = "phone";

    private final SmsLoginConfigProperties properties;

    private final UserManager userManager;

    private final UserDetailsMetaRepository userDetailsMetaRepository;

    private final SmsService smsService;

    @Override
    public void setApplicationContext(@Nullable ApplicationContext applicationContext) throws BeansException {
    }

    @Override
    @AuditLog(activity = "sms-login", template = "{} authenticated successfully",
            errorTpl = "{} authenticated failure: {}", args = { "#authentication.principal", "#throwExp?.message" })
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.trace("Start SMS Authentication: {}", authentication);

        val principal = authentication.getPrincipal();
        if (Objects.isNull(principal) || StringUtils.isBlank((String) principal)) {
            throw new GsvcException(new PhoneMissingError());
        }

        val phone = (String) principal;
        val context = AuditContextHolder.getContext();
        context.setUsername(phone);
        val token = (SmsLoginFilter.SmsLoginToken) authentication;
        // 验证码
        validateSmsCode(token);
        // 获取用户认证领域
        log.debug("开始短信验证码认证，认证域({}): 手机号: {}, 验证码: {}", PROVIDER, phone, authentication.getCredentials());

        Oauth oauth;
        try {
            // 用户已经注册过
            oauth = userManager.getOauthByUsernameAndProvider(phone, PROVIDER);
        }
        catch (UsernameNotFoundException e) {
            // 用户未注册过 - 启用自动注册
            if (properties.isAutoRegister()) {
                val user = new User();
                user.setUsername(phone);
                user.setNickname(phone);
                user.setPhoneNumber(phone);
                user.setPhonePrefix("");
                user.setPasswd(RandomUtil.randomString(18));
                user.setDevice(((DeviceAuthenticationDetails) token.getDetails()).getDevice());
                oauth = userManager.createOauthAccount(user, phone, phone, PROVIDER, token.getRecCode());
            }
            else {
                throw new GsvcException(new AccountNotFoundError());
            }
        }

        var user = oauth.getUser();
        try {
            if (user.getUsername() == null) {
                throw new IllegalStateException();
            }
        }
        catch (Exception e) {
            user = new User();
            user.setId(0L);
            user.setUsername(phone);
            user.setPasswd("");
        }

        val ud = org.springframework.security.core.userdetails.User
            .withUsername(user.getId() != 0L ? user.getUsername() : phone);
        ud.password(StringUtils.defaultIfBlank(user.getPasswd(), ""));

        if (user.getId() != 0L) {
            ud.accountExpired(user.getStatus() == UserStatus.EXPIRED);
            ud.credentialsExpired(userManager.isCredentialsExpired(user));
            ud.accountLocked(user.getStatus() == UserStatus.LOCKED);
            ud.disabled(user.getStatus() == UserStatus.DISABLED);
            ud.authorities(Collections.emptyList());
            context.setUsername(user.getUsername());
        }

        val realmUser = new InternalAuthenticatingRealm.InternalRealmUser(null, ud.build());
        val userDetailsMeta = userDetailsMetaRepository.create(realmUser);
        val authed = JwtAuthenticationToken.authenticated(userDetailsMeta, realmUser.getPassword());
        userDetailsMeta.remove(UserDetailsMeta.AUTHORITY_META_KEY);
        userDetailsMeta.remove(UserMetas.RUNNING_AS, authed);
        userDetailsMeta.set(UserDetailsMeta.LOGIN_TIME_META_KEY, authed, 0L);
        userDetailsMeta.setProvider(PROVIDER);
        userDetailsMeta.setOpenId(realmUser.getOpenId());
        userDetailsMeta.setUnionId(realmUser.getUnionId());

        userManager.onAuthenticated(authed, oauth);

        return authed;
    }

    private void validateSmsCode(SmsLoginFilter.SmsLoginToken authentication) {
        val builder = VerifyReq.newBuilder();
        builder.setPhone(authentication.getPhone());
        builder.setTid(authentication.getTemplateId());
        builder.addVariable(Variable.newBuilder().setName("code").setValue(authentication.getCode()));
        try {
            val res = smsService.verify(builder.build());
            if (res.getErrCode() == 0) {
                return;
            }
            else {
                log.debug("Verify SMS code failed, phone:{} - {}", authentication.getPhone(), res.getErrMsg());
            }
        }
        catch (Exception e) {
            log.debug("Verify SMS code failed, phone: {} - {}", authentication.getPhone(), e.getMessage());
        }
        throw new AuthenticationError(new SmsCodeError());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SmsLoginFilter.SmsLoginToken.class.isAssignableFrom(authentication);
    }

}
