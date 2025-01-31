package com.omarahmed42.socialmedia.projection;

import org.springframework.data.web.ProjectedPayload;

@ProjectedPayload
public interface PostInputProjection {
    Long getId();

    String getContent();

    String getPostStatus();

    Long getParentId();
}
