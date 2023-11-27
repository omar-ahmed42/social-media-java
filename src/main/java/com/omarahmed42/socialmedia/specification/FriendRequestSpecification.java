package com.omarahmed42.socialmedia.specification;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.omarahmed42.socialmedia.enums.FriendRequestStatus;
import com.omarahmed42.socialmedia.model.FriendRequest;
import com.omarahmed42.socialmedia.model.FriendRequest_;
import com.omarahmed42.socialmedia.model.User_;

public class FriendRequestSpecification {
    private FriendRequestSpecification() {
    }

    public static Specification<FriendRequest> hasRequestStatus(FriendRequestStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get(FriendRequest_.requestStatus), status);
    }

    public static Specification<FriendRequest> inRequestStatuses(List<FriendRequestStatus> statuses) {
        return (root, query, cb) -> root.get(FriendRequest_.requestStatus).in(statuses);
    }

    public static Specification<FriendRequest> afterId(Long after) {
        return (root, query, cb) -> after == null ? cb.conjunction()
                : cb.greaterThan(root.get(FriendRequest_.id), after);
    }

    public static Specification<FriendRequest> beforeId(Long before) {
        return (root, query, cb) -> before == null ? cb.conjunction()
                : cb.lessThan(root.get(FriendRequest_.id), before);
    }

    public static Specification<FriendRequest> hasSenderId(Long senderId) {
        return (root, query, cb) -> senderId == null ? cb.conjunction()
                : cb.equal(root.get(FriendRequest_.sender).get(User_.id), senderId);
    }

    public static Specification<FriendRequest> hasReceiverId(Long receiverId) {
        return (root, query, cb) -> receiverId == null ? cb.conjunction()
                : cb.equal(root.get(FriendRequest_.receiver).get(User_.id), receiverId);
    }
}
