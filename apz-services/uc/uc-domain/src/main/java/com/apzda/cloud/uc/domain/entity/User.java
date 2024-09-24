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
package com.apzda.cloud.uc.domain.entity;

import com.apzda.cloud.gsvc.domain.AuditableEntity;
import com.apzda.cloud.gsvc.domain.SnowflakeIdGenerator;
import com.apzda.cloud.gsvc.model.SoftDeletable;
import com.apzda.cloud.uc.domain.vo.Gender;
import com.apzda.cloud.uc.domain.vo.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
@Setter
@Entity
@Table(name = "uc_user")
@Slf4j
@ToString
public class User extends AuditableEntity<Long, String, Long> implements SoftDeletable {

    @Id
    @GeneratedValue(generator = SnowflakeIdGenerator.NAME, strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "deleted")
    private boolean deleted;

    @Size(max = 32)
    @NotNull
    @Column(name = "username", nullable = false, length = 32)
    private String username;

    @Size(max = 64)
    @Column(name = "nickname", length = 64)
    private String nickname;

    @Size(max = 128)
    @Column(name = "first_name", length = 128)
    private String firstName;

    @Size(max = 128)
    @Column(name = "last_name", length = 128)
    private String lastName;

    @Size(max = 20)
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Size(max = 10)
    @Column(name = "phone_prefix", length = 10)
    private String phonePrefix;

    @Size(max = 256)
    @Column(name = "email", length = 256)
    private String email;

    @Size(max = 512)
    @NotNull
    @Column(name = "passwd", nullable = false, length = 512)
    private String passwd;

    @Size(max = 1024)
    @Column(name = "avatar", length = 1024)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @NotNull
    @Column(name = "referrer_id", nullable = false)
    private Long referrerId;

    @Size(max = 256)
    @Column(name = "referrers", length = 256)
    private String referrers;

    @Column(name = "referrer_level", nullable = false, columnDefinition = "tinyint unsigned")
    private Short referrerLevel;

    @Size(max = 32)
    @Column(name = "recommend_code", length = 32)
    private String recommendCode;

    @Size(max = 16)
    @Column(name = "channel", length = 16)
    private String channel;

    @Size(max = 256)
    @NotNull
    @Column(name = "ip", nullable = false, length = 256)
    private String ip;

    @Size(max = 24)
    @Column(name = "device", length = 24)
    private String device;

    @Size(max = 255)
    @Column(name = "remark")
    private String remark;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @ToString.Exclude
    private List<Oauth> oauth;

    // 角色
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "uc_user_role", joinColumns = @JoinColumn(name = "uid"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @ToString.Exclude
    private List<Role> roles;

    // 元数据
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @SQLRestriction("tenant_id = 0 or tenant_id is null")
    @ToString.Exclude
    private List<UserMeta> metas;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @ToString.Exclude
    private List<TenantUser> tenants;

    // 组织
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @ToString.Exclude
    private List<UserOrganization> organizations;

    // 部门
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @ToString.Exclude
    private List<UserDepartment> departments;

    // 工作岗位
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @ToString.Exclude
    private List<UserJob> jobs;

    // 多因素认证
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @ToString.Exclude
    private List<UserMfa> mfa;

    // 安全问题
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @ToString.Exclude
    private List<UserSecurityQA> qa;

    @NonNull
    public Collection<Role> allRoles(@NonNull Long tenantId) {
        val rs = new HashSet<Role>();
        val roleList = getRoles();
        if (!CollectionUtils.isEmpty(roleList)) {
            for (Role role : roleList) {
                if (tenantId.equals(role.getTenantId()) || role.getTenantId() == 0L) {
                    if (rs.contains(role)) {
                        continue;
                    }
                    rs.add(role);
                    val children = role.allChildren();
                    rs.addAll(children);
                }
            }
        }
        return rs;
    }

    /**
     * 仅获取分配的角色
     * @param tenantId 租户ID
     * @return 角色列表
     */
    public Collection<Role> getRoles(@NonNull Long tenantId) {
        val rs = new HashSet<Role>();
        val roleList = getRoles();
        if (!CollectionUtils.isEmpty(roleList)) {
            for (Role role : roleList) {
                if (tenantId.equals(role.getTenantId()) || role.getTenantId() == 0L) {
                    if (rs.contains(role)) {
                        continue;
                    }
                    rs.add(role);
                }
            }
        }
        return rs;
    }

    @NonNull
    public Collection<Privilege> privileges(@NonNull Long tenantId) {
        val privileges = new HashSet<Privilege>();
        for (Role role : allRoles(tenantId)) {
            val privilege = role.getPrivileges();
            privileges.addAll(privilege);
            log.trace("Merged role({}) privileges: {}", role, privilege);
        }
        return privileges;
    }

    public Optional<UserMeta> getMeta(String name) {
        val metas = getMetas();

        if (metas == null) {
            return Optional.empty();
        }
        return metas.stream().filter((meta) -> meta.getName().equals(name)).findFirst();
    }

    public Optional<UserMfa> getMfa(String type) {
        val mfa = getMfa();
        if (!CollectionUtils.isEmpty(mfa)) {
            return mfa.stream().filter(userMfa -> userMfa.getType().equals(type)).findFirst();
        }
        return Optional.empty();
    }

    public List<UserMfa> getEnabledMfa() {
        val mfa = getMfa();
        if (!CollectionUtils.isEmpty(mfa)) {
            return mfa.stream().filter(userMfa -> Boolean.TRUE.equals(userMfa.getEnabled())).toList();
        }
        return Collections.emptyList();
    }

}
