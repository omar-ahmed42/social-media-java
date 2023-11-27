package com.omarahmed42.socialmedia.projection;

import org.springframework.data.web.ProjectedPayload;

@ProjectedPayload
public interface CommentInputProjection {
    Long getId();

    Long getPostId();

    String getContent();

    String getCommentStatus();
}
