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
import com.apzda.cloud.gsvc.model.Tenantable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
@Setter
@Entity
@Table(name = "uc_role")
@Slf4j
@ToString
public class Role extends AuditableEntity<Long, String, Long> implements Tenantable<Long>, SoftDeletable {

    @Id
    @GeneratedValue(generator = SnowflakeIdGenerator.NAME, strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "deleted")
    private boolean deleted;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Size(max = 32)
    @NotNull
    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Size(max = 128)
    @NotNull
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @NotNull
    @Column(name = "builtin")
    private Boolean builtin;

    @Size(max = 24)
    @NotNull
    @Column(name = "provider", nullable = false, length = 24)
    private String provider;

    @Column(name = "description")
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "uc_role_privilege", joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "privilege_id"))
    @ToString.Exclude
    private List<Privilege> privileges;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "uc_role_children", joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "child_id"))
    @ToString.Exclude
    private List<Role> children;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "uc_role_children", joinColumns = @JoinColumn(name = "child_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @ToString.Exclude
    private List<Role> parents;

    /**
     * 获取角色所有下级角色
     * @return 下级角色列表
     */
    @NonNull
    public Collection<Role> allChildren() {
        val roles = new HashSet<Role>();
        if (!CollectionUtils.isEmpty(getChildren())) {
            for (Role role : getChildren()) {
                if (!roles.contains(role)) {
                    roles.add(role);
                    val children = role.allChildren();
                    roles.addAll(children);
                    log.trace("Merged Role({}) and its children: {}", role, children);
                }
            }
        }
        return roles;
    }

    /**
     * 获取角色所有上级角色
     * @return 上级角色列表
     */
    @NonNull
    public Collection<Role> allParents() {
        val roles = new HashSet<Role>();
        if (!CollectionUtils.isEmpty(getParents())) {
            for (Role role : getParents()) {
                if (!roles.contains(role)) {
                    roles.add(role);
                    val parents = role.allParents();
                    roles.addAll(parents);
                    log.trace("Merged Role({}) and its parents: {}", role, parents);
                }
            }
        }
        return roles;
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return -1;
    }

    /**
     * 只要role和tenantId相同则角色相同.
     * @param obj 对比对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Role)) {
            return false;
        }
        val r = ((Role) obj).getRole();
        val tid = ((Role) obj).getTenantId();
        return this == obj || (Objects.equals(r, role) && Objects.equals(tid, tenantId));
    }

}
