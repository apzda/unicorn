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

import com.apzda.cloud.uc.domain.entity.User;
import com.apzda.cloud.uc.proto.UserInfo;
import com.apzda.cloud.uc.proto.UserMeta;
import org.mapstruct.*;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = MetaTypeMapper.class)
public interface DomainEntityMapper {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "uid", source = "username")
    @Mapping(target = "password", source = "passwd")
    @Mapping(target = "metaList", source = "metas")
    UserInfo fromUserEntity(User user);

    @InheritInverseConfiguration
    @Mapping(target = "metas", ignore = true)
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    User fromUserInfo(UserInfo userInfo);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    UserMeta fromEntity(com.apzda.cloud.uc.domain.entity.UserMeta meta);

    List<UserMeta> fromEntities(List<com.apzda.cloud.uc.domain.entity.UserMeta> metas);

}
