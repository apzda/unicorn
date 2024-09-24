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
package com.apzda.cloud.uc.domain.mapper;

import com.apzda.cloud.uc.domain.entity.Privilege;
import com.apzda.cloud.uc.domain.entity.SecurityResource;
import com.apzda.cloud.uc.proto.PrivilegeVo;
import com.apzda.cloud.uc.proto.ResourceVo;
import org.mapstruct.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface PrivilegeMapper {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    PrivilegeVo fromEntity(Privilege privilege);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "id", source = "rid")
    ResourceVo fromResource(SecurityResource resource);

}
