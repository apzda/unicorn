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
package com.apzda.cloud.uc.service;

import com.apzda.cloud.audit.aop.AuditContextHolder;
import com.apzda.cloud.audit.aop.AuditLog;
import com.apzda.cloud.gsvc.config.Props;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.dto.MessageType;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.security.mfa.MfaStatus;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import com.apzda.cloud.uc.ErrorCode;
import com.apzda.cloud.uc.UserMetas;
import com.apzda.cloud.uc.config.UCenterConfig;
import com.apzda.cloud.uc.config.UCenterConfigProperties;
import com.apzda.cloud.uc.domain.entity.UserMfa;
import com.apzda.cloud.uc.domain.mapper.RoleMapper;
import com.apzda.cloud.uc.domain.repository.UserMfaRepository;
import com.apzda.cloud.uc.domain.repository.UserRepository;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.domain.vo.UserStatus;
import com.apzda.cloud.uc.error.AlreadySwitchedError;
import com.apzda.cloud.uc.proto.*;
import com.google.protobuf.Empty;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.apzda.cloud.uc.ErrorCode.*;
import static com.apzda.cloud.uc.domain.UserManagerImpl.handleOccupiedError;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final UserManager userManager;

    private final PasswordEncoder passwordEncoder;

    private final UserDetailsMetaRepository userDetailsMetaRepository;

    private final UserRepository userRepository;

    private final UserMfaRepository userMfaRepository;

    private final RoleMapper roleMapper;

    private final UCenterConfigProperties properties;

    private final UCenterConfig uCenterConfig;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public AccountListResponse list(@Nonnull AccountListRequest request) {
        val builder = AccountListResponse.newBuilder().setErrCode(0);
        val current = Math.max(1, request.getCurrent()) - 1;
        val size = Math.max(10, request.getSize());
        val pageRequest = PageRequest.of(current, size);
        pageRequest.withSort(Sort.Direction.ASC, "username");
        ExampleMatcher matcher = ExampleMatcher.matchingAll();
        val probe = new com.apzda.cloud.uc.domain.entity.User();
        boolean quering = false;
        if (StringUtils.isNotBlank(request.getUsername())) {
            matcher = matcher.withMatcher("username", ExampleMatcher.GenericPropertyMatchers.contains());
            probe.setUsername(request.getUsername());
            quering = true;
        }

        if (StringUtils.isNotBlank(request.getPhone())) {
            matcher = matcher.withMatcher("phoneNumber", ExampleMatcher.GenericPropertyMatchers.startsWith());
            matcher = matcher.withMatcher("phonePrefix", ExampleMatcher.GenericPropertyMatchers.exact());
            probe.setPhonePrefix("");
            probe.setPhoneNumber(request.getPhone());
            quering = true;
        }

        if (StringUtils.isNotBlank(request.getEmail())) {
            matcher = matcher.withMatcher("email", ExampleMatcher.GenericPropertyMatchers.startsWith());
            probe.setEmail(request.getEmail());
            quering = true;
        }

        if (StringUtils.isNotBlank(request.getId())) {
            matcher = matcher.withMatcher("id", ExampleMatcher.GenericPropertyMatchers.exact());
            probe.setId(Long.parseLong(request.getId()));
            quering = true;
        }

        if (StringUtils.isNotBlank(request.getStatus())) {
            matcher = matcher.withMatcher("status", ExampleMatcher.GenericPropertyMatchers.exact());
            probe.setStatus(UserStatus.fromName(request.getStatus()));
            quering = true;
        }

        Page<com.apzda.cloud.uc.domain.entity.User> users;
        if (quering) {
            val example = Example.of(probe, matcher);
            users = userRepository.findAll(example, pageRequest);
        }
        else {
            users = userRepository.findAll(pageRequest);
        }

        builder.setTotalPage(users.getTotalPages());
        val tenantId = TenantManager.tenantId(0L);
        val records = users.getContent().stream().map(user -> {
            val ab = Account.newBuilder();
            assert user.getId() != null;
            ab.setId(user.getId());
            ab.setUsername(user.getUsername());
            ab.setDisplayName(user.getNickname());
            if (!StringUtils.isBlank(user.getPhoneNumber())) {
                ab.setPhone(user.getPhoneNumber());
            }
            if (!StringUtils.isBlank(user.getEmail())) {
                ab.setEmail(user.getEmail());
            }
            ab.setStatus(user.getStatus().name());
            val roles = user.getRoles(tenantId);
            if (!CollectionUtils.isEmpty(roles)) {
                ab.addAllRoles(roles.stream().map(roleMapper::fromEntity).toList());
            }
            return ab.build();
        }).toList();

        builder.addAllResults(records);
        return builder.setCurrent(request.getCurrent()).setTotalRecord(users.getTotalElements()).setSize(size).build();
    }

    @Override
    @AuditLog(activity = "创建账号", template = "账户'{}'创建成功", errorTpl = "账户'{}'创建失败: {}",
            args = { "#request.username", "#throwExp?.message" })
    public CreateAccountResponse createAccount(CreateAccountRequest request) {
        return userManager.createAccount(request);
    }

    @Override
    @AuditLog(activity = "修改账号", template = "账户'{}'修改成功", errorTpl = "账户'{}'修改失败: {}",
            args = { "#request.username", "#throwExp?.message" })
    public GsvcExt.CommonRes updateAccount(UpdateAccountRequest request) {
        return userManager.updateAccount(request);
    }

    @Override
    public GsvcExt.CommonRes deleteAccount(UserId request) {
        throw new AccessDeniedException("不支持删除用户");
    }

    @Override
    @Transactional
    @Modifying
    public GsvcExt.CommonRes updatePassword(UpdatePasswordRequest request) {
        val builder = GsvcExt.CommonRes.newBuilder().setErrCode(0);
        if (!Objects.equals(request.getConfirmPassword(), request.getNewPassword())) {
            return builder.setErrCode(PWD_NOT_MATCH).build();
        }
        if (StringUtils.isBlank(request.getNewPassword())) {
            return builder.setErrCode(PWD_IS_BLANK).build();
        }
        if (Objects.equals(request.getNewPassword(), request.getOldPassword())) {
            return builder.setErrCode(PWD_SAME_AS_ORIGINAL).build();
        }
        val currentUser = request.getCurrentUser();
        val username = currentUser.getUid();
        val user = userManager.getUserByUsername(username);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswd())) {
            return builder.setErrCode(PWD_IS_INVALID).build();
        }

        user.setPasswd(passwordEncoder.encode(request.getNewPassword()));
        userManager.setupUserCredentialsExpired(user, null);

        val build = User.withUsername(username).password(request.getNewPassword()).build();
        // 删除缓存的用户信息，强制用户在所有设备上重新登录
        userDetailsMetaRepository.removeMetaData(build, UserDetailsMeta.CACHED_USER_DETAILS_KEY);
        return builder.setErrCode(0).build();
    }

    @Override
    @AuditLog(activity = "分配角色", template = "账户'{}'分配角色成功", errorTpl = "账户'{}'分配角色失败: {}",
            args = { "#request.username", "#throwExp?.message" })
    public GsvcExt.CommonRes assignRole(UpdateRoleRequest request) {
        val context = AuditContextHolder.getContext();
        context.setNewValue(request.getRolesList());
        return userManager.assignRole(request);
    }

    @Override
    @Transactional
    @Modifying
    public GsvcExt.CommonRes updateMyAccount(UpdateAccountInfoRequest request) {
        val builder = GsvcExt.CommonRes.newBuilder().setErrCode(0);
        try {
            val currentUser = request.getCurrentUser();
            val username = currentUser.getUid();
            val user = userManager.getUserByUsername(username);
            val phoneHolder = userRepository.findByPhoneNumberAndPhonePrefix(request.getPhone(),
                    request.getPhonePrefix());
            if (phoneHolder.isPresent() && !Objects.equals(user.getId(), phoneHolder.get().getId())) {
                return builder.setErrCode(PHONE_IS_OCCUPIED).build();
            }
            val emailHolder = userRepository.findByEmail(request.getEmail());
            if (emailHolder.isPresent() && !Objects.equals(user.getId(), emailHolder.get().getId())) {
                return builder.setErrCode(EMAIL_IS_OCCUPIED).build();
            }
            user.setNickname(request.getDisplayName());
            user.setPhoneNumber(request.getPhone());
            user.setPhonePrefix(request.getPhonePrefix());
            user.setEmail(request.getEmail());
            userRepository.save(user);
            entityManager.flush();
        }
        catch (Exception e) {
            handleOccupiedError(e);
        }
        // todo 触发账户更新事件
        return builder.setErrCode(0).build();
    }

    @Override
    @Transactional(readOnly = true)
    @AuditLog(activity = "踢下线", template = "账户'{}'被强制下线", errorTpl = "账户'{}'强制下线失败: {}",
            args = { "#request.username", "#throwExp?.message" })
    public GsvcExt.CommonRes kickoff(Account request) {
        val builder = GsvcExt.CommonRes.newBuilder();
        val uname = request.getUsername();
        if (StringUtils.isNotBlank(uname)) {
            try {
                val user = userManager.getUserByUsername(uname);
                val username = user.getUsername();
                val ud = User.withUsername(username).password(user.getPasswd()).build();
                userDetailsMetaRepository.removeMetaData(ud);
                log.info("User '{}' has been kickoff", username);
            }
            catch (Exception e) {
                log.warn("kickoff User '{}' error", uname, e);
            }
        }

        return builder.setErrCode(0)
            .setErrType(MessageType.NOTIFY.name())
            .setErrMsg(I18nUtils.t("user.kickoff", new Object[] { uname }))
            .build();
    }

    @Override
    @AuditLog(activity = "授权", template = "查看授权码: {}", errorTpl = "查看授权码失败: {}{}",
            args = { "#cData['code']", "#throwExp?.message" })
    public SwitchCodeRes switchCode(Empty request) {
        val builder = SwitchCodeRes.newBuilder();
        val context = AuditContextHolder.getContext();
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            val jwtToken = token.getJwtToken();
            val runAs = jwtToken.getRunAs();
            if (StringUtils.isNotBlank(runAs)) {
                throw new GsvcException(new AlreadySwitchedError());
            }
            val userDetails = token.getUserDetails();
            userDetails.ifPresent(ud -> {
                val gCode = ud.get(UserMetas.RUNNING_GT, token);
                if (StringUtils.isNotBlank(gCode)) {
                    builder.setCode(gCode);
                }
                else {
                    val code = RandomStringUtils.random(12, true, true);
                    builder.setCode(code);
                    ud.set(UserMetas.RUNNING_GT, token, code);
                }
            });
        }
        val code = builder.getCode();
        if (StringUtils.isBlank(code)) {
            context.set("code", null);
            throw new GsvcException(ServiceError.SERVICE_ERROR);
        }
        else {
            context.set("code", code.substring(0, 4) + "****" + code.substring(8, 12));
        }
        return builder.setErrCode(0).build();
    }

    @Override
    @Transactional
    @Modifying
    public MfaConfigRes mfaConfig(Empty request) {
        return setupUserMfa(false);
    }

    @Override
    @Transactional
    @Modifying
    @AuditLog(activity = "mfa", template = "重置多重认证: {}", errorTpl = "重置多重认证失败: {}{}",
            args = { "#returnObj?.errCode", "#throwExp?.message" })
    public MfaConfigRes resetMfa(Empty request) {
        return setupUserMfa(true);
    }

    @Override
    @Transactional
    @Modifying
    @AuditLog(activity = "mfa", template = "多重认证配置: {}", errorTpl = "多重认证配置失败: {}{}",
            args = { "#returnObj?.errCode", "#throwExp?.message" })
    public GsvcExt.CommonRes setupMfa(VerifyMfaReq request) {
        val builder = GsvcExt.CommonRes.newBuilder();
        val authenticator = uCenterConfig.getAuthenticator();
        if (authenticator == null) {
            return builder.setErrCode(ErrorCode.AUTHENTICATOR_NOT_SET).buildPartial();
        }
        val type = authenticator.getType();
        val meta = UserManager.getUserDetailsMeta();
        val username = meta.getUsername();
        val user = userManager.getUserByUsername(username);
        val mfaOpt = user.getMfa(type);
        if (mfaOpt.isEmpty()) {
            return builder.setErrCode(ErrorCode.MFA_NOT_INITIALIZED).build();
        }
        val password = request.getPassword();
        if (!passwordEncoder.matches(password, user.getPasswd())) {
            return builder.setErrCode(ErrorCode.PWD_IS_INVALID).setErrType(MessageType.NOTIFY.name()).build();
        }
        val userMfa = mfaOpt.get();
        if (!authenticator.verify(request.getCode(), userMfa.getSecretKey())) {
            return builder.setErrCode(ErrorCode.MFA_NOT_MATCH).setErrType(MessageType.NOTIFY.name()).build();
        }
        userMfa.setEnabled(true);
        try {
            userMfaRepository.save(userMfa);
            entityManager.flush();
        }
        catch (Exception e) {
            throw new GsvcException(ServiceError.SERVICE_ERROR, e);
        }
        meta.set(UserDetailsMeta.MFA_STATUS_KEY, UserManager.getAuthentication(), MfaStatus.VERIFIED);
        return builder.setErrCode(0).setErrMsg("多重认证设置成功").setErrType(MessageType.NOTIFY.name()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public GsvcExt.CommonRes verifyMfa(VerifyMfaReq request) {
        val builder = GsvcExt.CommonRes.newBuilder();
        val authenticator = uCenterConfig.getAuthenticator();
        if (authenticator == null) {
            return builder.setErrCode(ErrorCode.AUTHENTICATOR_NOT_SET).buildPartial();
        }
        val type = authenticator.getType();
        val meta = UserManager.getUserDetailsMeta();
        val username = meta.getUsername();
        val user = userManager.getUserByUsername(username);
        val mfaOpt = user.getMfa(type);
        if (mfaOpt.isEmpty()) {
            return builder.setErrCode(ErrorCode.MFA_NOT_INITIALIZED).build();
        }
        val userMfa = mfaOpt.get();
        if (!authenticator.verify(request.getCode(), userMfa.getSecretKey())) {
            return builder.setErrCode(ErrorCode.MFA_NOT_MATCH).setErrType(MessageType.NOTIFY.name()).build();
        }
        meta.set(UserDetailsMeta.MFA_STATUS_KEY, UserManager.getAuthentication(), MfaStatus.VERIFIED);
        return builder.setErrCode(0).build();
    }

    /**
     * 获取用户MFA配置
     * @param reset 是否重置已有配置
     * @return 配置响应
     */
    private MfaConfigRes setupUserMfa(boolean reset) {
        val builder = MfaConfigRes.newBuilder().setErrCode(0);
        val authenticator = uCenterConfig.getAuthenticator();
        if (authenticator == null) {
            return builder.setErrCode(ErrorCode.AUTHENTICATOR_NOT_SET).buildPartial();
        }
        val type = authenticator.getType();
        val meta = UserManager.getUserDetailsMeta();
        val username = meta.getUsername();
        val user = userManager.getUserByUsername(username);
        val needSave = new AtomicBoolean(reset);
        val mfa = user.getMfa(type).orElseGet(() -> {
            val userMfa = new UserMfa();
            userMfa.setType(type);
            userMfa.setSecretKey(authenticator.getSecretKey());
            userMfa.setUser(user);
            userMfa.setEnabled(false);
            userMfaRepository.save(userMfa);
            entityManager.flush();
            needSave.set(false);
            return userMfa;
        });

        if (needSave.get()) {
            mfa.setSecretKey(authenticator.getSecretKey());
            mfa.setEnabled(false);
            userMfaRepository.save(mfa);
            entityManager.flush();
        }

        val config = authenticator.getConfig(user.getUsername(), mfa.getSecretKey(), new Props(properties.getProps()));
        if (config != null) {
            builder.setConfig(config);
        }
        if (mfa.getSecretKey() != null) {
            builder.setSecretKey(mfa.getSecretKey());
        }
        builder.setInitialized(Boolean.TRUE.equals(mfa.getEnabled()));
        builder.setType(type);
        return builder.build();
    }

}
