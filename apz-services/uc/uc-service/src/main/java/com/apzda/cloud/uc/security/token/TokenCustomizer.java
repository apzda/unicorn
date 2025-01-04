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
package com.apzda.cloud.uc.security.token;

import cn.hutool.core.bean.BeanUtil;
import com.apzda.cloud.config.exception.SettingUnavailableException;
import com.apzda.cloud.config.service.SettingService;
import com.apzda.cloud.gsvc.security.mfa.MfaStatus;
import com.apzda.cloud.gsvc.security.token.JwtToken;
import com.apzda.cloud.gsvc.security.token.JwtTokenCustomizer;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaService;
import com.apzda.cloud.uc.UserMetas;
import com.apzda.cloud.uc.config.UCenterConfig;
import com.apzda.cloud.uc.domain.entity.*;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.mapper.JwtTokenMapper;
import com.apzda.cloud.uc.setting.UcSetting;
import com.apzda.cloud.uc.token.UserToken;
import com.apzda.cloud.uc.vo.Department;
import com.apzda.cloud.uc.vo.Job;
import com.apzda.cloud.uc.vo.Organization;
import com.apzda.cloud.uc.vo.Role;
import com.apzda.cloud.uc.vo.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Optional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class TokenCustomizer implements JwtTokenCustomizer {

    private final static UserMeta emptyMeta = new UserMeta();

    private final UserManager userManager;

    private final JwtTokenMapper tokenMapper;

    private final UserDetailsMetaService userDetailsMetaService;

    private final SettingService settingService;

    private final UCenterConfig uCenterConfig;

    @Override
    @Transactional(readOnly = true)
    @NonNull
    public JwtToken customize(@NonNull Authentication authentication, @NonNull JwtToken token) {
        val userToken = tokenMapper.valueOf(token);
        val username = token.getName();
        String provider = token.getProvider();
        boolean loadSession = false;
        if (authentication.getPrincipal() instanceof UserDetailsMeta meta) {
            if (StringUtils.isBlank(provider)) {
                provider = meta.getProvider();
                loadSession = true;
            }

            val runAs = meta.get(UserMetas.RUNNING_AS, authentication);
            if (StringUtils.isNotBlank(runAs)) {
                userToken.setRunAs(runAs);
                loadSession = true;
            }
        }
        provider = StringUtils.defaultIfBlank(provider, Oauth.SIMPLE);
        val oauth = userManager.getOauthByUsernameAndProvider(username, provider);
        // 上一次登录会话
        if (loadSession) {
            val session = userManager.getLastLoginSession(oauth);
            if (session != null) {
                userToken.setLastLoginIp(session.getIp());
                userToken.setLastLoginTime(session.getCreatedAt());
            }
        }

        val user = oauth.getUser();
        try {
            userToken.setUid(String.valueOf(user.getId()));
            userToken.setStatus(user.getStatus().name());
            userToken.setDisplayName(user.getNickname());
            userToken.setFirstName(user.getFirstName());
            userToken.setLastName(user.getLastName());
            userToken.setAvatar(user.getAvatar());
            userToken.setEmail(user.getEmail());
            userToken.setPhone(user.getPhoneNumber());
            userToken.setPhonePrefix(user.getPhonePrefix());
            userToken.setTheme(user.getMeta(UserMetas.CURRENT_THEME_ID).orElse(emptyMeta).getValue());
            userToken.setLang(user.getMeta(UserMetas.LANGUAGE_KEY).orElse(emptyMeta).getValue());
            userToken.setTimezone(user.getMeta(UserMetas.TIMEZONE_KEY).orElse(emptyMeta).getValue());

            // 用户所属租户列表
            val tenants = user.getTenants();
            if (!CollectionUtils.isEmpty(tenants)) {
                calculateTenants(user, userToken);
                // 用户所属组织列表
                calculateOrganizations(user, userToken);
            }
            // 角色
            val tenant = Optional.ofNullable(userToken.getTenant());
            val tenantId = Long.parseLong(tenant.map(Tenant::getId).orElse("0"));
            userToken.setRoles(user.allRoles(tenantId).stream().map((role) -> {
                val r = new Role();
                r.setId(String.valueOf(role.getId()));
                r.setName(role.getName());
                r.setRole(role.getRole());
                return r;
            }).toList());

            // 账户主人才需要设置多因素认证
            if (authentication.getPrincipal() instanceof UserDetailsMeta meta
                    && StringUtils.isBlank(userToken.getRunAs())) {
                // 认证状态
                val mfaStatus = meta.get(UserDetailsMeta.MFA_STATUS_KEY, authentication);
                // 认证器
                val authenticator = uCenterConfig.getAuthenticator();
                if (authenticator != null && StringUtils.isBlank(mfaStatus)) {
                    UcSetting ucSetting = new UcSetting();
                    try {
                        ucSetting = settingService.load(UcSetting.class);
                    }
                    catch (SettingUnavailableException e) {
                        log.error("无法加载用户中心配置，使用默认配置: {} - {}", ucSetting, e.getMessage());
                    }
                    val mfa = user.getEnabledMfa();
                    if (!CollectionUtils.isEmpty(mfa)) {
                        // 等待验证
                        userToken.setMfa(MfaStatus.PENDING);
                    }
                    else if (ucSetting.isMfaEnabled()) {
                        // 需要配置多因素认证
                        userToken.setMfa(MfaStatus.UNSET);
                    }

                    if (StringUtils.isNotBlank(userToken.getMfa())) {
                        meta.set(UserDetailsMeta.MFA_STATUS_KEY, authentication, userToken.getMfa());
                    }
                }
            }
            // 用户权限列表
            val authorities = userDetailsMetaService.getAuthorities((UserDetails) authentication.getPrincipal());
            userToken.setAuthorities(authorities.stream().map(GrantedAuthority::getAuthority).toList());
        }
        catch (Exception e) {
            userToken.setUid("");
        }
        userToken.setProvider(provider);
        return userToken;
    }

    private static void calculateTenants(User user, UserToken userToken) {
        val tenantMap = new HashMap<String, Tenant>();
        val tenants = user.getTenants();
        val tenantId = user.getMeta(UserMetas.CURRENT_TENANT_ID).orElse(emptyMeta).getValue();

        for (TenantUser tenant : tenants) {
            val t = tenant.getTenant();
            if (t != null) {
                val org = new Tenant();
                org.setId(String.valueOf(t.getId()));
                org.setName(t.getName());
                if (org.getId().equals(tenantId)) {
                    org.setCurrent(true);
                    val currentTenant = BeanUtil.copyProperties(org, Tenant.class);
                    currentTenant.setOrganizations(null);
                    userToken.setTenant(currentTenant);
                }
                tenantMap.put(org.getId(), org);
            }
        }

        userToken.setTenants(tenantMap);
    }

    private static void calculateOrganizations(User user, UserToken userToken) {
        val organizations = user.getOrganizations();
        val curOrgId = user.getMeta(UserMetas.CURRENT_ORG_ID).orElse(emptyMeta).getValue();
        val curDeptId = user.getMeta(UserMetas.CURRENT_DEPT_ID).orElse(emptyMeta).getValue();
        val curJobId = user.getMeta(UserMetas.CURRENT_JOB_ID).orElse(emptyMeta).getValue();
        if (!CollectionUtils.isEmpty(organizations)) {
            val tenantsMap = userToken.getTenants();
            for (UserOrganization organization : organizations) {
                val tenantId = organization.getTenantId();
                if (tenantId == null) {
                    continue;
                }
                val org = organization.getOrganization();
                val orgVo = new Organization();
                orgVo.setId(String.valueOf(org.getId()));
                orgVo.setName(org.getName());
                orgVo.setDesc(org.getRemark());
                orgVo.setIcon(org.getIcon());
                if (orgVo.getId().equals(curOrgId)) {
                    orgVo.setCurrent(true);
                    val currentOrg = BeanUtil.copyProperties(orgVo, Organization.class);
                    currentOrg.setDepartments(null);
                    userToken.setOrganization(currentOrg);
                }
                tenantsMap.computeIfPresent(String.valueOf(org.getTenantId()), (key, value) -> {
                    value.getOrganizations().put(orgVo.getId(), orgVo);
                    return value;
                });
            }

            val departs = user.getDepartments();
            if (!CollectionUtils.isEmpty(departs)) {
                for (UserDepartment depart : departs) {
                    val tenantId = depart.getTenantId();
                    if (tenantId == null) {
                        continue;
                    }
                    val department = depart.getDepartment();
                    val organization = department.getOrganization();
                    val d = new com.apzda.cloud.uc.vo.Department();
                    d.setId(String.valueOf(department.getId()));
                    d.setName(department.getName());
                    d.setIcon(department.getIcon());
                    d.setDesc(department.getRemark());
                    if (d.getId().equals(curDeptId)) {
                        d.setCurrent(true);
                        val currentDepart = BeanUtil.copyProperties(d, Department.class);
                        currentDepart.setJobs(null);
                        userToken.setDepartment(currentDepart);
                    }
                    tenantsMap.computeIfPresent(String.valueOf(tenantId), (key, value) -> {
                        value.getOrganizations().computeIfPresent(String.valueOf(organization.getId()), (k1, v1) -> {
                            v1.getDepartments().put(d.getId(), d);
                            return v1;
                        });
                        return value;
                    });
                }
            }

            val jobs = user.getJobs();
            if (!CollectionUtils.isEmpty(jobs)) {
                for (UserJob userJob : jobs) {
                    val tenantId = userJob.getTenantId();
                    if (tenantId == null) {
                        continue;
                    }
                    val deprtJob = userJob.getJob();
                    val department = userJob.getDepartment();
                    val organization = department.getOrganization();
                    val job = new Job();
                    job.setId(String.valueOf(userJob.getId()));
                    job.setName(deprtJob.getName());
                    job.setLevel(deprtJob.getLevel().getLevel());
                    job.setTitle(deprtJob.getLevel().getName());
                    job.setIcon(deprtJob.getIcon());
                    if (job.getId().equals(curJobId)) {
                        job.setCurrent(true);
                        userToken.setJob(BeanUtil.copyProperties(job, Job.class));
                    }
                    tenantsMap.computeIfPresent(String.valueOf(tenantId), (key, value) -> {
                        value.getOrganizations().computeIfPresent(String.valueOf(organization.getId()), (k1, v1) -> {
                            v1.getDepartments().computeIfPresent(String.valueOf(department.getId()), (k2, v2) -> {
                                v2.getJobs().put(job.getId(), job);
                                return v2;
                            });
                            return v1;
                        });
                        return value;
                    });
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

}
