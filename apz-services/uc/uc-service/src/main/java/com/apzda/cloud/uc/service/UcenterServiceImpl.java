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

import com.apzda.cloud.audit.aop.AuditLog;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.uc.UserMetas;
import com.apzda.cloud.uc.domain.entity.Privilege;
import com.apzda.cloud.uc.domain.entity.Tenant;
import com.apzda.cloud.uc.domain.entity.TenantUser;
import com.apzda.cloud.uc.domain.entity.User;
import com.apzda.cloud.uc.domain.mapper.MetaTypeMapper;
import com.apzda.cloud.uc.domain.repository.TenantUserRepository;
import com.apzda.cloud.uc.domain.repository.UserRepository;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.domain.vo.UserStatus;
import com.apzda.cloud.uc.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class UcenterServiceImpl implements UcenterService {

    private final UserManager userManager;

    private final UserRepository userRepository;

    private final MetaTypeMapper metaTypeMapper;

    private final TenantUserRepository tenantUserRepository;

    @Value("${apzda.cloud.security.role-prefix:ROLE_}")
    private String rolePrefix;

    @Override
    @Transactional
    public UserInfo getUserInfo(Request request) {
        val builder = UserInfo.newBuilder();
        try {
            builder.setErrCode(0);
            val username = request.getUsername();
            val user = userManager.getUserByUsername(username);
            builder.setId(user.getId());
            builder.setUid(username);
            builder.setUsername(username);
            val status = user.getStatus();
            builder.setStatus(status.name());
            builder.setEnabled(status == UserStatus.ACTIVATED || status == UserStatus.PENDING);
            builder.setAccountNonLocked(status != UserStatus.LOCKED);
            builder.setAccountNonExpired(status != UserStatus.EXPIRED);
            builder.setCredentialsNonExpired(userManager.isCredentialsExpired(user));
            if (request.hasAll() && request.getAll()) {
                val metas = getMetas(request);
                builder.addAllMeta(metas.getMetaList());
                val org = getOrganizations(request);
                builder.addAllOrg(org.getOrgList());
                val authorities = getAuthorities(request);
                builder.addAllAuthority(authorities.getAuthorityList());
            }
            if (StringUtils.isNotBlank(user.getAvatar())) {
                builder.setAvatar(user.getAvatar());
            }
            if (StringUtils.isNotBlank(user.getPhoneNumber())) {
                if (StringUtils.isNotBlank(user.getPhonePrefix())) {
                    builder.setPhone(user.getPhonePrefix() + " " + user.getPhoneNumber());
                }
                else {
                    builder.setPhone(user.getPhoneNumber());
                }
            }
            if (StringUtils.isNotBlank(user.getEmail())) {
                builder.setEmail(user.getEmail());
            }
            if (StringUtils.isNotBlank(user.getFirstName())) {
                builder.setFirstName(user.getFirstName());
            }
            if (StringUtils.isNotBlank(user.getLastName())) {
                builder.setLastName(user.getLastName());
            }
            if (StringUtils.isNotBlank(user.getRecommendCode())) {
                builder.setRecommendCode(user.getRecommendCode());
            }
            if (user.getReferrerId() != null) {
                builder.setReferrerId(user.getReferrerId());
            }
            if (user.getReferrerLevel() != null) {
                builder.setReferrerLevel(user.getReferrerLevel());
            }
            if (user.getChannel() != null) {
                builder.setChannel(user.getChannel());
            }
            if (user.getDevice() != null) {
                builder.setDevice(user.getDevice());
            }
            if (user.getIp() != null) {
                builder.setIp(user.getIp());
            }
            if (StringUtils.isNotBlank(user.getRemark())) {
                builder.setRemark(user.getRemark());
            }

            val tenantId = getTenantId(request);
            val roles = user.allRoles(tenantId);
            for (val role : roles) {
                val rb = com.apzda.cloud.uc.proto.RoleVo.newBuilder();
                rb.setId(String.valueOf(role.getId()));
                rb.setRole(role.getRole());
                rb.setName(role.getName());
                rb.setDescription(role.getDescription());
                rb.setBuiltin(Boolean.TRUE.equals(role.getBuiltin()));
                rb.setTenantId(role.getTenantId() == null ? "0" : String.valueOf(role.getTenantId()));
                builder.addRole(rb);
            }
        }
        catch (UsernameNotFoundException e) {
            builder.setErrCode(404);
            builder.setErrMsg("User not found");
        }
        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo query(Query request) {
        Optional<User> user = Optional.empty();
        if (request.hasRecCode()) {
            user = userRepository.findByRecommendCode(request.getRecCode());
        }
        else if (request.hasEmail()) {
            user = userRepository.findByEmail(request.getEmail());
        }
        else if (request.hasId()) {
            user = userRepository.findById(request.getId());
        }
        else if (request.hasPhone()) {
            user = userRepository.findByPhoneNumberAndPhonePrefix(request.getPhone(), request.getIac());
        }

        if (user.isEmpty()) {
            UserInfo.Builder builder = UserInfo.newBuilder();
            builder.setErrCode(404);
            return builder.build();
        }

        val req = Request.newBuilder().setUsername(user.get().getUsername()).build();

        return getUserInfo(req);
    }

    @Override
    @Transactional
    public UserMetaResp getMetas(Request request) {
        val metaName = request.getMetaName();
        val username = request.getUsername();
        if (UserMetas.CURRENT_TENANT_ID.equals(metaName)) {
            val builder = UserMeta.newBuilder();
            builder.setName(UserMetas.CURRENT_TENANT_ID);
            builder.setValue(calculateUserTenantIds(username));
            builder.setType(MetaValueType.STRING);
            return UserMetaResp.newBuilder().setErrCode(0).addMeta(builder).build();
        }

        val tenantId = getTenantId(request);
        if (tenantId.equals(0L)) {
            return getPlatformUserMeta(request);
        }
        else {
            return getTenantUserMeta(request, tenantId);
        }
    }

    @Override
    @Transactional
    public AuthorityResp getAuthorities(Request request) {
        val builder = AuthorityResp.newBuilder().setErrCode(0);
        try {
            val username = request.getUsername();
            val tenantId = getTenantId(request);
            val user = userManager.getUserByUsername(username);
            val authorities = new HashSet<String>();
            val roles = user.allRoles(tenantId);
            val privileges = user.privileges(tenantId);
            for (val role : roles) {
                authorities.add(rolePrefix + role.getRole());
            }
            for (Privilege privilege : privileges) {
                authorities.add(privilege.getPermission());
            }
            builder.addAllAuthority(authorities);
        }
        catch (Exception e) {
            log.debug("User({}) could be external, should be bound to a internal one", request.getUsername());
        }
        return builder.build();
    }

    @Override
    @Transactional
    public OrganizationResp getOrganizations(Request request) {
        val builder = OrganizationResp.newBuilder().setErrCode(0);
        try {
            val username = request.getUsername();
            val user = userManager.getUserByUsername(username);
            val jobs = user.getJobs();
            if (!CollectionUtils.isEmpty(jobs)) {
                for (val job : jobs) {
                    val department = job.getDepartment();
                    val organization = department.getOrganization();

                    val orgBuilder = Organization.newBuilder();
                    orgBuilder.setName(organization.getName());
                    orgBuilder.setDesc(organization.getRemark());
                    orgBuilder.setId(String.valueOf(organization.getId()));
                    orgBuilder.setIcon(organization.getIcon());
                    orgBuilder.setJobLevel(job.getJob().getLevel().getLevel());
                    orgBuilder.setJobTitle(job.getJob().getName());

                    val departments = organization.getDepartments();
                    builder.addOrg(orgBuilder);
                }
            }
        }
        catch (UsernameNotFoundException e) {
            builder.setErrCode(404);
            builder.setErrMsg(e.getMessage());
        }
        return builder.build();
    }

    @Override
    @AuditLog(activity = "开通账号", template = "账户'{}'开通成功", errorTpl = "账户'{}'开通失败: {}",
            args = { "#request.username", "#throwExp?.message" })
    public OpenAccountResponse openAccount(OpenAccountRequest request) {
        return userManager.openAccount(request);
    }

    @Override
    @AuditLog(activity = "重置密码", template = "账户'{}'密码重置成功", errorTpl = "账户'{}'密码重置失败: {}",
            args = { "#request.username", "#throwExp?.message" })
    public GsvcExt.CommonRes resetPassword(ResetPasswordRequest request) {
        return userManager.resetPassword(request);
    }

    /**
     * 获取平台用户元数据.
     * @param request 请求.
     * @return 元数据.
     */
    private UserMetaResp getPlatformUserMeta(Request request) {
        val builder = UserMetaResp.newBuilder().setErrCode(0);
        try {
            val username = request.getUsername();
            val user = userManager.getUserByUsername(username);
            val metaName = request.getMetaName();
            val metas = user.getMetas();
            builder.addAllMeta(filterAndMapMetas(metas, metaName));
        }
        catch (UsernameNotFoundException e) {
            builder.setErrCode(404);
            builder.setErrMsg(e.getMessage());
        }
        return builder.build();
    }

    /**
     * 获取租户用户元数据.
     * @param request 请求.
     * @param tenantId 租户ID.
     * @return 元数据.
     */
    private UserMetaResp getTenantUserMeta(Request request, Long tenantId) {
        val builder = UserMetaResp.newBuilder().setErrCode(0);
        try {
            val username = request.getUsername();
            val user = userManager.getUserByUsername(username);
            val tenant = new Tenant();
            tenant.setId(tenantId);
            val tenantUser = tenantUserRepository.getByTenantAndUser(tenant, user);
            val metaName = request.getMetaName();
            val metas = tenantUser.allMetas();
            builder.addAllMeta(filterAndMapMetas(metas, metaName));
        }
        catch (UsernameNotFoundException e) {
            builder.setErrCode(404);
            builder.setErrMsg(e.getMessage());
        }
        return builder.build();
    }

    /**
     * 根据metaName过滤并转换结构.
     * @param metas 元数据集合.
     * @param metaName 用于过滤的元数据名称.
     * @return 元数据集合.
     */
    private Collection<UserMeta> filterAndMapMetas(Collection<com.apzda.cloud.uc.domain.entity.UserMeta> metas,
            String metaName) {
        if (metas == null) {
            return Collections.emptyList();
        }

        if (StringUtils.isBlank(metaName)) {
            return metas.stream().map(meta -> {
                val b = com.apzda.cloud.uc.proto.UserMeta.newBuilder();
                b.setName(meta.getName());
                b.setType(metaTypeMapper.fromMetaType(meta.getType()));
                b.setValue(meta.getValue());
                return b.build();
            }).toList();
        }
        else {
            return metas.stream().filter(meta -> meta.getName().equals(metaName)).map(meta -> {
                val b = com.apzda.cloud.uc.proto.UserMeta.newBuilder();
                b.setName(meta.getName());
                b.setType(metaTypeMapper.fromMetaType(meta.getType()));
                b.setValue(meta.getValue());
                return b.build();
            }).toList();
        }
    }

    /**
     * 计算用户所属租户列表，不区分登录设备。
     * @param username 用户名
     * @return 以逗号分隔的租户ID列表.
     */
    @NonNull
    private String calculateUserTenantIds(String username) {
        val user = userManager.getUserByUsername(username);
        val tenants = user.getTenants();
        val tenantIds = new HashSet<String>();
        val curTenantId = user.getMeta(UserMetas.CURRENT_TENANT_ID);
        if (!CollectionUtils.isEmpty(tenants)) {
            for (TenantUser tenant : tenants) {
                tenantIds.add(String.valueOf(tenant.getTenantId()));
            }
        }
        if (curTenantId.isPresent()) {
            val tid = curTenantId.get().getValue();
            tenantIds.remove(tid);
            return StringUtils.strip(tid + "," + String.join(",", tenantIds), ",");
        }
        return String.join(",", tenantIds);
    }

    /**
     * 根据请求获取租户ID.
     * @param request 请求.
     * @return 租房ID.
     */
    @NonNull
    private Long getTenantId(Request request) {
        return Long.parseLong(StringUtils.defaultIfBlank(request.getTenantId(), TenantManager.tenantId("0")));
    }

}
