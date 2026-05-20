package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.AdminActivityLog;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.repository.AdminActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AdminActivityLogService {

    private final AdminActivityLogRepository repository;

    public AdminActivityLogService(AdminActivityLogRepository repository) {
        this.repository = repository;
    }

    public void log(User admin, Module module, PermissionAction action, String entityType, Long entityId, String description) {
        log(admin, module, action, entityType, entityId, null, null, description);
    }

    public void log(User admin, Module module, PermissionAction action, String entityType, Long entityId,
                    String oldValue, String newValue, String description) {
        AdminActivityLog entry = new AdminActivityLog();
        if (admin != null) {
            entry.setAdminId(admin.getId());
            entry.setAdminEmail(admin.getEmail());
        }
        entry.setModule(module);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setOldValue(truncate(oldValue, 8000));
        entry.setNewValue(truncate(newValue, 8000));
        entry.setDescription(truncate(description, 1000));

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entry.setIpAddress(extractIp(request));
            entry.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        }
        repository.save(entry);
    }

    public Page<AdminActivityLog> all(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<AdminActivityLog> forAdmin(Long adminId, Pageable pageable) {
        return repository.findByAdminId(adminId, pageable);
    }

    public Page<AdminActivityLog> forEntity(String entityType, Long entityId, Pageable pageable) {
        return repository.findByEntity(entityType, entityId, pageable);
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
