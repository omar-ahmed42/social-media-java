package com.omarahmed42.socialmedia.mapper;

import org.mapstruct.Mapper;

import com.omarahmed42.socialmedia.dto.request.SignupRequest;
import com.omarahmed42.socialmedia.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(SignupRequest request);
}
