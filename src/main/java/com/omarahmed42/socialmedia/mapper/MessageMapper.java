package com.omarahmed42.socialmedia.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.omarahmed42.socialmedia.dto.event.PublishedMessage;
import com.omarahmed42.socialmedia.model.Message;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageMapper {

    @Mapping(source = "userId", target = "senderId")
    @Mapping(source = "id.messageId", target = "messageId")
    @Mapping(source = "id.conversationId", target = "conversationId")
    PublishedMessage toPublishedMessage(Message message);

    @InheritInverseConfiguration
    Message toMessage(PublishedMessage publishedMessage);

}
