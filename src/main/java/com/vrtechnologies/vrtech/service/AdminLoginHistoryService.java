package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.AdminLoginHistory;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.repository.AdminLoginHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AdminLoginHistoryService {

    private final AdminLoginHistoryRepository repository;

    public AdminLoginHistoryService(AdminLoginHistoryRepository repository) {
        this.repository = repository;
    }

    public void recordSuccess(User admin, Long sessionId) {
        AdminLoginHistory entry = baseEntry(admin, sessionId);
        entry.setStatus("SUCCESS");
        repository.save(entry);
    }

    public void recordFailure(String email, String reason) {
        AdminLoginHistory entry = new AdminLoginHistory();
        entry.setAdminEmail(email);
        entry.setLoginAt(LocalDateTime.now());
        entry.setStatus("FAILURE");
        entry.setFailureReason(truncate(reason, 255));
        applyRequestMeta(entry);
        repository.save(entry);
    }

    public Page<AdminLoginHistory> all(Pageable pageable) {
        return repository.findAllByOrderByLoginAtDesc(pageable);
    }

    public Page<AdminLoginHistory> forAdmin(Long adminId, Pageable pageable) {
        return repository.findByAdminId(adminId, pageable);
    }

    public void recordLogout(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        AdminLoginHistory entry = repository.findFirstBySessionIdOrderByLoginAtDesc(sessionId);
        if (entry == null || entry.getLogoutAt() != null) {
            return;
        }
        entry.setLogoutAt(LocalDateTime.now());
        repository.save(entry);
    }

    private AdminLoginHistory baseEntry(User admin, Long sessionId) {
        AdminLoginHistory entry = new AdminLoginHistory();
        if (admin != null) {
            entry.setAdminId(admin.getId());
            entry.setAdminEmail(admin.getEmail());
        }
        entry.setSessionId(sessionId);
        entry.setLoginAt(LocalDateTime.now());
        applyRequestMeta(entry);
        return entry;
    }

    private void applyRequestMeta(AdminLoginHistory entry) {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String forwarded = request.getHeader("X-Forwarded-For");
            entry.setIpAddress(forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr());
            entry.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        }
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
