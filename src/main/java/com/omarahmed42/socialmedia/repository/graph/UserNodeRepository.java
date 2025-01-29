package com.omarahmed42.socialmedia.repository.graph;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.graph.UserNode;

@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {
        @Query(exists = true, value = """
                        RETURN exists(
                            (:User {userId: $user_id}) <-[:FRIEND_WITH]-> (:User {userId: $friend_id})
                            ) AS is_friend
                            """)
        boolean isFriend(@Param("user_id") Long userId, @Param("friend_id") Long friendId);

        @Query(exists = true, value = """
                        RETURN exists(
                            (:User {userId: $first_user_id}) <-[:BLOCKS]-> (:User {userId: $second_user_id})
                            ) AS is_blocked
                                """)
        boolean isBlocked(@Param("first_user_id") Long firstUserId, @Param("second_user_id") Long secondUserId);

        @Query("""
                        MATCH (user:User {userId: $user_id}) <-[:FRIEND_WITH]-> (friend:User)
                        RETURN friend
                        """)
        List<UserNode> findAllFriendsById(@Param("user_id") Long userId);

        @Query("""
                        MATCH (source: User {userId: $sourceNodeId})-[blocks:BLOCKS]-> (target: User {userId: $targetNodeId})
                        DELETE blocks
                        """)
        void deleteBlocksRelationshipBetween(Long sourceNodeId, Long targetNodeId);

        @Query("""
                        MATCH (user:User {userId: $user_id})<-[:FRIEND_WITH]->(friend:User)
                        WITH friend
                        ORDER BY friend.userId
                        SKIP $offset
                        LIMIT $page_size
                        RETURN friend;
                            """)
        List<UserNode> findFriends(@Param("user_id") Long userId, @Param("offset") Integer offset,
                        @Param("page_size") Integer limit);

        @Query("""
                        MATCH (user: User {userId: $user_id}) -[:BLOCKS]-> (blocked: User)
                        WITH blocked
                        ORDER BY blocked.userId
                        RETURN blocked;
                        """)
        List<UserNode> findAllBlockedUsersBy(@Param("user_id") Long userId);

        @Query("""
                        MATCH (user: User{userId: $user_id})-[friend_with:FRIEND_WITH]-(friend: User) RETURN count(friend_with);
                                    """)
        long countFriends(@Param("user_id") Long userId);

        @Query("""
                        MATCH (user:User {userId: $userId})-[:FRIEND_WITH]->(friend:User)-[:FRIEND_WITH]->(fof:User)
                            WHERE NOT (user)-[:FRIEND_WITH]->(fof) AND user <> fof
                            AND NOT (user)-[:BLOCKS]->(fof)
                            AND NOT (fof)-[:BLOCKS]->(user)
                            WITH fof, COUNT(friend) AS commonFriends
                            RETURN fof
                            ORDER BY commonFriends DESC
                            SKIP $offset
                            LIMIT $limit
                            """)
        List<UserNode> findRecommendedConnections(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("limit") Integer limit);
}
