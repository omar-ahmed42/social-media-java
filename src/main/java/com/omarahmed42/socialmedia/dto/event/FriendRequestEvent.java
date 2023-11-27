package com.omarahmed42.socialmedia.dto.event;

import java.io.Serializable;

import com.omarahmed42.socialmedia.enums.FriendRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendRequestEvent implements Serializable {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private FriendRequestStatus requestStatus;
    
}
