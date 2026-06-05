package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.TrustedDevice;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.repository.TrustedDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class TrustedDeviceService {

    private final TrustedDeviceRepository trustedDeviceRepository;

    public TrustedDeviceService(TrustedDeviceRepository trustedDeviceRepository) {
        this.trustedDeviceRepository = trustedDeviceRepository;
    }

    @Transactional
    public String createTrustedDevice(User user, String ipAddress, String userAgent) {
        String rawToken = generateSecureToken();
        TrustedDevice device = new TrustedDevice();
        device.setUser(user);
        device.setDeviceHash(rawToken);
        device.setIpAddress(ipAddress);
        device.setUserAgent(userAgent);
        device.setExpiresAt(LocalDateTime.now().plusDays(30));
        trustedDeviceRepository.save(device);
        return rawToken;
    }

    public boolean isDeviceTrusted(User user, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Optional<TrustedDevice> opt = trustedDeviceRepository.findByDeviceHash(token);
        if (opt.isEmpty()) {
            return false;
        }
        TrustedDevice device = opt.get();
        if (!device.getUser().getId().equals(user.getId())) {
            return false;
        }
        if (device.getExpiresAt().isBefore(LocalDateTime.now())) {
            trustedDeviceRepository.delete(device);
            return false;
        }
        return true;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
