package com.omarahmed42.socialmedia.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.cql.PagingState;
import com.omarahmed42.socialmedia.dto.CursorBasedPagination;
import com.omarahmed42.socialmedia.dto.SortablePaginationInfo;
import com.omarahmed42.socialmedia.enums.MessageStatus;
import com.omarahmed42.socialmedia.enums.SortOrder;
import com.omarahmed42.socialmedia.exception.ConversationNotFoundException;
import com.omarahmed42.socialmedia.exception.ForbiddenConversationAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.UserNotFoundException;
import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.ConversationMember;
import com.omarahmed42.socialmedia.model.Conversation_;
import com.omarahmed42.socialmedia.model.Message;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.projection.ConversationDetailsProjection;
import com.omarahmed42.socialmedia.repository.ConversationMemberRepository;
import com.omarahmed42.socialmedia.repository.ConversationRepository;
import com.omarahmed42.socialmedia.repository.MessageRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.service.BlockingService;
import com.omarahmed42.socialmedia.service.ConversationService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final BlockingService blockingService;

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    @Override
    public Conversation addConversation(ConversationDetailsProjection details, Set<Long> membersIds) {
        SecurityUtils.throwIfNotAuthenticated();
        Long creatorId = SecurityUtils.getAuthenticatedUserId();
        return addConversation(creatorId, details, membersIds);
    }

    @Override
    @Transactional
    public Conversation addConversation(Long creatorId, ConversationDetailsProjection details, Set<Long> membersIds) {
        if (creatorId == null)
            throw new InvalidInputException("Conversation creator cannot be null");

        if (details == null)
            throw new InvalidInputException("Conversation details cannot be null");

        if (details.isGroup() == null)
            throw new InvalidInputException("Conversation type must be specified");

        throwIfNullMembers(membersIds);

        return details.isGroup().booleanValue() ? createGroupConversation(creatorId, details, membersIds)
                : createNormalConversation(creatorId, details, membersIds);
    }

    private void throwIfNullMembers(Set<Long> membersIds) {
        if (membersIds == null)
            throw new InvalidInputException("Conversation members cannot be null or empty");
    }

    private Conversation createGroupConversation(Long creatorId, ConversationDetailsProjection details,
            Set<Long> membersIds) {
        throwIfNullMembers(membersIds);
        membersIds.remove(creatorId);
        if (membersIds.isEmpty())
            throw new InvalidInputException("Conversation members must consist of at least 2 users");

        List<User> members = userRepository.findAllById(membersIds);

        Set<Long> nonExistingMemberIds = extractNonExistingMembers(members, membersIds);
        if (nonExistingMemberIds != null && !nonExistingMemberIds.isEmpty())
            throw new UserNotFoundException("Users with ids " + nonExistingMemberIds.toString() + " not found");

        throwIfCreatorBlocked(creatorId, membersIds);

        String name = StringUtils.isBlank(details.getName()) ? generateGroupName(members) : details.getName();

        Conversation conversation = new Conversation(name, details.isGroup().booleanValue());
        addConversationMembers(conversation, members, creatorId);

        conversation = conversationRepository.save(conversation);
        return conversation;
    }

    private void addConversationMembers(Conversation conversation, List<User> members, Long creatorId) {
        List<User> membersIncludingCreator = new LinkedList<>(members);
        membersIncludingCreator.add(userRepository.getReferenceById(creatorId));

        Collection<ConversationMember> conversationMembers = createConversationMembers(membersIncludingCreator,
                conversation);
        conversation.addConversationMembers(conversationMembers);
    }

    private Collection<ConversationMember> createConversationMembers(List<User> members, Conversation conversation) {
        if (members == null || members.isEmpty())
            return new HashSet<>();

        Set<ConversationMember> conversationMembers = new HashSet<>();
        for (User member : members) {
            ConversationMember conversationMember = new ConversationMember();
            conversationMember.setUser(member);
            conversationMember.setConversation(conversation);
            conversationMembers.add(conversationMember);
        }

        return conversationMembers;
    }

    private Set<Long> extractNonExistingMembers(List<User> members, Set<Long> membersIds) {
        return (members == null || members.isEmpty()) ? membersIds
                : members
                        .stream()
                        .filter(Objects::nonNull)
                        .map(User::getId)
                        .filter(id -> !membersIds.contains(id))
                        .collect(Collectors.toSet());
    }

    private void throwIfCreatorBlocked(Long creatorId, Set<Long> membersIds) {
        if (isCreatorBlocked(creatorId, membersIds))
            throw new ForbiddenConversationAccessException("Some user cannot be added to the conversation");
    }

    private boolean isCreatorBlocked(Long creatorId, Set<Long> membersIds) {
        for (Long memberId : membersIds) {
            if (memberId == null)
                continue;
            if (blockingService.isBlocked(memberId, creatorId))
                return true;
        }
        return false;
    }

    private String generateGroupName(List<User> members) {
        StringBuilder builder = new StringBuilder();

        int i;
        for (i = 0; i < members.size(); i++) {
            if (i == 2)
                break;
            User member = members.get(i);
            builder.append(member.getFirstName());

            if (i + 1 < members.size())
                builder.append(", ");
        }

        if (i < members.size()) {
            builder.append("Others");
        }

        return builder.toString();
    }

    private Conversation createNormalConversation(Long creatorId, ConversationDetailsProjection details,
            Set<Long> membersIds) {
        throwIfNullMembers(membersIds);

        membersIds.remove(creatorId);
        if (membersIds.size() != 1)
            throw new InvalidInputException("Conversation members must consist of exactly 2 users");

        Long memberId = membersIds.iterator().next();
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + memberId + " not found"));

        throwIfCreatorBlocked(creatorId, membersIds);

        Optional<Conversation> personalConversation = conversationRepository.findPersonalConversationBy(creatorId,
                memberId);
        if (personalConversation.isPresent())
            return personalConversation.get();

        String name = null;
        Conversation conversation = new Conversation(name,
                details.isGroup().booleanValue());

        addConversationMembers(conversation, List.of(member), creatorId);
        conversation = conversationRepository.save(conversation);
        return conversation;
    }

    @Override
    public List<Message> getMessagesBy(Conversation conversation, CursorBasedPagination paginationInfo) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        List<ConversationMember> conversationMembers = conversationMemberRepository.findAllByConversation(conversation);

        if (!isMemberOf(authenticatedUserId, conversationMembers))
            throw new ForbiddenConversationAccessException(
                    "Forbidden: cannot access conversation with id " + conversation.getId());

        if (paginationInfo == null)
            paginationInfo = new CursorBasedPagination(15, null);

        if (paginationInfo.getPageSize() == null || paginationInfo.getPageSize() < 1)
            paginationInfo.setPageSize(15);
        else if (paginationInfo.getPageSize() > 30)
            paginationInfo.setPageSize(30);

        PagingState pagingState = paginationInfo.getCursor() == null ? null
                : PagingState.fromString(paginationInfo.getCursor());

        CassandraPageRequest pageRequest = CassandraPageRequest.of(PageRequest.ofSize(paginationInfo.getPageSize()),
                pagingState == null ? null : pagingState.getRawPagingState());

        Slice<Message> sliceOfMessages = messageRepository.findAllByIdConversationId(conversation.getId(),
                pageRequest.withSort(Sort.by(Direction.DESC, "id.messageId")));

        List<Message> messages = sliceOfMessages.getContent();
        if (sliceOfMessages.hasNext()) {
            log.info("New cursor: {}",
                    ((CassandraPageRequest) sliceOfMessages.getPageable()).getPagingState().toString());
        }

        if (messages == null || messages.isEmpty())
            return new ArrayList<>();

        return messages.stream()
                .filter(m -> m.getMessageStatus() != MessageStatus.DRAFT)
                .toList();
    }

    private boolean isMemberOf(Long userId, Collection<ConversationMember> conversationMembers) {
        for (ConversationMember conversationMember : conversationMembers) {
            if (conversationMember.getUser().getId().equals(userId))
                return true;
        }

        return false;
    }

    @Override
    public List<User> getUsersBy(Conversation conversation) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        List<ConversationMember> conversationMembers = conversationMemberRepository.findAllByConversation(conversation);
        if (!isMemberOf(authenticatedUserId, conversationMembers))
            throw new ForbiddenConversationAccessException(
                    "Forbidden: cannot access conversation with id " + conversation.getId());

        List<User> users = new ArrayList<>(conversationMembers.size());
        for (ConversationMember member : conversationMembers) {
            users.add(member.getUser());
        }

        return users;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Conversation> getConversations(SortablePaginationInfo sortablePaginationInfo, Long after) {
        SecurityUtils.throwIfNotAuthenticated();
        if (sortablePaginationInfo == null)
            sortablePaginationInfo = new SortablePaginationInfo(1, 15, SortOrder.DESC);
        else {
            if (sortablePaginationInfo.getPage() == null || sortablePaginationInfo.getPage() < 1)
                sortablePaginationInfo.setPage(1);

            if (sortablePaginationInfo.getPageSize() == null || sortablePaginationInfo.getPageSize() < 1)
                sortablePaginationInfo.setPageSize(15);
            else if (sortablePaginationInfo.getPageSize() > 30)
                sortablePaginationInfo.setPageSize(30);

            if (sortablePaginationInfo.getSort() == null)
                sortablePaginationInfo.setSort(SortOrder.DESC);
        }

        PageRequest page = PageRequest.of(sortablePaginationInfo.getPage() - 1, sortablePaginationInfo.getPageSize(),
                Sort.by(Sort.Direction.fromString(SortOrder.DESC.toString()), Conversation_.ID));

        Page<Conversation> conversations;
        if (after == null)
            conversations = conversationRepository
                    .findAllByConversationMembers_User_id(SecurityUtils.getAuthenticatedUserId(), page);
        else
            conversations = conversationRepository
                    .findAllByConversationMembers_User_idAndIdAfter(SecurityUtils.getAuthenticatedUserId(), after,
                            page);

        return conversations.getContent();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Conversation getConversation(Long conversationId) {
        log.info("Conversation ID: {}", conversationId);
        if (conversationId == null || conversationId <= 0L)
            throw new InvalidInputException("Conversation ID cannot be empty");

        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        List<ConversationMember> conversationMembers = conversationMemberRepository
                .findAllByConversation_id(conversationId);

        if (!isMemberOf(authenticatedUserId, conversationMembers))
            throw new ForbiddenConversationAccessException(
                    "Forbidden: cannot access conversation with id " + conversationId);
        return conversationRepository.findById(conversationId)
                .orElseThrow(ConversationNotFoundException::new);
    }

}
