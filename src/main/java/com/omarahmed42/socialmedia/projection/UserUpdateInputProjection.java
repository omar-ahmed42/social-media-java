package com.omarahmed42.socialmedia.projection;

import org.springframework.data.web.ProjectedPayload;

@ProjectedPayload
public interface UserUpdateInputProjection {
    String getFirstName();

    String getLastName();

    String getBio();
}
