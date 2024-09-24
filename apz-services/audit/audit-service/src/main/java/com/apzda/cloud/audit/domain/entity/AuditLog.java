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
package com.apzda.cloud.audit.domain.entity;

import com.apzda.cloud.gsvc.domain.AuditingEntityListener;
import com.apzda.cloud.gsvc.model.Auditable;
import com.apzda.cloud.gsvc.model.SoftDeletable;
import com.apzda.cloud.gsvc.model.Tenantable;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Entity
@Table(name = "apzda_audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
public class AuditLog implements Auditable<Long, String, Long>, Tenantable<String>, SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long createdAt;

    private String createdBy;

    private Long updatedAt;

    private String updatedBy;

    private String tenantId;

    private boolean deleted;

    private Boolean template;

    private String userId;

    private Long logTime;

    private String activity;

    private String runas;

    private String level;

    private String ip;

    private String device;

    private String message;

    private String args;

    private String oldValue;

    private String newValue;

}
