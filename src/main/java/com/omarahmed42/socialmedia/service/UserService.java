package com.omarahmed42.socialmedia.service;

import java.time.LocalDate;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.omarahmed42.socialmedia.model.User;

public interface UserService {
    User addUser(String firstName, String lastName, String email, String password, LocalDate dateOfBirth,
            boolean enabled, boolean active, Set<String> rolesNames);

    void addUserToGraph(ConsumerRecord<String, Long> consumerRecord);

    Integer deleteUser(Long userId);

    User getUser(Long userId);
}
