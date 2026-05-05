package com.tour.identity.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;
}