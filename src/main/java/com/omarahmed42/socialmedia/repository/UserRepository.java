package com.omarahmed42.socialmedia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.dto.projection.UserPersonalInfoDto;
import com.omarahmed42.socialmedia.dto.projection.UserPublicInfoDto;
import com.omarahmed42.socialmedia.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = { "roles" })
    Optional<User> findByEmail(String username);

    boolean existsByEmail(String email);

    @Query(value = """
        SELECT new com.omarahmed42.socialmedia.dto.projection.UserPersonalInfoDto(u.id, u.firstName, u.lastName, u.dateOfBirth,
         u.email, u.createdAt, new com.omarahmed42.socialmedia.dto.projection.AttachmentDto(avatar.id, avatar.url), 
         new com.omarahmed42.socialmedia.dto.projection.AttachmentDto(coverPicture.id, coverPicture.url)
        ) FROM User u 
        LEFT JOIN Attachment avatar
             ON avatar.id = u.avatar.id 
        LEFT JOIN Attachment coverPicture
         ON coverPicture.id = u.coverPicture.id
         WHERE u.id = :userId
            """)
    Optional<UserPersonalInfoDto> findUserPersonalInfoById(@Param("userId") Long userId);

    @Query(value = """
        SELECT new com.omarahmed42.socialmedia.dto.projection.UserPublicInfoDto(u.id, u.firstName, u.lastName, 
        u.dateOfBirth, u.createdAt, 
        new com.omarahmed42.socialmedia.dto.projection.AttachmentDto(avatar.id, avatar.url),
        new com.omarahmed42.socialmedia.dto.projection.AttachmentDto(coverPicture.id, coverPicture.url)) 
        FROM User u
        LEFT JOIN Attachment avatar
             ON avatar.id = u.avatar.id
        LEFT JOIN Attachment coverPicture
         ON coverPicture.id = u.coverPicture.id 
         WHERE u.id = :userId
                """)
    Optional<UserPublicInfoDto> findUserPublicInfoById(@Param("userId") Long userId);
}
