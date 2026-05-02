package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.entity.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admin_roles")
public class AdminRole extends BaseEntity {

    @Id
    @Column(name = "role_key", nullable = false, length = 64)
    private String roleKey;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_role", nullable = false, length = 32)
    private Role baseRole;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "protected_role", nullable = false)
    private boolean protectedRole = false;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole = false;
}
