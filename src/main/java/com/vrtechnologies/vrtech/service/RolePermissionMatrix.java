package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.Role;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class RolePermissionMatrix {

    private static final EnumSet<PermissionAction> ALL_ACTIONS = EnumSet.allOf(PermissionAction.class);
    private static final EnumSet<PermissionAction> READ_ONLY = EnumSet.of(PermissionAction.VIEW);
    private static final EnumSet<PermissionAction> READ_WRITE = EnumSet.of(
            PermissionAction.VIEW, PermissionAction.CREATE, PermissionAction.UPDATE
    );
    private static final EnumSet<PermissionAction> READ_WRITE_DELETE = EnumSet.of(
            PermissionAction.VIEW, PermissionAction.CREATE, PermissionAction.UPDATE, PermissionAction.DELETE
    );
    private static final EnumSet<PermissionAction> READ_EXPORT = EnumSet.of(
            PermissionAction.VIEW, PermissionAction.EXPORT
    );

    private final Map<Role, Map<Module, EnumSet<PermissionAction>>> matrix = new EnumMap<>(Role.class);

    public RolePermissionMatrix() {
        // SUPER_ADMIN: every module, every action
        Map<Module, EnumSet<PermissionAction>> superAdmin = new EnumMap<>(Module.class);
        for (Module module : Module.values()) {
            superAdmin.put(module, EnumSet.copyOf(ALL_ACTIONS));
        }
        matrix.put(Role.SUPER_ADMIN, superAdmin);

        // ADMIN (legacy): everything except admin user management
        Map<Module, EnumSet<PermissionAction>> admin = new EnumMap<>(Module.class);
        for (Module module : Module.values()) {
            if (module == Module.ADMINS) {
                admin.put(module, EnumSet.of(PermissionAction.VIEW));
            } else {
                admin.put(module, EnumSet.copyOf(ALL_ACTIONS));
            }
        }
        matrix.put(Role.ADMIN, admin);

        // MANAGER: full operational, no admin user creation
        Map<Module, EnumSet<PermissionAction>> manager = new EnumMap<>(Module.class);
        manager.put(Module.DASHBOARD, EnumSet.copyOf(READ_ONLY));
        manager.put(Module.PRODUCTS, EnumSet.copyOf(ALL_ACTIONS));
        manager.put(Module.CATEGORIES, EnumSet.copyOf(READ_WRITE_DELETE));
        manager.put(Module.BRANDS, EnumSet.copyOf(READ_WRITE_DELETE));
        manager.put(Module.STORES, EnumSet.copyOf(READ_WRITE));
        manager.put(Module.BANNERS, EnumSet.copyOf(READ_WRITE_DELETE));
        manager.put(Module.COUPONS, EnumSet.copyOf(READ_WRITE_DELETE));
        manager.put(Module.ORDERS, EnumSet.copyOf(ALL_ACTIONS));
        manager.put(Module.CUSTOMERS, EnumSet.copyOf(READ_WRITE));
        manager.put(Module.INVENTORY, EnumSet.copyOf(READ_WRITE));
        manager.put(Module.ENQUIRIES, EnumSet.copyOf(ALL_ACTIONS));
        manager.put(Module.SERVICES, EnumSet.copyOf(ALL_ACTIONS));
        manager.put(Module.REPORTS, EnumSet.copyOf(READ_EXPORT));
        manager.put(Module.WEBSITE_CONTENT, EnumSet.copyOf(READ_WRITE));
        manager.put(Module.SETTINGS, EnumSet.copyOf(READ_ONLY));
        matrix.put(Role.MANAGER, manager);

        // STORE_MANAGER: scoped to stores assigned, full ops on store-related modules
        Map<Module, EnumSet<PermissionAction>> storeManager = new EnumMap<>(Module.class);
        storeManager.put(Module.DASHBOARD, EnumSet.copyOf(READ_ONLY));
        storeManager.put(Module.PRODUCTS, EnumSet.copyOf(READ_WRITE));
        storeManager.put(Module.INVENTORY, EnumSet.copyOf(READ_WRITE));
        storeManager.put(Module.ORDERS, EnumSet.copyOf(READ_WRITE));
        storeManager.put(Module.CUSTOMERS, EnumSet.copyOf(READ_ONLY));
        storeManager.put(Module.ENQUIRIES, EnumSet.copyOf(READ_WRITE));
        storeManager.put(Module.COUPONS, EnumSet.copyOf(READ_ONLY));
        storeManager.put(Module.SERVICES, EnumSet.copyOf(READ_WRITE));
        storeManager.put(Module.STORES, EnumSet.copyOf(READ_ONLY));
        storeManager.put(Module.REPORTS, EnumSet.copyOf(READ_ONLY));
        matrix.put(Role.STORE_MANAGER, storeManager);

        // SALES_EXECUTIVE: orders, customers, enquiries
        Map<Module, EnumSet<PermissionAction>> salesExec = new EnumMap<>(Module.class);
        salesExec.put(Module.DASHBOARD, EnumSet.copyOf(READ_ONLY));
        salesExec.put(Module.PRODUCTS, EnumSet.copyOf(READ_ONLY));
        salesExec.put(Module.ORDERS, EnumSet.copyOf(READ_WRITE));
        salesExec.put(Module.CUSTOMERS, EnumSet.copyOf(READ_WRITE));
        salesExec.put(Module.COUPONS, EnumSet.copyOf(READ_ONLY));
        salesExec.put(Module.ENQUIRIES, EnumSet.copyOf(READ_WRITE));
        matrix.put(Role.SALES_EXECUTIVE, salesExec);

        // SUPPORT_AGENT: enquiries + services
        Map<Module, EnumSet<PermissionAction>> support = new EnumMap<>(Module.class);
        support.put(Module.DASHBOARD, EnumSet.copyOf(READ_ONLY));
        support.put(Module.ENQUIRIES, EnumSet.copyOf(READ_WRITE));
        support.put(Module.SERVICES, EnumSet.copyOf(READ_WRITE));
        support.put(Module.CUSTOMERS, EnumSet.copyOf(READ_ONLY));
        matrix.put(Role.SUPPORT_AGENT, support);

        // INVENTORY_MANAGER: products + inventory + categories + brands
        Map<Module, EnumSet<PermissionAction>> inventoryManager = new EnumMap<>(Module.class);
        inventoryManager.put(Module.DASHBOARD, EnumSet.copyOf(READ_ONLY));
        inventoryManager.put(Module.PRODUCTS, EnumSet.copyOf(ALL_ACTIONS));
        inventoryManager.put(Module.INVENTORY, EnumSet.copyOf(ALL_ACTIONS));
        inventoryManager.put(Module.CATEGORIES, EnumSet.copyOf(READ_WRITE_DELETE));
        inventoryManager.put(Module.BRANDS, EnumSet.copyOf(READ_WRITE_DELETE));
        inventoryManager.put(Module.COUPONS, EnumSet.copyOf(READ_ONLY));
        inventoryManager.put(Module.REPORTS, EnumSet.copyOf(READ_EXPORT));
        matrix.put(Role.INVENTORY_MANAGER, inventoryManager);

        // CONTENT_MANAGER: banners + website content
        Map<Module, EnumSet<PermissionAction>> contentManager = new EnumMap<>(Module.class);
        contentManager.put(Module.DASHBOARD, EnumSet.copyOf(READ_ONLY));
        contentManager.put(Module.BANNERS, EnumSet.copyOf(ALL_ACTIONS));
        contentManager.put(Module.WEBSITE_CONTENT, EnumSet.copyOf(ALL_ACTIONS));
        contentManager.put(Module.CATEGORIES, EnumSet.copyOf(READ_ONLY));
        contentManager.put(Module.PRODUCTS, EnumSet.copyOf(READ_ONLY));
        matrix.put(Role.CONTENT_MANAGER, contentManager);

        // ACCOUNTANT: reports, orders read+export
        Map<Module, EnumSet<PermissionAction>> accountant = new EnumMap<>(Module.class);
        accountant.put(Module.DASHBOARD, EnumSet.copyOf(READ_ONLY));
        accountant.put(Module.COUPONS, EnumSet.copyOf(READ_ONLY));
        accountant.put(Module.ORDERS, EnumSet.copyOf(READ_EXPORT));
        accountant.put(Module.REPORTS, EnumSet.copyOf(READ_EXPORT));
        accountant.put(Module.CUSTOMERS, EnumSet.copyOf(READ_ONLY));
        matrix.put(Role.ACCOUNTANT, accountant);

        // USER (customer-facing) sees nothing in admin
        matrix.put(Role.USER, new EnumMap<>(Module.class));
    }

    public boolean hasPermission(Role role, Module module, PermissionAction action) {
        if (role == null || module == null || action == null) {
            return false;
        }
        Map<Module, EnumSet<PermissionAction>> moduleMap = matrix.get(role);
        if (moduleMap == null) {
            return false;
        }
        EnumSet<PermissionAction> actions = moduleMap.get(module);
        return actions != null && actions.contains(action);
    }

    public Set<Module> visibleModules(Role role) {
        Map<Module, EnumSet<PermissionAction>> moduleMap = matrix.get(role);
        if (moduleMap == null) {
            return EnumSet.noneOf(Module.class);
        }
        EnumSet<Module> modules = EnumSet.noneOf(Module.class);
        moduleMap.forEach((module, actions) -> {
            if (actions.contains(PermissionAction.VIEW)) {
                modules.add(module);
            }
        });
        return modules;
    }

    public Map<Module, EnumSet<PermissionAction>> rolePermissions(Role role) {
        return matrix.getOrDefault(role, new EnumMap<>(Module.class));
    }
}
