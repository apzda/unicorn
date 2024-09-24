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
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
@Setter
@Entity
@Table(name = "uc_oauth")
@ToString
public class Oauth extends AuditableEntity<Long, String, Long> implements SoftDeletable {

    public static final String SIMPLE = "db";

    public static final String PHONE = "phone";

    public static final String EMAIL = "email";

    public static final String LDAP = "ldap";

    @Id
    @GeneratedValue(generator = SnowflakeIdGenerator.NAME, strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "deleted")
    private boolean deleted;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false)
    @ToString.Exclude
    private User user;

    @Size(max = 24)
    @NotNull
    @Column(name = "provider", nullable = false, length = 24)
    private String provider;

    @Size(max = 256)
    @NotNull
    @Column(name = "open_id", nullable = false, length = 256)
    private String openId;

    @Size(max = 256)
    @NotNull
    @Column(name = "union_id", nullable = false, length = 256)
    private String unionId;

    @Size(max = 32)
    @Column(name = "recommend_code", length = 32)
    private String recCode;

    @NotNull
    @Column(name = "login_time", nullable = false)
    private Long loginTime;

    @Size(max = 24)
    @NotNull
    @Column(name = "device", nullable = false, length = 24)
    private String device;

    @Size(max = 256)
    @NotNull
    @Column(name = "ip", nullable = false, length = 256)
    private String ip;

    @NotNull
    @Column(name = "last_login_time", nullable = false)
    private Long lastLoginTime;

    @Size(max = 24)
    @NotNull
    @Column(name = "last_device", nullable = false, length = 24)
    private String lastDevice;

    @Size(max = 256)
    @NotNull
    @Column(name = "last_ip", nullable = false, length = 256)
    private String lastIp;

    @Size(max = 255)
    @Column(name = "remark")
    private String remark;

}
