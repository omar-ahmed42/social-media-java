package com.omarahmed42.socialmedia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.enums.FriendRequestStatus;
import com.omarahmed42.socialmedia.model.FriendRequest;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    List<FriendRequest> findAll(Specification<FriendRequest> specification, Pageable pageable);

    @Query("SELECT fr FROM FriendRequest fr WHERE (((fr.sender.id = :auth_user_id AND fr.receiver.id = :friend_id) OR (fr.sender.id = :friend_id AND fr.receiver.id = :auth_user_id)) AND fr.requestStatus = :status)")
    Optional<FriendRequest> findByFriendRequestStatus(@Param("auth_user_id") Long authenticatedUserId,
            @Param("friend_id") Long friendId,
            @Param("status") FriendRequestStatus status);
}
