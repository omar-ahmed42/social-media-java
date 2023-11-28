package com.omarahmed42.socialmedia.projection;

import org.springframework.data.web.ProjectedPayload;

@ProjectedPayload
public interface ConversationDetailsProjection {
    String getName();

    Boolean isGroup();
}
