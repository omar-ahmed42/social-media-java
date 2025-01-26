package com.omarahmed42.socialmedia.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.GenericGenerator;

import com.omarahmed42.socialmedia.generator.SnowflakeUIDGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@EqualsAndHashCode(callSuper = false)
@Table
@Setter
@Getter
public class User extends Auditable {
    @Id
    @GenericGenerator(name = "snowflake_id_generator", type = SnowflakeUIDGenerator.class)
    @GeneratedValue(generator = "snowflake_id_generator")
    private Long id;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "email", length = 254, nullable = false, unique = true)
    private String email;

    @Column(name = "password", length = 128, nullable = false)
    private String password;

    @Column(name = "bio", length = 160)
    private String bio;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "active")
    private boolean active;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "avatar_id", referencedColumnName = "id")
    private Attachment avatar;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cover_pic_id", referencedColumnName = "id")
    private Attachment coverPicture;

    @ManyToMany
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) {
        if (role == null || roles.contains(role))
            return;

        roles.add(role);
    }

    public void addRoles(Iterable<Role> roles) {
        for (Role role : roles) {
            addRole(role);
        }
    }
}
