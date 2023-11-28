package com.omarahmed42.socialmedia.service.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.datastax.oss.driver.shaded.guava.common.collect.Sets;
import com.omarahmed42.socialmedia.dto.event.PublishedMessage;
import com.omarahmed42.socialmedia.enums.MessageStatus;
import com.omarahmed42.socialmedia.exception.ConversationNotFoundException;
import com.omarahmed42.socialmedia.exception.ForbiddenConversationAccessException;
import com.omarahmed42.socialmedia.exception.InternalServerErrorException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.mapper.MessageMapper;
import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.ConversationMember;
import com.omarahmed42.socialmedia.model.Message;
import com.omarahmed42.socialmedia.model.MessageId;
import com.omarahmed42.socialmedia.projection.ConversationDetailsProjection;
import com.omarahmed42.socialmedia.repository.ConversationRepository;
import com.omarahmed42.socialmedia.repository.MessageRepository;
import com.omarahmed42.socialmedia.service.ConversationService;
import com.omarahmed42.socialmedia.service.FileService;
import com.omarahmed42.socialmedia.service.MessageService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    private final FileService fileService;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private static final String EXTENSION_SEPARATOR = ".";

    private final MessageRepository messageRepository;

    @Value("${storage.messages.path}")
    private String storagePath;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final Sinks.Many<PublishedMessage> sink;

    private final MessageMapper messageMapper;

    public MessageServiceImpl(ConversationRepository conversationRepository, ConversationService conversationService,
            MessageRepository messageRepository, KafkaTemplate<String, Object> kafkaTemplate,
            MessageMapper messageMapper, FileService fileService) {
        this.conversationRepository = conversationRepository;
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.messageMapper = messageMapper;
        this.fileService = fileService;
    }

    @Override
    public Message addPersonalMessage(MultipartFile multipartFile, Long receiverId, String content) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

        Conversation conversation = conversationRepository.findPersonalConversationBy(authenticatedUserId, receiverId)
                .orElseGet(
                        () -> conversationService.addConversation(authenticatedUserId,
                                createConversationDetailsProjection(null, Boolean.FALSE),
                                Sets.newHashSet(receiverId)));

        if (multipartFile == null && StringUtils.isBlank(content))
            throw new InvalidInputException("Message cannot be empty");

        Message message = storeMessageAndFile(multipartFile, conversation.getId(), MessageStatus.SENT,
                authenticatedUserId,
                content);

        kafkaTemplate.send("messages", conversation.getId().toString(),
                toPublishedMessage(message, Set.of(authenticatedUserId, receiverId)));
        return message;
    }

    private Message storeMessageAndFile(MultipartFile multipartFile, Long conversationId, MessageStatus status,
            Long userId,
            String content) {
        Message message = new Message();
        if (multipartFile != null) {
            String attachmentUrl = storeFile(multipartFile, conversationId);
            message.setAttachmentUrl(attachmentUrl);
        }

        message.setId(new MessageId(generateId(), conversationId));
        // message.setMessageId(generateId());
        message.setMessageStatus(status);
        message.setUserId(userId);
        message.setContent(content);
        // message.setConversationId(conversationId);
        message = messageRepository.save(message);

        return message;
    }

    @Override
    public Message addMessage(MultipartFile multipartFile, Long conversationId, String content) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

        if (conversationId == null)
            throw new InvalidInputException("Conversation id cannot be null");

        Conversation conversation = conversationRepository.findConversationById(conversationId)
                .orElseThrow(ConversationNotFoundException::new);

        if (!isMemberOf(authenticatedUserId, conversation.getConversationMembers()))
            throw new ForbiddenConversationAccessException(
                    "Forbidden: cannot send a message to conversation with id " + conversationId);

        if (multipartFile == null && StringUtils.isBlank(content))
            throw new InvalidInputException("Message cannot be empty");

        Message message = storeMessageAndFile(multipartFile, conversationId, MessageStatus.SENT, authenticatedUserId,
                content);

        Set<Long> membersIds = conversation.getConversationMembers()
                .stream()
                .map(m -> m.getUser().getId())
                .distinct()
                .collect(Collectors.toSet());
        kafkaTemplate.send("messages", toPublishedMessage(message, membersIds));
        return message;
    }

    private ConversationDetailsProjection createConversationDetailsProjection(String name, Boolean isGroup) {
        return new ConversationDetailsProjection() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Boolean isGroup() {
                return isGroup;
            }
        };
    }

    private Long generateId() {
        // Will be replaced with an API call to get a distributed UID
        return RandomUtils.nextLong(1L, Long.MAX_VALUE);
    }

    private String storeFile(MultipartFile multipartFile, Long conversationId) {
        try {
            String conversationPath = storagePath + File.separator + conversationId;
            File conversationDirectory = new File(conversationPath);
            FileUtils.forceMkdir(conversationDirectory);

            String extension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
            String filename = UUID.randomUUID().toString() + EXTENSION_SEPARATOR + extension;

            String fileUrl = conversationPath + File.separator + filename;
            fileService.copy(multipartFile.getInputStream(), Path.of(fileUrl));
            return fileUrl;
        } catch (Exception e) {
            throw new InternalServerErrorException("Error while storing file");
        }
    }

    private boolean isMemberOf(Long authenticatedUserId, Set<ConversationMember> conversationMembers) {
        for (ConversationMember conversationMember : conversationMembers) {
            if (conversationMember.getUser().getId().equals(authenticatedUserId))
                return true;
        }

        return false;
    }

    @KafkaListener(topics = "messages")
    public void receiveMessages(ConsumerRecord<String, Object> consumerRecord) {
        PublishedMessage message = (PublishedMessage) consumerRecord.value();
        publish(message);
    }

    private void publish(PublishedMessage message) {
        EmitResult result = sink.tryEmitNext(message);
        if (result.isFailure()) {
            log.error("Error emitting message with id " + message.getMessageId());
        }
    }

    @Override
    public Publisher<Message> receiveMessagesPublisher() {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

        return sink.asFlux()
                .filter(m -> m.getMemberIds().contains(authenticatedUserId))
                .map(message -> {
                    log.info("Publishing message with id {}", message.getMessageId());
                    return messageMapper.toMessage(message);
                });
    }

    @Override
    public Publisher<Message> receiveMessagesPublisher(Long conversationId) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

        log.info("Retrieving conversation with id {}", conversationId);
        Conversation conversation = conversationRepository.findConversationById(conversationId)
                .orElseThrow(ConversationNotFoundException::new);

        log.info("Conversation {} retrieved", conversationId);
        if (!isMemberOf(authenticatedUserId, conversation.getConversationMembers()))
            throw new ForbiddenConversationAccessException(
                    "Forbidden: cannot receive message from conversation with id " + conversationId);

        return sink.asFlux()
                .filter(message -> message.getConversationId().equals(conversationId))
                .filter(message -> message.getMemberIds().contains(authenticatedUserId))
                .map(message -> {
                    log.info("Publishing message with id {} for conversation {}", message.getMessageId(),
                            message.getConversationId());
                    return messageMapper.toMessage(message);
                });
    }

    private PublishedMessage toPublishedMessage(Message message, Set<Long> memberIds) {
        log.info("Converting to published message");
        PublishedMessage publishedMessage = messageMapper.toPublishedMessage(message);
        if (memberIds == null || memberIds.isEmpty())
            return publishedMessage;

        publishedMessage.getMemberIds().addAll(memberIds);
        return publishedMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation getConversationBy(Message message) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        Long conversationId = message.getConversationId();
        Conversation conversation = conversationRepository.findConversationById(conversationId)
                .orElseThrow(ConversationNotFoundException::new);
        Set<ConversationMember> conversationMembers = conversation.getConversationMembers();

        if (isMemberOf(authenticatedUserId, conversationMembers)) {
            return conversation;
        }

        throw new ForbiddenConversationAccessException(
                "Forbidden: cannot access conversation using message with id" + message.getMessageId());
    }

    @Override
    public List<Message> findMessages(Long conversationId, Long after, Long before) {
        List<Message> messages;
        Pageable pageable;
        if (after != null) {
            pageable = PageRequest.of(0, 15, Sort.by(Direction.ASC, "id.messageId"));
            messages = messageRepository.findAllByIdConversationIdAndIdMessageIdGreaterThan(conversationId, after, pageable);
        } else if (before != null) {
            pageable = PageRequest.of(0, 15, Sort.by(Direction.DESC, "id.messageId"));
            messages = messageRepository.findAllByIdConversationIdAndIdMessageIdLessThan(conversationId, before, pageable);
        } else {
            pageable = PageRequest.of(0, 15, Sort.by(Direction.DESC, "id.messageId"));
            messages = messageRepository.findAllByIdConversationId(conversationId, pageable);
        }

        return messages;
    }

}
