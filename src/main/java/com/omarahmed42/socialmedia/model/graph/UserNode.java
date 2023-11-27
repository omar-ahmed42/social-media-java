package com.omarahmed42.socialmedia.model.graph;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import jakarta.persistence.Column;
import lombok.Getter;

@Node("User")
@Getter
public class UserNode {
    @Id
    private final Long userId;

    @Relationship(type = "FRIEND_WITH", direction = Direction.OUTGOING)
    private List<UserNode> friends = new ArrayList<>();

    @Relationship(type = "BLOCKS", direction = Direction.OUTGOING)
    private List<UserNode> blockedUsers = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_at", insertable = false)
    private LocalDateTime lastModifiedAt;

    public UserNode(Long userId) {
        this.userId = userId;
    }

    public void addFriend(UserNode userNode) {
        if (userNode == null)
            return;
        friends.add(userNode);
    }

    public void addBlockedUser(UserNode userNode) {
        if (userNode == null)
            return;

        blockedUsers.add(userNode);
    }

    public void removeBlockedUser(UserNode userNode) {
        if (userNode == null || blockedUsers == null)
            return;

        blockedUsers.removeIf(u -> u.getUserId().equals(userNode.getUserId()));
    }
}
