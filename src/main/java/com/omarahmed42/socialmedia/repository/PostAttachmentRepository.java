package com.omarahmed42.socialmedia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.PostAttachment;
import com.omarahmed42.socialmedia.model.PostAttachmentId;

@Repository
public interface PostAttachmentRepository extends JpaRepository<PostAttachment, PostAttachmentId> {

    List<PostAttachment> findAllByPostAttachmentIdPost(Post post);
    
    @EntityGraph(attributePaths = {"postAttachmentId.attachment"})
    List<PostAttachment> queryAllByPostAttachmentIdPost(Post post);
}
