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
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
@Setter
@Entity
@Table(name = "uc_tenant_user")
@ToString
public class TenantUser extends AuditableEntity<Long, String, Long> implements Tenantable<Long>, SoftDeletable {

    @Id
    @GeneratedValue(generator = SnowflakeIdGenerator.NAME, strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "deleted")
    private boolean deleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @ToString.Exclude
    private Tenant tenant;

    @Column(name = "tenant_id")
    public Long getTenantId() {
        if (Optional.ofNullable(getTenant()).isPresent()) {
            return getTenant().getId();
        }
        return null;
    }

    public void setTenantId(Long tenantId) {
        throw new IllegalStateException("Use setTenant() instead");
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid")
    @ToString.Exclude
    private User user;

    @NotNull
    @Column(name = "sa", nullable = false)
    private Boolean sa = false;

    // 元数据
    @OneToMany(fetch = FetchType.LAZY,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH })
    @JoinColumns({ @JoinColumn(name = "uid", referencedColumnName = "uid"),
            @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id") })
    @ToString.Exclude
    private List<UserMeta> metas;

    @NonNull
    public Set<UserMeta> allMetas() {
        val userMetas = new HashSet<UserMeta>();
        val tenantUserMeta = getMetas();
        if (tenantUserMeta != null) {
            userMetas.addAll(tenantUserMeta);
        }
        val userDefaultMetas = user.getMetas();
        if (userDefaultMetas != null) {
            userMetas.addAll(userDefaultMetas);
        }
        return userMetas;
    }

}
