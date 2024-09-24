/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.config.domain.entity;

import com.apzda.cloud.gsvc.domain.TenantableEntity;
import com.apzda.cloud.gsvc.model.SoftDeletable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@Builder
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Slf4j
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE apzda_base_setting SET deleted = true WHERE id=? AND version=?")
@Table(name = "apzda_base_setting")
public class Setting extends TenantableEntity<Long, String, Long, String> implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long createdAt;

    private String createdBy;

    private Long updatedAt;

    private String updatedBy;

    private String tenantId;

    private boolean deleted;

    @Version
    private Short version;

    @NotNull
    @Column(length = 32, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String settingKey;

    @NotNull
    @Column(nullable = false)
    private String settingCls;

    private String setting;

}
