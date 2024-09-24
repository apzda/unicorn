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
package com.apzda.cloud.uc.domain;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.apzda.cloud.config.exception.SettingUnavailableException;
import com.apzda.cloud.config.service.SettingService;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.error.NotBlankError;
import com.apzda.cloud.gsvc.error.NotFoundError;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.security.authentication.AuthenticationDetails;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.uc.config.UCenterConfigProperties;
import com.apzda.cloud.uc.domain.entity.Role;
import com.apzda.cloud.uc.domain.entity.UserMeta;
import com.apzda.cloud.uc.domain.entity.*;
import com.apzda.cloud.uc.domain.repository.*;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.domain.vo.Gender;
import com.apzda.cloud.uc.domain.vo.MetaType;
import com.apzda.cloud.uc.domain.vo.UserStatus;
import com.apzda.cloud.uc.error.*;
import com.apzda.cloud.uc.event.AccountEvent;
import com.apzda.cloud.uc.event.EventType;
import com.apzda.cloud.uc.proto.*;
import com.apzda.cloud.uc.setting.UcSetting;
import com.google.common.collect.Lists;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static com.apzda.cloud.uc.ErrorCode.PWD_IS_BLANK;
import static com.apzda.cloud.uc.ErrorCode.PWD_NOT_MATCH;
import static com.apzda.cloud.uc.domain.entity.OauthSession.SIMULATOR;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagerImpl implements UserManager, ApplicationEventPublisherAware {

    private final UCenterConfigProperties properties;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    private final OauthRepository oauthRepository;

    private final OauthSessionRepository oauthSessionRepository;

    private final PasswordEncoder passwordEncoder;

    private final SettingService settingService;

    @PersistenceContext
    private EntityManager entityManager;

    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void setApplicationEventPublisher(@Nonnull ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Nonnull
    public User getUserByUsername(String username) {
        return getUserByUsernameAndProvider(username, Oauth.SIMPLE);
    }

    @Override
    @Nonnull
    public User getUserByUsernameAndProvider(@Nonnull String username, @Nonnull String provider) {
        if (StringUtils.isBlank(username)) {
            throw new UsernameNotFoundException("username is blank");
        }
        val oauth = oauthRepository.findByUnionIdAndProvider(username, provider);
        if (oauth.isEmpty()) {
            throw new UsernameNotFoundException(String.format("User '%s' not found", username));
        }
        val user = oauth.get().getUser();

        if (user == null) {
            throw new UsernameNotFoundException(String.format("User '%s' not found", username));
        }
        return user;
    }

    @Override
    @Nonnull
    public Oauth getOauthByUsernameAndProvider(@Nonnull String openId, @Nonnull String provider) {
        val oauth = oauthRepository.findByUnionIdAndProvider(openId, provider);
        if (oauth.isPresent()) {
            return oauth.get();
        }
        throw new UsernameNotFoundException(String.format("User '%s' not found", openId));
    }

    @Override
    public boolean isCredentialsExpired(@Nonnull User user) {
        val credentialsExpiredAt = user.getMeta(UserMeta.CREDENTIALS_EXPIRED_AT);
        if (credentialsExpiredAt.isEmpty()) {
            return false;
        }
        val userMeta = credentialsExpiredAt.get();
        val value = userMeta.getValue();
        try {
            val expiredAt = Long.parseLong(value);
            if (expiredAt < System.currentTimeMillis()) {
                return true;
            }
        }
        catch (Exception e) {
            log.warn("Cannot parse user({})'s credentialsExpiredAt({}) to long: {}", user.getId(), value,
                    e.getMessage());
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public void setupUserCredentialsExpired(User user, Integer timeout) {
        try {
            val setting = settingService.load(UcSetting.class);
            int passwordExpired = Optional.ofNullable(timeout).orElse(setting.getPasswordExpired());
            String expiredAt = null;
            if (passwordExpired > 0) {
                expiredAt = String.valueOf(passwordExpired * 1000L + System.currentTimeMillis());
            }
            else if (passwordExpired < 0) {
                expiredAt = "0";
            }
            if (expiredAt == null) {
                return;
            }
            val meta = user.getMeta(UserMeta.CREDENTIALS_EXPIRED_AT);
            if (meta.isPresent()) {
                val m = meta.get();
                m.setValue(expiredAt);
            }
            else {
                val mt = new UserMeta();
                mt.setUser(user);
                mt.setType(MetaType.L);
                mt.setTenantId(0L);
                mt.setName(UserMeta.CREDENTIALS_EXPIRED_AT);
                mt.setValue(expiredAt);
                user.setMetas(Lists.newArrayList(mt));
            }
        }
        catch (SettingUnavailableException e) {
            throw new GsvcException(ServiceError.SERVICE_ERROR, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying
    public void onAuthenticated(AbstractAuthenticationToken token, Oauth oauth) {
        // Oauth Data
        oauth = oauthRepository.findByUnionIdAndProvider(oauth.getUnionId(), oauth.getProvider()).orElse(null);
        if (oauth == null) {
            throw new UsernameNotFoundException(String.format("%s is not found", token.getName()));
        }
        // Last Login Info
        oauth.setLastLoginTime(DateUtil.current());
        oauth.setLastIp("0.0.0.0");
        oauth.setLastDevice("UNKNOWN");
        if (token.getDetails() instanceof AuthenticationDetails details) {
            oauth.setLastDevice(details.getDevice());
            oauth.setLastIp(details.getRemoteAddress());
        }
        if (token.getPrincipal() instanceof UserDetailsMeta meta) {
            try {
                meta.setUid(String.valueOf(oauth.getUser().getId()));
            }
            catch (Exception e) {
                meta.setUid("");
            }
            // 清空MFA认证结果
            meta.remove(UserDetailsMeta.MFA_STATUS_KEY, token);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying(clearAutomatically = true)
    public void createOauthSession(@Nonnull JwtAuthenticationToken token) {
        // Record Login Session
        val userDetails = (UserDetailsMeta) token.getPrincipal();
        val oauth = oauthRepository.findByUnionIdAndProvider(userDetails.getUsername(), userDetails.getProvider())
            .orElse(null);
        if (oauth == null) {
            throw new UsernameNotFoundException(String.format("%s is not found", token.getName()));
        }
        val session = newOauthSession(token, oauth);
        oauthSessionRepository.save(session);
        assert session.getId() != null;
        log.trace("Session recorded: {}", oauth.getOpenId());

        publishEvent(new AccountEvent(userDetails.getUsername(), EventType.LOGIN));
    }

    @Nullable
    @Override
    public OauthSession getLastLoginSession(Oauth oauth) {
        val session = oauthSessionRepository.getFirstByOauthOrderByCreatedAtDesc(oauth);
        return session.orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying
    public CreateAccountResponse createAccount(CreateAccountRequest dto, String provider) {
        val builder = CreateAccountResponse.newBuilder().setErrCode(0);
        if (StringUtils.isBlank(dto.getPassword())) {
            return builder.setErrCode(PWD_IS_BLANK).build();
        }
        if (!Objects.equals(dto.getConfirmPassword(), dto.getPassword())) {
            return builder.setErrCode(PWD_NOT_MATCH).build();
        }

        val user = createUser(dto);

        createOauthAccount(user, user.getUsername(), user.getUsername(), provider, dto.getRecCode());

        builder.setUid(user.getId());

        publishEvent(new AccountEvent(user.getUsername(), EventType.CREATED));
        return builder.build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying
    public OpenAccountResponse openAccount(OpenAccountRequest request) {
        val builder = CreateAccountRequest.newBuilder();
        builder.setUsername(request.getUsername());
        builder.setDisplayName(request.getDisplayName());
        builder.setPassword(request.getPassword());
        builder.setConfirmPassword(request.getPassword());
        builder.setResetPassword(false);
        builder.setStatus("ACTIVATED");
        builder.addAllRoles(request.getRolesList());
        if (request.hasAvatar()) {
            builder.setAvatar(request.getAvatar());
        }
        if (request.hasEmail()) {
            builder.setEmail(request.getEmail());
        }
        if (request.hasPhone()) {
            builder.setPhone(request.getPhone());
        }
        if (request.hasPhoneIac()) {
            builder.setPhoneIac(request.getPhoneIac());
        }
        if (request.hasChannel()) {
            builder.setChannel(request.getChannel());
        }
        if (request.hasGender()) {
            builder.setGender(request.getGender());
        }
        if (request.hasRecCode() && StringUtils.isNotBlank(request.getRecCode())) {
            builder.setRecCode(request.getRecCode());
        }
        else {
            val setting = settingService.load(UcSetting.class);
            if (setting.isRecCodeNeed()) {
                throw new GsvcException(new RecCodeMissingError());
            }
        }
        if (request.hasFirstName()) {
            builder.setFirstName(request.getFirstName());
        }
        if (request.hasLastName()) {
            builder.setLastName(request.getLastName());
        }

        val resp = createAccount(builder.build(), StringUtils.defaultIfBlank(request.getProvider(), Oauth.SIMPLE));
        val b = OpenAccountResponse.newBuilder().setErrCode(resp.getErrCode());
        if (resp.hasErrMsg()) {
            b.setErrMsg(resp.getErrMsg());
        }
        b.setUid(resp.getUid());
        return b.build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying
    public GsvcExt.CommonRes updateAccount(UpdateAccountRequest dto) {
        val builder = GsvcExt.CommonRes.newBuilder().setErrCode(0);
        if (StringUtils.isNotBlank(dto.getPassword()) && !Objects.equals(dto.getConfirmPassword(), dto.getPassword())) {
            throw new GsvcException(new PasswordNotMatchError());
        }
        val username = dto.getUsername();
        val user = userRepository.findByUsername(username);
        if (user == null) {
            throw new GsvcException(new NotFoundError("{resource.user}", username));
        }
        // 修改密码判定
        if (StringUtils.isNotBlank(dto.getPassword())) {
            user.setPasswd(passwordEncoder.encode(dto.getPassword()));
            // 需要立即重置密码
            setupUserCredentialsExpired(user, -1);
        }
        user.setNickname(dto.getDisplayName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhone());
        if (1L != user.getId()) {
            user.setStatus(UserStatus.fromName(dto.getStatus()));
        }
        userRepository.save(user);
        try {
            entityManager.flush();
        }
        catch (Exception e) {
            handleOccupiedError(e);
        }
        // 角色
        if (dto.getRolesCount() > 0) {
            updateRoles(dto.getRolesList(), user, true);
        }

        val eventType = switch (user.getStatus()) {
            case LOCKED -> EventType.LOCKED;
            case ACTIVATED -> EventType.ACTIVATED;
            case DISABLED -> EventType.DISABLED;
            default -> null;
        };

        if (eventType != null) {
            publishEvent(new AccountEvent(user.getUsername(), eventType));
        }
        return builder.build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying
    public GsvcExt.CommonRes resetPassword(ResetPasswordRequest dto) {
        val builder = GsvcExt.CommonRes.newBuilder().setErrCode(0);
        val username = dto.getUsername();
        val user = userRepository.findByUsername(username);
        if (user == null) {
            throw new GsvcException(new NotFoundError("{resource.user}", username));
        }

        if (StringUtils.isNotBlank(dto.getPassword())) {
            user.setPasswd(passwordEncoder.encode(dto.getPassword()));
            setupUserCredentialsExpired(user, null);
            userRepository.save(user);
        }
        else {
            throw new GsvcException(new NotBlankError("password"));
        }

        return builder.build();
    }

    @Nonnull
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying
    public User createUser(@Nonnull CreateAccountRequest dto) {
        val setting = settingService.load(UcSetting.class);
        val device = StringUtils.defaultIfBlank(dto.getDevice(), "pc");
        // 创建用户
        val user = new User();
        user.setUsername(dto.getUsername()); // 验重
        user.setNickname(dto.getDisplayName());
        if (dto.hasPhone()) {
            if (StringUtils.isBlank(dto.getPhone())) {
                throw new GsvcException(new PhoneMissingError());
            }
            user.setPhoneNumber(dto.getPhone());
            if (dto.hasPhoneIac()) {
                user.setPhonePrefix(dto.getPhoneIac());
            }
            else {
                user.setPhonePrefix("");
            }
        }
        else if (setting.isPhoneNeed()) {
            throw new GsvcException(new PhoneMissingError());
        }
        val email = dto.getEmail();
        if (StringUtils.isNotBlank(email)) {
            val emailOccupied = userRepository.existsByEmail(email);
            if (emailOccupied) {
                throw new GsvcException(new EmailOccupiedError());
            }
            user.setEmail(email);
        }
        else if (setting.isEmailNeed()) {
            throw new GsvcException(new EmailMissingError());
        }
        user.setPasswd(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(UserStatus.fromName(dto.getStatus()));
        user.setIp(GsvcContextHolder.getRemoteIp());
        user.setDevice(device);
        calculateReference(user, dto.getRecCode(), setting);
        if (dto.hasAvatar()) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.hasChannel()) {
            user.setChannel(dto.getChannel());
        }
        if (dto.hasGender()) {
            user.setGender(Gender.valueOf(dto.getGender()));
        }
        if (dto.hasFirstName()) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.hasLastName()) {
            user.setLastName(dto.getLastName());
        }

        userRepository.save(user);

        try {
            entityManager.flush();
            generateRecCode(user);
        }
        catch (Exception e) {
            handleOccupiedError(e);
        }
        if (dto.getResetPassword()) {
            // 重置密码
            setupUserCredentialsExpired(user, -1);
        }
        else {
            // 密码永不过期
            setupUserCredentialsExpired(user, null);
        }
        // 角色
        val defaultRoles = setting.getDefaultRoles();
        val roles = new ArrayList<>(dto.getRolesList());
        if (roles.isEmpty() && defaultRoles != null && !defaultRoles.isEmpty()) {
            roles.addAll(defaultRoles);
        }

        if (!roles.isEmpty()) {
            updateRoles(roles, user, true);
        }
        return user;
    }

    @Nonnull
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    public Oauth createOauthAccount(@Nonnull User user, @Nonnull String openId, @Nonnull String unionId,
            @Nonnull String provider, String recCode) {
        val setting = settingService.load(UcSetting.class);

        var id = user.getId();
        if (id == null) {
            id = 0L;
            user.setId(id);
        }
        // 自动创建账户
        if (id == 0 && properties.isAutoCreateAccount()) {
            user.setId(null);
            if (StringUtils.isBlank(user.getUsername())) {
                user.setUsername(openId);
            }
            user.setStatus(UserStatus.ACTIVATED);
            user.setIp(GsvcContextHolder.getRemoteIp());
            user.setPasswd(passwordEncoder.encode(user.getPasswd()));
            calculateReference(user, recCode, setting);

            int i = 0;
            val username = user.getUsername();
            val prefix = StringUtils.defaultIfBlank(properties.getUsernamePrefix(), "");

            while (userRepository.existsByUsername(user.getUsername())) {
                // 理论上生成username不会是死循环
                user.setUsername(generateRandomUsername(prefix, username, ++i));
            }
            userRepository.save(user);
            try {
                entityManager.flush();
            }
            catch (Exception e) {
                handleOccupiedError(e);
            }

            generateRecCode(user);
            // 密码永不过期
            setupUserCredentialsExpired(user, null);
            // 默认角色
            val defaultRoles = setting.getDefaultRoles();
            if (!defaultRoles.isEmpty()) {
                updateRoles(defaultRoles, user, true);
            }

            publishEvent(new AccountEvent(user.getUsername(), EventType.CREATED));
        }

        if (!Oauth.SIMPLE.equals(provider)) {
            createOauth(user, user.getUsername(), user.getUsername(), Oauth.SIMPLE, recCode);
        }
        // 自动绑定
        if (Oauth.SIMPLE.equals(provider) && StringUtils.isNotBlank(user.getPhoneNumber()) && setting.isAutoBind()) {
            createOauth(user, user.getPhoneNumber(), user.getPhoneNumber(), Oauth.PHONE, recCode);
        }

        return createOauth(user, openId, unionId, provider, recCode);
    }

    /**
     * 处理用户名，手机，邮箱占用异常.
     * @param e 异常.
     * @throws GsvcException 用户名，手机，邮箱被占用时
     */
    public static void handleOccupiedError(Exception e) throws GsvcException {
        val message = e.getMessage();
        if (message.indexOf("UDX_USERNAME") > 0) {
            throw new GsvcException(new UsernameOccupiedError(), e);
        }
        if (message.indexOf("UDX_PHONE") > 0) {
            throw new GsvcException(new PhoneOccupiedError(), e);
        }
        if (message.indexOf("UDX_EMAIL") > 0) {
            throw new GsvcException(new EmailOccupiedError(), e);
        }
        throw new GsvcException(ServiceError.SERVICE_ERROR, e);
    }

    @Nonnull
    private Oauth createOauth(User user, String openId, String unionId, String provider, String recCode) {
        val old = oauthRepository.findByUnionIdAndProvider(unionId, provider);

        if (old.isPresent()) {
            val oauth = old.get();
            val u = oauth.getUser();
            if (!u.getId().equals(user.getId())) {
                throw new GsvcException(new UsernameOccupiedError());
            }

            return oauth;
        }

        val oauth = new Oauth();
        val device = user.getDevice();
        oauth.setOpenId(openId);
        oauth.setUnionId(unionId);
        oauth.setRecCode(recCode);
        oauth.setProvider(provider);
        oauth.setUser(user);
        oauth.setIp(GsvcContextHolder.getRemoteIp());
        oauth.setLastIp(GsvcContextHolder.getRemoteIp());
        oauth.setDevice(device);
        oauth.setLastDevice(device);
        oauth.setLoginTime(0L);
        oauth.setLastLoginTime(0L);
        oauthRepository.save(oauth);
        try {
            entityManager.flush();
        }
        catch (Exception e) {
            val message = e.getMessage();
            if (message.indexOf("UDX_TYPE_ID") > 0) {
                throw new GsvcException(new UsernameOccupiedError(), e);
            }
            throw new GsvcException(ServiceError.SERVICE_ERROR, e);
        }
        return oauth;
    }

    /**
     * 为用户生成推荐码
     * @param user 用户实体
     */
    private void generateRecCode(@Nonnull User user) {
        val id = user.getId();
        var recNumber = new BigDecimal(id).add(new BigDecimal(System.currentTimeMillis()));
        do {
            val recCode = Base64.encodeWithoutPadding(recNumber.toBigInteger().toByteArray()).replace("/", "");
            val used = userRepository.findByRecommendCode(recCode);
            if (used.isEmpty()) {
                user.setRecommendCode(recCode);
                break;
            }
            recNumber = recNumber.add(BigDecimal.ONE);
        }
        while (true);
    }

    private String generateRandomUsername(String prefix, String username, int i) {
        if (StringUtils.isBlank(prefix)) {
            return username + "-" + i;
        }
        return prefix + RandomUtil.randomNumbers(12);
    }

    /**
     * 保存账户角色信息.
     * @param roleList 角色列表
     * @param user 当前用户实例
     */
    @Override
    @Transactional
    @Modifying
    public void updateRoles(@Nonnull List<String> roleList, @Nonnull User user, boolean force) {
        Collection<Role> granted = Optional.ofNullable(user.getRoles()).orElse(new ArrayList<>());
        val roles = new ArrayList<UserRole>();
        val tenantId = TenantManager.tenantId(0L);
        for (val rid : roleList) {
            Optional<Role> role = roleRepository.findByRoleAndTenantId(rid, tenantId);
            // 角色存在
            if (role.isPresent()) {
                val entity = role.get();
                if (!userRoleRepository.existsByUserAndRole(user, entity)) {
                    // 未授权
                    val userRole = new UserRole();
                    userRole.setTenantId(entity.getTenantId());
                    userRole.setRole(entity);
                    userRole.setUser(user);
                    roles.add(userRole);
                }
                if (!granted.isEmpty()) {
                    granted = granted.stream().filter((r) -> !r.getRole().equals(rid)).toList();
                }
            }
        }

        if (force && 1L != user.getId() && !granted.isEmpty()) {
            // 删除不再分配的角色
            for (Role role : granted) {
                userRoleRepository.removeAssignment(user.getId(), role.getId());
            }
        }

        if (!roles.isEmpty()) {
            userRoleRepository.saveAll(roles);
        }
    }

    @Override
    @Transactional
    @Modifying
    public GsvcExt.CommonRes assignRole(UpdateRoleRequest request) {
        val username = request.getUsername();
        val user = userRepository.findByUsername(username);
        if (user == null) {
            throw new GsvcException(new NotFoundError("{resource.user}", username));
        }
        val uid = user.getId();
        val rolesList = request.getRolesList();
        val tenantId = TenantManager.tenantId(0L);
        val roles = new ArrayList<UserRole>();
        for (String role : rolesList) {
            if (StringUtils.startsWith(role, "-")) {
                if (uid != 1L) {
                    val rid = role.substring(1);
                    Optional<Role> roleOpt = roleRepository.findByRoleAndTenantId(rid, tenantId);
                    roleOpt.ifPresent(value -> userRoleRepository.removeAssignment(uid, value.getId()));
                }
                else {
                    log.warn("Cannot reassign role to user with id {}", uid);
                }
            }
            else {
                Optional<Role> roleOpt = roleRepository.findByRoleAndTenantId(role, tenantId);
                if (roleOpt.isPresent() && !userRoleRepository.existsByUserAndRole(user, roleOpt.get())) {
                    val entity = roleOpt.get();
                    val userRole = new UserRole();
                    userRole.setTenantId(entity.getTenantId());
                    userRole.setRole(entity);
                    userRole.setUser(user);
                    roles.add(userRole);
                }
            }
        }

        if (!roles.isEmpty()) {
            userRoleRepository.saveAll(roles);
        }

        return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
    }

    // 根据推荐码计算推荐关系
    private void calculateReference(User user, String recCode, UcSetting setting) {
        if (StringUtils.isNotBlank(recCode)) {
            val opts = userRepository.findByRecommendCode(recCode);
            if (opts.isPresent()) {
                val referee = opts.get();
                val refereeId = referee.getId();
                val recCodeMaxReferenced = setting.getRecCodeMaxReferenced();
                if (recCodeMaxReferenced > 0) {
                    val count = userRepository.countByReferrerId(refereeId);
                    if (count >= recCodeMaxReferenced) {
                        throw new GsvcException(new RecCodeMaxReferencedExceeded());
                    }
                }
                val referrers = StringUtils.defaultIfBlank(referee.getReferrers(), "") + "/" + recCode;
                user.setReferrerId(refereeId);
                user.setReferrers(referrers);
                user.setReferrerLevel((short) (referee.getReferrerLevel() + 1));
                return;
            }
            else if (setting.isRecCodeNeed()) {
                throw new GsvcException(new RecCodeNotFoundError());
            }
        }
        else if (setting.isRecCodeNeed()) {
            throw new GsvcException(new RecCodeNotFoundError());
        }

        user.setReferrerId(0L);
        user.setReferrerLevel((short) 0);
        user.setReferrers("");
    }

    // 发布账户相关事件
    private void publishEvent(AccountEvent event) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(event);
        }
    }

    /**
     * 创建登录会话.
     * @param token Token
     * @param oauth Oauth
     * @return 会话实例.
     */
    @Nonnull
    private static OauthSession newOauthSession(AbstractAuthenticationToken token, Oauth oauth) {
        val session = new OauthSession();
        session.setOauth(oauth);
        session.setUser(oauth.getUser());
        session.setDevice(oauth.getDevice());
        session.setIp(oauth.getIp());
        session.setSimulator(SIMULATOR.equals(oauth.getDevice()));
        if (token instanceof JwtAuthenticationToken) {
            session.setAccessToken(((JwtAuthenticationToken) token).getJwtToken().getAccessToken());
        }
        session.setExpiration(0L);
        return session;
    }

}
