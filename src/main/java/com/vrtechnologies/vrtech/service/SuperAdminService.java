package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.AdminCreateRequest;
import com.vrtechnologies.vrtech.dto.request.AdminPasswordResetRequest;
import com.vrtechnologies.vrtech.dto.request.AdminPermissionsRequest;
import com.vrtechnologies.vrtech.dto.request.AdminRoleCreateRequest;
import com.vrtechnologies.vrtech.dto.request.AdminRoleUpdateRequest;
import com.vrtechnologies.vrtech.dto.request.AdminStoresRequest;
import com.vrtechnologies.vrtech.dto.request.AdminUpdateRequest;
import com.vrtechnologies.vrtech.dto.request.RolePermissionsRequest;
import com.vrtechnologies.vrtech.dto.response.AdminPermissionResponse;
import com.vrtechnologies.vrtech.dto.response.AdminLoginHistoryResponse;
import com.vrtechnologies.vrtech.dto.response.AdminRoleResponse;
import com.vrtechnologies.vrtech.dto.response.AdminUserResponse;
import com.vrtechnologies.vrtech.dto.response.PermissionCatalogResponse;
import com.vrtechnologies.vrtech.dto.response.RolePermissionsResponse;
import com.vrtechnologies.vrtech.entity.AdminActivityLog;
import com.vrtechnologies.vrtech.entity.AdminRole;
import com.vrtechnologies.vrtech.entity.AdminPermission;
import com.vrtechnologies.vrtech.entity.AdminStoreAccess;
import com.vrtechnologies.vrtech.entity.RolePermission;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.AdminStatus;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.AdminRoleRepository;
import com.vrtechnologies.vrtech.repository.AdminPermissionRepository;
import com.vrtechnologies.vrtech.repository.AdminStoreAccessRepository;
import com.vrtechnologies.vrtech.repository.RolePermissionRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import com.vrtechnologies.vrtech.repository.UserRepository;
import com.vrtechnologies.vrtech.repository.UserPasswordHistoryRepository;
import com.vrtechnologies.vrtech.entity.UserPasswordHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final AdminStoreAccessRepository adminStoreAccessRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserContextService userContextService;
    private final AdminActivityLogService activityLogService;
    private final AdminLoginHistoryService loginHistoryService;
    private final PermissionService permissionService;
    private final UserPasswordHistoryRepository passwordHistoryRepository;

    public SuperAdminService(
            UserRepository userRepository,
            StoreRepository storeRepository,
            AdminRoleRepository adminRoleRepository,
            AdminPermissionRepository adminPermissionRepository,
            AdminStoreAccessRepository adminStoreAccessRepository,
            RolePermissionRepository rolePermissionRepository,
            PasswordEncoder passwordEncoder,
            UserContextService userContextService,
            AdminActivityLogService activityLogService,
            AdminLoginHistoryService loginHistoryService,
            PermissionService permissionService,
            UserPasswordHistoryRepository passwordHistoryRepository
    ) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.adminPermissionRepository = adminPermissionRepository;
        this.adminStoreAccessRepository = adminStoreAccessRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.userContextService = userContextService;
        this.activityLogService = activityLogService;
        this.loginHistoryService = loginHistoryService;
        this.permissionService = permissionService;
        this.passwordHistoryRepository = passwordHistoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listAdmins(Role role, String search, Pageable pageable) {
        return userRepository.findAdmins(role, search, pageable).map(this::toResponse);
    }

    @Transactional
    public AdminUserResponse createAdmin(AdminCreateRequest request) {
        AdminRole assignedRole = resolveRequestedRole(request.getRoleKey(), request.getRole(), true);
        Role role = assignedRole.getBaseRole();
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }
        validateAccessWindow(request.getAccessStartDate(), request.getAccessEndDate());
        validateLoginWindow(request.getAllowedLoginStartTime(), request.getAllowedLoginEndTime());
        validatePasswordStrength(request.getPassword());

        User actor = userContextService.getCurrentUser();

        User user = new User();
        user.setName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setPhone(normalizePhone(request.getPhone()));
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setAdminRoleKey(assignedRole.getRoleKey());
        user.setActive(true);
        user.setAdminStatus(AdminStatus.ACTIVE);
        user.setProfileImageUrl(request.getProfileImageUrl());
        user.setAccessStartDate(request.getAccessStartDate());
        user.setAccessEndDate(request.getAccessEndDate());
        user.setAllowedLoginStartTime(request.getAllowedLoginStartTime());
        user.setAllowedLoginEndTime(request.getAllowedLoginEndTime());
        user.setAllowedLoginDaysFromSet(parseDaysOfWeek(request.getAllowedLoginDays()));
        if (request.getTwoFactorEnabled() != null) {
            user.setTwoFactorEnabled(request.getTwoFactorEnabled());
        } else {
            user.setTwoFactorEnabled(role == Role.SUPER_ADMIN);
        }
        user.setCreatedBy(actor.getId());

        user = userRepository.save(user);
        recordPasswordHistory(user.getId(), encodedPassword);

        if (request.getStoreIds() != null && !request.getStoreIds().isEmpty()) {
            assignStores(user, request.getStoreIds());
        }

        activityLogService.log(actor, Module.ADMINS, PermissionAction.CREATE, "User", user.getId(),
                null, summary(user), "Created admin " + user.getEmail());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getAdmin(Long id) {
        return toResponse(mustGet(id));
    }

    @Transactional
    public AdminUserResponse updateAdmin(Long id, AdminUpdateRequest request) {
        User user = mustGet(id);
        User actor = userContextService.getCurrentUser();
        String before = summary(user);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setName(request.getFullName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(newEmail);
        }
        if (request.getPhone() != null) {
            user.setPhone(normalizePhone(request.getPhone()));
        }
        if (request.getRole() != null) {
            request.setRoleKey(request.getRoleKey() == null ? request.getRole().name() : request.getRoleKey());
        }
        if (request.getRoleKey() != null) {
            AdminRole assignedRole = resolveRequestedRole(request.getRoleKey(), request.getRole(), false);
            if (Objects.equals(user.getId(), actor.getId()) && !assignedRole.getRoleKey().equalsIgnoreCase(permissionService.resolveRoleKey(actor))) {
                throw new BadRequestException("You cannot change your own role");
            }
            user.setRole(assignedRole.getBaseRole());
            user.setAdminRoleKey(assignedRole.getRoleKey());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        validateAccessWindow(request.getAccessStartDate(), request.getAccessEndDate());
        user.setAccessStartDate(request.getAccessStartDate());
        user.setAccessEndDate(request.getAccessEndDate());
        validateLoginWindow(request.getAllowedLoginStartTime(), request.getAllowedLoginEndTime());
        user.setAllowedLoginStartTime(request.getAllowedLoginStartTime());
        user.setAllowedLoginEndTime(request.getAllowedLoginEndTime());
        if (request.getAllowedLoginDays() != null) {
            user.setAllowedLoginDaysFromSet(parseDaysOfWeek(request.getAllowedLoginDays()));
        }
        if (request.getTwoFactorEnabled() != null) {
            if (user.getRole() == Role.SUPER_ADMIN && !request.getTwoFactorEnabled()) {
                throw new BadRequestException("Two-factor authentication cannot be disabled for SUPER_ADMIN accounts");
            }
            user.setTwoFactorEnabled(request.getTwoFactorEnabled());
        }

        user = userRepository.save(user);

        if (request.getStoreIds() != null) {
            assignStores(user, request.getStoreIds());
        }

        activityLogService.log(actor, Module.ADMINS, PermissionAction.UPDATE, "User", user.getId(),
                before, summary(user), "Updated admin " + user.getEmail());
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse setStatus(Long id, AdminStatus status) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        User user = mustGet(id);
        User actor = userContextService.getCurrentUser();
        if (Objects.equals(user.getId(), actor.getId())) {
            throw new BadRequestException("You cannot change your own status");
        }
        AdminStatus before = user.effectiveAdminStatus();
        user.setAdminStatus(status);
        user.setActive(status == AdminStatus.ACTIVE);
        user = userRepository.save(user);
        activityLogService.log(actor, Module.ADMINS, PermissionAction.UPDATE, "User", user.getId(),
                before.name(), status.name(), "Status changed for " + user.getEmail() + " to " + status);
        return toResponse(user);
    }

    @Transactional
    public void resetPassword(Long id, AdminPasswordResetRequest request) {
        User user = mustGet(id);
        User actor = userContextService.getCurrentUser();
        validatePasswordStrength(request.getPassword());
        validatePasswordHistory(user.getId(), request.getPassword());
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);
        recordPasswordHistory(user.getId(), encodedPassword);
        activityLogService.log(actor, Module.ADMINS, PermissionAction.UPDATE, "User", user.getId(),
                null, null, "Password reset for " + user.getEmail());
    }

    @Transactional
    public void deleteAdmin(Long id) {
        User user = mustGet(id);
        User actor = userContextService.getCurrentUser();
        if (Objects.equals(user.getId(), actor.getId())) {
            throw new BadRequestException("You cannot delete your own account");
        }
        user.setAdminStatus(AdminStatus.DISABLED);
        user.setActive(false);
        userRepository.save(user);
        adminPermissionRepository.deleteByAdminId(id);
        adminStoreAccessRepository.deleteByAdminId(id);
        activityLogService.log(actor, Module.ADMINS, PermissionAction.DELETE, "User", id, null, null,
                "Disabled admin " + user.getEmail());
    }

    @Transactional
    public AdminPermissionResponse setPermissions(Long id, AdminPermissionsRequest request) {
        User user = mustGet(id);
        User actor = userContextService.getCurrentUser();
        adminPermissionRepository.deleteByAdminId(id);
        if (request.getPermissions() != null) {
            for (AdminPermissionsRequest.Entry entry : request.getPermissions()) {
                if (entry.getModule() == null || entry.getAction() == null) {
                    continue;
                }
                AdminPermission permission = new AdminPermission();
                permission.setAdminId(id);
                permission.setModule(entry.getModule());
                permission.setAction(entry.getAction());
                permission.setGranted(entry.isGranted());
                adminPermissionRepository.save(permission);
            }
        }
        activityLogService.log(actor, Module.ADMINS, PermissionAction.ASSIGN, "User", id, null, null,
                "Updated permissions for " + user.getEmail());
        return buildPermissionResponse(user);
    }

    @Transactional
    public AdminUserResponse setStores(Long id, AdminStoresRequest request) {
        User user = mustGet(id);
        User actor = userContextService.getCurrentUser();
        assignStores(user, request.getStoreIds() != null ? request.getStoreIds() : List.of());
        activityLogService.log(actor, Module.ADMINS, PermissionAction.ASSIGN, "User", id, null, null,
                "Updated store access for " + user.getEmail());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AdminPermissionResponse getPermissions(Long id) {
        return buildPermissionResponse(mustGet(id));
    }

    @Transactional(readOnly = true)
    public Page<AdminActivityLog> getAdminActivity(Long id, Pageable pageable) {
        mustGet(id);
        return activityLogService.forAdmin(id, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminLoginHistoryResponse> getLoginHistory(Pageable pageable) {
        return loginHistoryService.all(pageable).map(this::toLoginHistoryResponse);
    }

    @Transactional
    public RolePermissionsResponse createRole(AdminRoleCreateRequest request) {
        String roleKey = normalizeRoleKey(request.getRoleKey());
        if (adminRoleRepository.existsById(roleKey)) {
            throw new BadRequestException("Role key already exists");
        }
        validateCustomBaseRole(request.getBaseRole());

        AdminRole role = new AdminRole();
        role.setRoleKey(roleKey);
        role.setDisplayName(request.getDisplayName().trim());
        role.setDescription(normalizeText(request.getDescription()));
        role.setBaseRole(request.getBaseRole());
        role.setActive(true);
        role.setProtectedRole(false);
        role.setSystemRole(false);
        adminRoleRepository.save(role);

        User actor = userContextService.getCurrentUser();
        activityLogService.log(actor, Module.ADMINS, PermissionAction.CREATE, "Role", null, null, roleKey,
                "Created role " + roleKey);
        return getRolePermissions(roleKey);
    }

    @Transactional(readOnly = true)
    public RolePermissionsResponse getRolePermissions(String roleKey) {
        AdminRole role = mustGetRole(roleKey);
        Map<Module, EnumSet<PermissionAction>> effective = permissionService.effectiveRolePermissions(role.getRoleKey());
        List<RolePermissionsResponse.Entry> entries = new ArrayList<>();
        for (Module module : Module.values()) {
            EnumSet<PermissionAction> actions = effective.getOrDefault(module, EnumSet.noneOf(PermissionAction.class));
            for (PermissionAction action : PermissionAction.values()) {
                entries.add(RolePermissionsResponse.Entry.builder()
                        .module(module)
                        .action(action)
                        .granted(actions.contains(action))
                        .build());
            }
        }
        return RolePermissionsResponse.builder()
                .role(role.getBaseRole())
                .role(role.getBaseRole())
                .roleKey(role.getRoleKey())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .active(role.isActive())
                .protectedRole(isCoreSuperAdminRole(role))
                .systemRole(isCoreSuperAdminRole(role))
                .adminCount(countAdminsForRole(role))
                .entries(entries)
                .build();
    }

    @Transactional
    public RolePermissionsResponse updateRole(String roleKey, AdminRoleUpdateRequest request) {
        AdminRole role = mustGetRole(roleKey);
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            role.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getDescription() != null) {
            role.setDescription(normalizeText(request.getDescription()));
        }
        if (request.getActive() != null) {
            if (isCoreSuperAdminRole(role) && !request.getActive()) {
                throw new BadRequestException("Protected roles cannot be disabled");
            }
            if (!request.getActive() && countAdminsForRole(role) > 0) {
                throw new BadRequestException("Role cannot be disabled while assigned to active admins");
            }
            role.setActive(request.getActive());
        }
        adminRoleRepository.save(role);

        User actor = userContextService.getCurrentUser();
        activityLogService.log(actor, Module.ADMINS, PermissionAction.UPDATE, "Role", null, null, role.getRoleKey(),
                "Updated role " + role.getRoleKey());
        return getRolePermissions(role.getRoleKey());
    }

    @Transactional
    public void deleteRole(String roleKey) {
        AdminRole role = mustGetRole(roleKey);
        if (isCoreSuperAdminRole(role)) {
            throw new BadRequestException("Protected roles cannot be deleted");
        }
        User actor = userContextService.getCurrentUser();
        userRepository.findAll().stream()
                .filter(User::isAdmin)
                .filter(user -> roleMatches(user, role))
                .forEach(user -> {
                    if (Objects.equals(user.getId(), actor.getId())) {
                        throw new BadRequestException("You cannot delete your own assigned role");
                    }
                    user.setRole(Role.USER);
                    user.setAdminRoleKey(null);
                    user.setAdminStatus(AdminStatus.DISABLED);
                    user.setActive(false);
                    userRepository.save(user);
                });
        rolePermissionRepository.deleteByRole(role.getRoleKey());
        adminRoleRepository.delete(role);

        activityLogService.log(actor, Module.ADMINS, PermissionAction.DELETE, "Role", null, role.getRoleKey(), null,
                "Deleted role " + role.getRoleKey());
    }

    @Transactional
    public RolePermissionsResponse setRolePermissions(String roleKey, RolePermissionsRequest request) {
        AdminRole role = mustGetRole(roleKey);
        if (role.getBaseRole() == Role.SUPER_ADMIN) {
            throw new BadRequestException("SUPER_ADMIN permissions cannot be modified");
        }
        if (role.getBaseRole() == Role.USER) {
            throw new BadRequestException("USER role is not an admin role");
        }
        rolePermissionRepository.deleteByRole(role.getRoleKey());
        if (request.getPermissions() != null) {
            for (RolePermissionsRequest.Entry entry : request.getPermissions()) {
                if (entry.getModule() == null || entry.getAction() == null) {
                    continue;
                }
                RolePermission permission = new RolePermission();
                permission.setRole(role.getRoleKey());
                permission.setModule(entry.getModule());
                permission.setAction(entry.getAction());
                permission.setGranted(entry.isGranted());
                rolePermissionRepository.save(permission);
            }
        }
        User actor = userContextService.getCurrentUser();
        activityLogService.log(actor, Module.ADMINS, PermissionAction.ASSIGN, "Role", null, null, role.getRoleKey(),
                "Updated role permissions for " + role.getRoleKey());
        return getRolePermissions(role.getRoleKey());
    }

    @Transactional(readOnly = true)
    public List<RolePermissionsResponse> allRolePermissions() {
        return adminRoleRepository.findAllByOrderBySystemRoleDescDisplayNameAsc().stream()
                .map(role -> getRolePermissions(role.getRoleKey()))
                .toList();
    }

    public PermissionCatalogResponse permissionCatalog() {
        return PermissionCatalogResponse.builder()
                .modules(Arrays.asList(Module.values()))
                .actions(Arrays.asList(PermissionAction.values()))
                .roles(Arrays.stream(Role.values()).filter(r -> r != Role.USER).toList())
                .managedRoles(adminRoleRepository.findAllByOrderBySystemRoleDescDisplayNameAsc().stream()
                        .map(this::toRoleSummary)
                        .toList())
                .build();
    }

    private AdminPermissionResponse buildPermissionResponse(User user) {
        Map<Module, EnumSet<PermissionAction>> effective = permissionService.effectivePermissions(user);
        Set<String> overrides = adminPermissionRepository.findByAdminId(user.getId()).stream()
                .map(perm -> perm.getModule().name() + ":" + perm.getAction().name())
                .collect(Collectors.toSet());

        List<AdminPermissionResponse.Entry> entries = new ArrayList<>();
        for (Module module : Module.values()) {
            EnumSet<PermissionAction> actions = effective.getOrDefault(module, EnumSet.noneOf(PermissionAction.class));
            for (PermissionAction action : PermissionAction.values()) {
                entries.add(AdminPermissionResponse.Entry.builder()
                        .module(module)
                        .action(action)
                        .granted(actions.contains(action))
                        .fromOverride(overrides.contains(module.name() + ":" + action.name()))
                        .build());
            }
        }
        return AdminPermissionResponse.builder()
                .adminId(user.getId())
                .entries(entries)
                .visibleModules(new ArrayList<>(permissionService.visibleModules(user)))
                .build();
    }

    private void assignStores(User admin, List<Long> storeIds) {
        adminStoreAccessRepository.deleteByAdminId(admin.getId());
        for (Long storeId : storeIds) {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new BadRequestException("Store not found: " + storeId));
            AdminStoreAccess access = new AdminStoreAccess();
            access.setAdmin(admin);
            access.setStore(store);
            adminStoreAccessRepository.save(access);
        }
    }

    private void validateAccessWindow(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("Access end date must be on or after start date");
        }
    }

    private void validateLoginWindow(LocalTime start, LocalTime end) {
        if ((start == null) != (end == null)) {
            throw new BadRequestException("Both login start and end times must be provided");
        }
    }

    private Set<DayOfWeek> parseDaysOfWeek(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new TreeSet<>();
        }
        Set<DayOfWeek> result = new TreeSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            try {
                result.add(DayOfWeek.valueOf(normalized));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid day of week: " + value);
            }
        }
        return result;
    }

    private void validateAssignableRole(Role role) {
        if (role == null) {
            throw new BadRequestException("Role is required");
        }
        if (role == Role.USER) {
            throw new BadRequestException("Cannot assign USER role to an admin");
        }
    }

    private void validateCustomBaseRole(Role role) {
        validateAssignableRole(role);
    }

    private User mustGet(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (user.getRole() == Role.USER) {
            throw new ResourceNotFoundException("Admin not found");
        }
        return user;
    }

    private AdminRole mustGetRole(String roleKey) {
        return adminRoleRepository.findById(normalizeRoleKey(roleKey))
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    private AdminUserResponse toResponse(User user) {
        List<AdminStoreAccess> accessRows = adminStoreAccessRepository.findByAdminId(user.getId());
        AdminRole assignedRole = resolveAssignedRole(user);
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .roleKey(assignedRole != null ? assignedRole.getRoleKey() : permissionService.resolveRoleKey(user))
                .roleName(assignedRole != null ? assignedRole.getDisplayName() : formatRoleName(user.getRole()))
                .status(user.effectiveAdminStatus())
                .active(user.isActive())
                .profileImageUrl(user.getProfileImageUrl())
                .accessStartDate(user.getAccessStartDate())
                .accessEndDate(user.getAccessEndDate())
                .allowedLoginStartTime(user.getAllowedLoginStartTime())
                .allowedLoginEndTime(user.getAllowedLoginEndTime())
                .allowedLoginDays(user.allowedLoginDaysSet().stream().map(Enum::name).toList())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .createdBy(user.getCreatedBy())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .stores(accessRows.stream().map(access -> AdminUserResponse.StoreSummary.builder()
                        .id(access.getStore().getId())
                        .name(access.getStore().getName())
                        .city(access.getStore().getCity())
                        .build()).toList())
                .build();
    }

    private String summary(User user) {
        return user.getRole() + "|" + user.effectiveAdminStatus() + "|" + user.getEmail();
    }

    private AdminRole resolveRequestedRole(String roleKey, Role fallbackRole, boolean requireRole) {
        if (roleKey != null && !roleKey.isBlank()) {
            AdminRole role = mustGetRole(roleKey);
            if (!role.isActive()) {
                throw new BadRequestException("Selected role is inactive");
            }
            validateAssignableRole(role.getBaseRole());
            return role;
        }
        if (fallbackRole == null) {
            if (requireRole) {
                throw new BadRequestException("Role is required");
            }
            return null;
        }
        validateAssignableRole(fallbackRole);
        return mustGetRole(fallbackRole.name());
    }

    private AdminRole resolveAssignedRole(User user) {
        String roleKey = permissionService.resolveRoleKey(user);
        if (roleKey == null || roleKey.isBlank()) {
            return null;
        }
        return adminRoleRepository.findById(roleKey).orElse(null);
    }

    private long countAdminsForRole(AdminRole role) {
        return userRepository.findAll().stream()
                .filter(User::isAdmin)
                .filter(user -> roleMatches(user, role))
                .count();
    }

    private boolean roleMatches(User user, AdminRole role) {
        String roleKey = permissionService.resolveRoleKey(user);
        if (roleKey == null || roleKey.isBlank()) {
            return false;
        }
        return role.getRoleKey().equalsIgnoreCase(roleKey);
    }

    private boolean isCoreSuperAdminRole(AdminRole role) {
        return role != null && role.getBaseRole() == Role.SUPER_ADMIN && Role.SUPER_ADMIN.name().equals(role.getRoleKey());
    }

    private AdminRoleResponse toRoleSummary(AdminRole role) {
        return AdminRoleResponse.builder()
                .roleKey(role.getRoleKey())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .baseRole(role.getBaseRole())
                .active(role.isActive())
                .protectedRole(isCoreSuperAdminRole(role))
                .systemRole(isCoreSuperAdminRole(role))
                .adminCount(countAdminsForRole(role))
                .build();
    }

    private String formatRoleName(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "Super Admin";
            case STORE_MANAGER -> "Store Manager";
            case SALES_EXECUTIVE -> "Sales Executive";
            case SUPPORT_AGENT -> "Support Agent";
            case INVENTORY_MANAGER -> "Inventory Manager";
            case CONTENT_MANAGER -> "Content Manager";
            default -> {
                String normalized = role.name().replace('_', ' ').toLowerCase(Locale.ROOT);
                yield Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
            }
        };
    }

    private String normalizeRoleKey(String value) {
        if (value == null) {
            throw new BadRequestException("Role key is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (!normalized.matches("[A-Z0-9_]{3,64}")) {
            throw new BadRequestException("Role key must use letters, numbers, or underscores");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 12) {
            throw new BadRequestException("Password must be at least 12 characters long.");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        String specialChars = "!@#$%^&*()-_=+[]{}|;:',.<>?/~`\"";

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (specialChars.indexOf(c) >= 0) hasSpecial = true;
        }

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new BadRequestException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
        }
    }

    private void validatePasswordHistory(Long userId, String newPassword) {
        List<UserPasswordHistory> history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (UserPasswordHistory past : history) {
            if (passwordEncoder.matches(newPassword, past.getPasswordHash())) {
                throw new BadRequestException("You cannot reuse any of your last 3 passwords.");
            }
        }
    }

    private void recordPasswordHistory(Long userId, String newPasswordHash) {
        List<UserPasswordHistory> history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (history.size() >= 3) {
            for (int i = 2; i < history.size(); i++) {
                passwordHistoryRepository.delete(history.get(i));
            }
        }
        UserPasswordHistory entry = new UserPasswordHistory();
        entry.setUserId(userId);
        entry.setPasswordHash(newPasswordHash);
        passwordHistoryRepository.save(entry);
    }

    private AdminLoginHistoryResponse toLoginHistoryResponse(com.vrtechnologies.vrtech.entity.AdminLoginHistory entry) {
        return AdminLoginHistoryResponse.builder()
                .id(entry.getId())
                .adminId(entry.getAdminId())
                .adminEmail(entry.getAdminEmail())
                .sessionId(entry.getSessionId())
                .loginAt(entry.getLoginAt())
                .logoutAt(entry.getLogoutAt())
                .ipAddress(entry.getIpAddress())
                .userAgent(entry.getUserAgent())
                .status(entry.getStatus())
                .failureReason(entry.getFailureReason())
                .build();
    }
}
