package com.omarahmed42.socialmedia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.PostAttachment;
import com.omarahmed42.socialmedia.model.PostAttachmentId;

@Repository
public interface PostAttachmentRepository extends JpaRepository<PostAttachment, PostAttachmentId> {

}
