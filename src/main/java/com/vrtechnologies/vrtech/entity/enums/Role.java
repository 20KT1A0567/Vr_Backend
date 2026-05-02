package com.vrtechnologies.vrtech.entity.enums;

public enum Role {
    USER,
    ADMIN,
    SUPER_ADMIN,
    MANAGER,
    STORE_MANAGER,
    SALES_EXECUTIVE,
    SUPPORT_AGENT,
    INVENTORY_MANAGER,
    CONTENT_MANAGER,
    ACCOUNTANT;

    public boolean isAdminScope() {
        return this != USER;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }
}
