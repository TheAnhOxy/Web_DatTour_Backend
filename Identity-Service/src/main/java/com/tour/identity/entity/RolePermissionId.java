package com.tour.identity.entity;


import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RolePermissionId implements Serializable {
    private Long  roleId;
    private Long  permissionId;
}