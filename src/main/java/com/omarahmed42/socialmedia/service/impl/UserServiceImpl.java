package com.omarahmed42.socialmedia.service.impl;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.Uuid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.omarahmed42.socialmedia.enums.AttachmentStatus;
import com.omarahmed42.socialmedia.enums.AttachmentType;
import com.omarahmed42.socialmedia.exception.AttachmentNotFoundException;
import com.omarahmed42.socialmedia.exception.ForbiddenException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.UnsupportedMediaExtensionException;
import com.omarahmed42.socialmedia.exception.UserNotFoundException;
import com.omarahmed42.socialmedia.mapper.UserMapper;
import com.omarahmed42.socialmedia.model.Attachment;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.model.graph.UserNode;
import com.omarahmed42.socialmedia.projection.UserUpdateInputProjection;
import com.omarahmed42.socialmedia.repository.AttachmentRepository;
import com.omarahmed42.socialmedia.repository.RoleRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.repository.graph.UserNodeRepository;
import com.omarahmed42.socialmedia.service.FileService;
import com.omarahmed42.socialmedia.service.UserService;
import com.omarahmed42.socialmedia.util.AttachmentUtils;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserNodeRepository userNodeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    private final FileService fileService;
    private final AttachmentRepository attachmentRepository;

    private final TransactionTemplate transactionTemplate;

    @Value("${storage.users.path}")
    private String userStoragePath;

    @Value("${storage.users.public.path}")
    private String userPublicStoragePath;

    @Override
    @Transactional
    public User addUser(String firstName, String lastName, String username, String email, String password,
            LocalDate dateOfBirth,
            boolean enabled, boolean active, Set<String> rolesNames) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setDateOfBirth(dateOfBirth);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(enabled);
        user.setActive(active);

        Set<Role> roles = roleRepository.findAllByNameIn(rolesNames);
        user.addRoles(roles);

        user = userRepository.save(user);
        kafkaTemplate.send("graph-user-creation", user.getId());

        return user;
    }

    @Override
    @Transactional
    public User updateUser(UserUpdateInputProjection userInput) {
        log.info("Updating user");
        SecurityUtils.throwIfNotAuthenticated();

        Long authUserId = SecurityUtils.getAuthenticatedUserId();

        if (userInput == null)
            throw new InvalidInputException("User fields cannot be empty");

        User user = userRepository.findById(authUserId).orElseThrow(UserNotFoundException::new);
        if (StringUtils.length(userInput.getFirstName()) < 1 || StringUtils.length(userInput.getFirstName()) > 50)
            throw new InvalidInputException("First name length must be between 1 and 50 characters.");
        if (StringUtils.length(userInput.getLastName()) < 1 || StringUtils.length(userInput.getLastName()) > 50)
            throw new InvalidInputException("Last name length must be between 1 and 50 characters.");
        if (StringUtils.length(userInput.getBio()) > 160)
            throw new InvalidInputException("Bio length must be between 0 and 160 characters.");

        user.setFirstName(userInput.getFirstName());
        user.setLastName(userInput.getLastName());
        user.setBio(userInput.getBio());

        user = userRepository.save(user);
        return user;
    }

    @KafkaListener(topics = "graph-user-creation")
    public void addUserToGraph(ConsumerRecord<String, Long> consumerRecord) {
        Long userId = consumerRecord.value();
        UserNode userNode = new UserNode(userId);
        userNodeRepository.save(userNode);
    }

    @Override
    @Transactional
    public Integer deleteUser(Long userId) {
        throwIfBlankUserId(userId);
        Integer rowsAffected = 0;
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            userRepository.delete(user.get());
            kafkaTemplate.send("graph-user-deletion", userId);
            rowsAffected++;
        }

        return rowsAffected;
    }

    @KafkaListener(topics = "graph-user-deletion")
    public void removeUserFromGraph(ConsumerRecord<String, Long> consumerRecord) {
        Long userId = consumerRecord.value();
        userNodeRepository.deleteById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        throwIfBlankUserId(userId);
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserPersonalInfo(Long userId) {
        throwIfBlankUserId(userId);
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        if (!authenticatedUserId.equals(userId))
            throw new ForbiddenException("Cannot access private information for user with id " + userId);
        return userMapper.toEntity(userRepository.findUserPersonalInfoById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id  " + userId)));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserPublicInfo(Long userId) {
        throwIfBlankUserId(userId);
        SecurityUtils.throwIfNotAuthenticated();
        return userMapper.toEntity(userRepository.findUserPublicInfoById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + userId)));
    }

    private void throwIfBlankUserId(Long userId) {
        if (userId == null)
            throw new IllegalArgumentException("User id cannot be empty");
    }

    @Override
    public void updateAvatar(MultipartFile avatarMultipartFile) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

        String fileExtension = FilenameUtils.getExtension(avatarMultipartFile.getOriginalFilename());

        if (!isValidImageExtension(fileExtension))
            throw new UnsupportedMediaExtensionException(fileExtension + " extension is not supported");

        AttachmentType attachmentType = AttachmentType.IMAGE;

        String filename = Uuid.randomUuid().toString() + AttachmentUtils.EXTENSION_SEPARATOR
                + fileExtension;

        String fileUrl = userPublicStoragePath + File.separator + authenticatedUserId.toString() + File.separator
                + filename;

        Attachment attachment = new Attachment();
        attachment.setExtension(fileExtension);
        attachment.setName(filename);
        attachment.setSize(avatarMultipartFile.getSize());
        attachment.setStatus(AttachmentStatus.UPLOADING);
        attachment.setUrl(fileUrl);
        attachment.setAttachmentType(attachmentType);
        attachment = attachmentRepository.save(attachment);

        storeFile(avatarMultipartFile, attachment, authenticatedUserId, "user-avatar");
    }

    private boolean isValidImageExtension(String fileExtension) {
        for (String extension : AttachmentUtils.IMAGE_EXTENSIONS) {
            if (fileExtension.equalsIgnoreCase(extension))
                return true;
        }

        return false;
    }

    public void storeFile(MultipartFile multipartFile, Attachment attachment, Long userId, String topicName) {
        String fileUrl = attachment.getUrl();
        String requestId = UUID.randomUUID().toString();
        try {
            log.info("MY_PATH: " + Path.of(fileUrl).toString());
            fileService.copy(multipartFile.getInputStream(), Path.of(fileUrl));
            Map<String, Object> successMessage = buildMessage(userId, attachment.getId(), AttachmentStatus.COMPLETED);
            kafkaTemplate.send(topicName, requestId, successMessage);
        } catch (Exception e) {
            log.error("Error while saving attachment: {}", e);
            Map<String, Object> failedMessage = buildMessage(userId, attachment.getId(), AttachmentStatus.FAILED);
            kafkaTemplate.send(topicName, requestId, failedMessage);
        }
    }

    private Map<String, Object> buildMessage(Long userId, Long attachmentId, AttachmentStatus status) {
        return Map.of("userId", userId, "attachmentId", attachmentId,
                "status",
                status.toString());
    }

    @KafkaListener(topics = "user-avatar")
    public void consumeAvatar(ConsumerRecord<String, Map<String, Object>> consumerRecord) {
        Map<String, Object> value = consumerRecord.value();
        Long userId = (Long) value.get("userId");
        Long attachmentId = (Long) value.get("attachmentId");
        AttachmentStatus status = AttachmentStatus.valueOf((String) value.get("status"));

        transactionTemplate.execute(transactionStatus -> {
            Attachment newAvatar = attachmentRepository.findById(attachmentId)
                    .orElseThrow(AttachmentNotFoundException::new);
            newAvatar.setStatus(status);
            newAvatar = attachmentRepository.save(newAvatar);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));

            Attachment oldAvatar = user.getAvatar();
            user.setAvatar(newAvatar);

            if (oldAvatar != null)
                attachmentRepository.delete(oldAvatar);
            return userRepository.save(user);
        });
    }

    @Override
    public void updateCover(MultipartFile coverFile) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

        String fileExtension = FilenameUtils.getExtension(coverFile.getOriginalFilename());

        if (!isValidImageExtension(fileExtension))
            throw new UnsupportedMediaExtensionException(fileExtension + " extension is not supported");

        AttachmentType attachmentType = AttachmentType.IMAGE;

        String filename = Uuid.randomUuid().toString() + AttachmentUtils.EXTENSION_SEPARATOR
                + fileExtension;

        String fileUrl = userPublicStoragePath + File.separator + authenticatedUserId.toString() + File.separator
                + filename;

        Attachment attachment = new Attachment();
        attachment.setExtension(fileExtension);
        attachment.setName(filename);
        attachment.setSize(coverFile.getSize());
        attachment.setStatus(AttachmentStatus.UPLOADING);
        attachment.setUrl(fileUrl);
        attachment.setAttachmentType(attachmentType);
        attachment = attachmentRepository.save(attachment);

        storeFile(coverFile, attachment, authenticatedUserId, "user-cover-picture");
    }

    @KafkaListener(topics = "user-cover-picture")
    public void consumeCoverPicture(ConsumerRecord<String, Map<String, Object>> consumerRecord) {
        Map<String, Object> value = consumerRecord.value();
        Long userId = (Long) value.get("userId");
        Long attachmentId = (Long) value.get("attachmentId");
        log.info("Status: {}", (String) value.get("status"));
        AttachmentStatus attachmentStatus = AttachmentStatus.valueOf((String) value.get("status"));

        transactionTemplate.execute(transactionStatus -> {
            Attachment newCoverPicture = attachmentRepository.findById(attachmentId)
                    .orElseThrow(AttachmentNotFoundException::new);
            newCoverPicture.setStatus(attachmentStatus);
            newCoverPicture = attachmentRepository.save(newCoverPicture);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));

            Attachment oldCoverPicture = user.getCoverPicture();
            user.setCoverPicture(newCoverPicture);

            if (oldCoverPicture != null)
                attachmentRepository.delete(oldCoverPicture);
            return userRepository.save(user);
        });

    }

}
