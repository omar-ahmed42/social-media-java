package com.omarahmed42.socialmedia.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.omarahmed42.socialmedia.dto.projection.UserPersonalInfoDto;
import com.omarahmed42.socialmedia.dto.projection.UserPublicInfoDto;
import com.omarahmed42.socialmedia.dto.request.SignupRequest;
import com.omarahmed42.socialmedia.model.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toEntity(SignupRequest request);
    User toEntity(UserPublicInfoDto dto);
    User toEntity(UserPersonalInfoDto dto);
}
