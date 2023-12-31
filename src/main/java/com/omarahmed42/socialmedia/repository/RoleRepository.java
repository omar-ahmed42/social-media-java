package com.omarahmed42.socialmedia.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Set<Role> findAllByNameIn(Iterable<String> rolesNames);

}
