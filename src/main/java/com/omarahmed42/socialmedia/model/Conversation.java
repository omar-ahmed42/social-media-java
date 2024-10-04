package com.omarahmed42.socialmedia.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.omarahmed42.socialmedia.generator.SnowflakeUIDGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true, exclude = "conversationMembers")
@Table
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends Auditable {

    @Id
    @GenericGenerator(name = "snowflake_id_generator", type = SnowflakeUIDGenerator.class)
    @GeneratedValue(generator = "snowflake_id_generator")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", length = 75)
    private String name;

    @Column(name = "is_group")
    private Boolean isGroup;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "conversation")
    @Fetch(FetchMode.SUBSELECT)
    @JsonManagedReference
    private Set<ConversationMember> conversationMembers = new HashSet<>();

    public Conversation(String name, Boolean isGroup) {
        this.name = name;
        this.isGroup = isGroup;
    }

    public void addConversationMember(ConversationMember member) {
        if (member == null)
            return;
        member.setConversation(this);
        conversationMembers.add(member);
    }

    public void addConversationMembers(Collection<ConversationMember> members) {
        if (members == null || members.isEmpty())
            return;

        for (ConversationMember member : members) {
            if (member == null)
                continue;
            member.setConversation(this);
            conversationMembers.add(member);
        }
    }
}
