package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.AdminRole;
import com.vrtechnologies.vrtech.entity.AdminPermission;
import com.vrtechnologies.vrtech.entity.RolePermission;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.repository.AdminRoleRepository;
import com.vrtechnologies.vrtech.repository.AdminPermissionRepository;
import com.vrtechnologies.vrtech.repository.AdminStoreAccessRepository;
import com.vrtechnologies.vrtech.repository.RolePermissionRepository;
import com.vrtechnologies.vrtech.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final RolePermissionMatrix matrix;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final AdminStoreAccessRepository adminStoreAccessRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;

    public PermissionService(
            RolePermissionMatrix matrix,
            AdminRoleRepository adminRoleRepository,
            AdminPermissionRepository adminPermissionRepository,
            AdminStoreAccessRepository adminStoreAccessRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRepository userRepository
    ) {
        this.matrix = matrix;
        this.adminRoleRepository = adminRoleRepository;
        this.adminPermissionRepository = adminPermissionRepository;
        this.adminStoreAccessRepository = adminStoreAccessRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
    }

    public boolean hasPermission(User admin, Module module, PermissionAction action) {
        if (admin == null) {
            return false;
        }
        if (admin.getRole() == Role.SUPER_ADMIN) {
            return true;
        }
        if (module == Module.ADMINS) {
            return false;
        }

        Boolean adminOverride = readAdminOverride(admin.getId(), module, action);
        if (adminOverride != null) {
            return adminOverride;
        }
        Boolean roleOverride = readRoleOverride(resolveRoleKey(admin), module, action);
        if (roleOverride != null) {
            return roleOverride;
        }
        return matrix.hasPermission(admin.getRole(), module, action);
    }

    public boolean hasPermission(Long adminId, Module module, PermissionAction action) {
        return userRepository.findById(adminId)
                .map(user -> hasPermission(user, module, action))
                .orElse(false);
    }

    public void requirePermission(User admin, Module module, PermissionAction action) {
        if (!hasPermission(admin, module, action)) {
            throw new AccessDeniedException("Missing permission: " + module + ":" + action);
        }
    }

    public Set<Module> visibleModules(User admin) {
        if (admin == null) {
            return EnumSet.noneOf(Module.class);
        }
        if (admin.getRole() == Role.SUPER_ADMIN) {
            return EnumSet.allOf(Module.class);
        }

        Map<Module, EnumSet<PermissionAction>> effective = effectivePermissions(admin);
        EnumSet<Module> visible = EnumSet.noneOf(Module.class);
        effective.forEach((module, actions) -> {
            if (actions.contains(PermissionAction.VIEW)) {
                visible.add(module);
            }
        });
        return visible;
    }

    public Map<Module, EnumSet<PermissionAction>> effectivePermissions(User admin) {
        if (admin == null) {
            return new LinkedHashMap<>();
        }
        if (admin.getRole() == Role.SUPER_ADMIN) {
            Map<Module, EnumSet<PermissionAction>> all = new LinkedHashMap<>();
            for (Module module : Module.values()) {
                all.put(module, EnumSet.allOf(PermissionAction.class));
            }
            return all;
        }
        Map<Module, EnumSet<PermissionAction>> base = matrix.rolePermissions(admin.getRole()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> EnumSet.copyOf(entry.getValue()),
                        (a, b) -> a, LinkedHashMap::new));

        rolePermissionRepository.findByRole(resolveRoleKey(admin)).forEach(permission -> applyOverride(base, permission.getModule(), permission.getAction(), permission.isGranted()));
        adminPermissionRepository.findByAdminId(admin.getId()).forEach(permission -> applyOverride(base, permission.getModule(), permission.getAction(), permission.isGranted()));
        base.remove(Module.ADMINS);
        return base;
    }

    public Map<Module, EnumSet<PermissionAction>> effectiveRolePermissions(String roleKey) {
        AdminRole role = requireRole(roleKey);
        if (role.getBaseRole() == Role.SUPER_ADMIN) {
            Map<Module, EnumSet<PermissionAction>> all = new LinkedHashMap<>();
            for (Module module : Module.values()) {
                all.put(module, EnumSet.allOf(PermissionAction.class));
            }
            return all;
        }
        Map<Module, EnumSet<PermissionAction>> base = matrix.rolePermissions(role.getBaseRole()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> EnumSet.copyOf(entry.getValue()),
                        (a, b) -> a, LinkedHashMap::new));

        rolePermissionRepository.findByRole(role.getRoleKey()).forEach(permission -> applyOverride(base, permission.getModule(), permission.getAction(), permission.isGranted()));
        base.remove(Module.ADMINS);
        return base;
    }

    public Map<Module, EnumSet<PermissionAction>> effectiveRolePermissions(Role role) {
        return effectiveRolePermissions(role.name());
    }

    @Transactional(readOnly = true)
    public List<Long> accessibleStoreIds(User admin) {
        if (admin == null) {
            return List.of();
        }
        if (admin.getRole() == Role.SUPER_ADMIN || admin.getRole() == Role.ADMIN || admin.getRole() == Role.MANAGER) {
            return List.of();
        }
        return adminStoreAccessRepository.findByAdminId(admin.getId()).stream()
                .map(access -> access.getStore().getId())
                .toList();
    }

    public boolean canAccessStore(User admin, Long storeId) {
        if (admin == null || storeId == null) {
            return false;
        }
        if (admin.getRole() == Role.SUPER_ADMIN || admin.getRole() == Role.ADMIN || admin.getRole() == Role.MANAGER) {
            return true;
        }
        return adminStoreAccessRepository.existsByAdminIdAndStoreId(admin.getId(), storeId);
    }

    public void requireStoreAccess(User admin, Long storeId) {
        if (!canAccessStore(admin, storeId)) {
            throw new AccessDeniedException("You do not have access to this store");
        }
    }

    private Boolean readAdminOverride(Long adminId, Module module, PermissionAction action) {
        for (AdminPermission permission : adminPermissionRepository.findByAdminId(adminId)) {
            if (permission.getModule() == module && permission.getAction() == action) {
                return permission.isGranted();
            }
        }
        return null;
    }

    public String resolveRoleKey(User admin) {
        if (admin == null) {
            return null;
        }
        if (admin.getAdminRoleKey() != null && !admin.getAdminRoleKey().isBlank()) {
            return admin.getAdminRoleKey().trim().toUpperCase();
        }
        return admin.getRole() != null ? admin.getRole().name() : null;
    }

    public AdminRole requireRole(String roleKey) {
        if (roleKey == null || roleKey.isBlank()) {
            throw new AccessDeniedException("Role key is required");
        }
        return adminRoleRepository.findById(roleKey.trim().toUpperCase())
                .orElseThrow(() -> new AccessDeniedException("Role not found: " + roleKey));
    }

    private Boolean readRoleOverride(String roleKey, Module module, PermissionAction action) {
        if (roleKey == null || roleKey.isBlank()) {
            return null;
        }
        for (RolePermission permission : rolePermissionRepository.findByRole(roleKey)) {
            if (permission.getModule() == module && permission.getAction() == action) {
                return permission.isGranted();
            }
        }
        return null;
    }

    private void applyOverride(Map<Module, EnumSet<PermissionAction>> base, Module module, PermissionAction action, boolean granted) {
        EnumSet<PermissionAction> actions = base.computeIfAbsent(module, key -> EnumSet.noneOf(PermissionAction.class));
        if (granted) {
            actions.add(action);
        } else {
            actions.remove(action);
        }
    }
}
