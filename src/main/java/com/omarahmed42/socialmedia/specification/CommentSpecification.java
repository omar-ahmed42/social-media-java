package com.omarahmed42.socialmedia.specification;

import java.util.Collection;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.omarahmed42.socialmedia.enums.CommentStatus;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.Comment_;
import com.omarahmed42.socialmedia.model.Post_;
import com.omarahmed42.socialmedia.model.User_;

public class CommentSpecification {
    private CommentSpecification() {
    }

    public static Sort sortDescById() {
        return Sort.by(Comment_.ID).descending();
    }

    public static Specification<Comment> hasPostId(Long postId) {
        return (root, query, cb) -> postId == null ? cb.conjunction()
                : cb.equal(root.join(Comment_.post).get(Post_.id), postId);
    }

    public static Specification<Comment> hasCommentStatus(CommentStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get(Comment_.commentStatus), status);
    }

    public static Specification<Comment> inCommentStatuses(Collection<CommentStatus> statuses) {
        return (root, query, cb) -> statuses == null ? cb.conjunction() : root.get(Comment_.commentStatus).in(statuses);
    }

    public static Specification<Comment> afterId(Long after) {
        return (root, query, cb) -> after == null ? cb.conjunction()
                : cb.greaterThan(root.get(Comment_.id), after);
    }

    public static Specification<Comment> beforeId(Long before) {
        return (root, query, cb) -> before == null ? cb.conjunction()
                : cb.lessThan(root.get(Comment_.id), before);
    }

    public static Specification<Comment> hasAuthorId(Long authorId) {
        return (root, query, cb) -> authorId == null ? cb.conjunction()
                : cb.equal(root.join(Comment_.user).get(User_.id), authorId);
    }
}
