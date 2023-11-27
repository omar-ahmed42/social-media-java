package com.omarahmed42.socialmedia.model.cache;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Newsfeed implements Serializable {
    private Long userId;
    private Long postId;

    public Newsfeed(Long userId, Long postId) {
        this.userId = userId;
        this.postId = postId;
    }
}
