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
import com.apzda.cloud.uc.domain.vo.MetaType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.val;

import java.util.Objects;
import java.util.Optional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
@Setter
@Entity
@Table(name = "uc_user_meta")
@ToString
public class UserMeta extends AuditableEntity<Long, String, Long> implements Tenantable<Long>, SoftDeletable {

    public static final String CREDENTIALS_EXPIRED_AT = "credentials_expired_at";

    public static final String EMAIL_ACTIVATED_AT = "email_activated_at";

    @Id
    @GeneratedValue(generator = SnowflakeIdGenerator.NAME, strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "deleted")
    private boolean deleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private MetaType type;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid")
    @ToString.Exclude
    private User user;

    @Size(max = 32)
    @NotNull
    @Column(name = "name", nullable = false, length = 32)
    private String name;

    @Lob
    @Column(name = "value", columnDefinition = "LONGTEXT")
    private String value;

    @Column(name = "remark")
    private String remark;

    @Override
    public int hashCode() {
        val user = Optional.ofNullable(getUser()).orElse(new User());
        return (user.getId() + "@" + name).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserMeta)) {
            return false;
        }
        val u1 = Optional.ofNullable(getUser()).orElse(new User());
        val u2 = Optional.ofNullable(((UserMeta) obj).getUser()).orElse(new User());
        return obj == this
                || (Objects.equals(u1.getId(), u2.getId()) && Objects.equals(name, ((UserMeta) obj).getName()));
    }

}
