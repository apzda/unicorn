package com.apzda.cloud.uc.mapper;

import com.apzda.cloud.gsvc.security.token.JwtToken;
import com.apzda.cloud.uc.token.UserToken;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface JwtTokenMapper {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    UserToken valueOf(JwtToken token);

}
