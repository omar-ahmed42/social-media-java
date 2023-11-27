package com.omarahmed42.socialmedia.dto.response;

import java.io.Serializable;

import lombok.Data;

@Data
public class ReactionStatistics implements Serializable {
    private Long likeCount;
    private Long loveCount;
    private Long angryCount;
    private Long sadCount;
    private Long laughCount;
}
