package com.omarahmed42.socialmedia.service.impl;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.dto.projection.UserPersonalInfoDto;
import com.omarahmed42.socialmedia.dto.projection.UserPublicInfoDto;
import com.omarahmed42.socialmedia.exception.ForbiddenException;
import com.omarahmed42.socialmedia.exception.UserNotFoundException;
import com.omarahmed42.socialmedia.mapper.UserMapper;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.model.graph.UserNode;
import com.omarahmed42.socialmedia.repository.RoleRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.repository.graph.UserNodeRepository;
import com.omarahmed42.socialmedia.service.UserService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserNodeRepository userNodeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public User addUser(String firstName, String lastName, String email, String password, LocalDate dateOfBirth,
            boolean enabled, boolean active, Set<String> rolesNames) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
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
        if (!authenticatedUserId.equals(userId)) throw new ForbiddenException("Cannot access private information for user with id " + userId);
        return userMapper.toEntity(userRepository.findUserPersonalInfoById(userId)
                                .orElseThrow(() -> new UserNotFoundException("User not found with id  " + userId)));
    }

    @Override
    @Transactional(readOnly = true) 
    public User getUserPublicInfo(Long userId){
        throwIfBlankUserId(userId);
        SecurityUtils.throwIfNotAuthenticated();
        return userMapper.toEntity(userRepository.findUserPublicInfoById(userId)
                                .orElseThrow(() -> new UserNotFoundException("User not found with id " + userId)));
    }

    private void throwIfBlankUserId(Long userId) {
        if (userId == null)
            throw new IllegalArgumentException("User id cannot be empty");
    }
}
