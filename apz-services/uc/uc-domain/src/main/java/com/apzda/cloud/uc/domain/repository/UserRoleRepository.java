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
package com.apzda.cloud.uc.domain.repository;

import com.apzda.cloud.uc.domain.entity.Role;
import com.apzda.cloud.uc.domain.entity.User;
import com.apzda.cloud.uc.domain.entity.UserRole;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface UserRoleRepository extends CrudRepository<UserRole, Long> {

    long deleteByUser(User user);

    boolean existsByUserAndRole(User user, Role role);

    @Query("delete from UserRole where user.id = ?1 and role.id = ?2")
    @Modifying
    void removeAssignment(Long uid, Long rid);

}
