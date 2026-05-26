package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.UserAddressRequest;
import com.vrtechnologies.vrtech.dto.request.UserProfileRequest;
import com.vrtechnologies.vrtech.dto.response.UserAddressResponse;
import com.vrtechnologies.vrtech.dto.response.UserProfileResponse;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.UserAddress;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.UserAddressRepository;
import com.vrtechnologies.vrtech.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserProfileService {

    private final UserContextService userContextService;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;

    public UserProfileService(UserContextService userContextService, UserRepository userRepository, UserAddressRepository userAddressRepository) {
        this.userContextService = userContextService;
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
    }

    public UserProfileResponse getProfile() {
        return toProfile(userContextService.getCurrentUser());
    }

    @Transactional
    public UserProfileResponse updateProfile(UserProfileRequest request) {
        User user = userContextService.getCurrentUser();
        user.setName(request.getName().trim());
        user.setPreferredContactName(normalize(request.getPreferredContactName()));
        user.setPreferredContactPhone(normalize(request.getPreferredContactPhone()));
        user.setPreferredContactEmail(normalize(request.getPreferredContactEmail()));
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        return toProfile(userRepository.save(user));
    }

    @Transactional
    public UserAddressResponse saveAddress(UserAddressRequest request, Long id) {
        User user = userContextService.getCurrentUser();
        UserAddress address = id == null ? new UserAddress() : userAddressRepository.findById(id)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        address.setUser(user);
        address.setLabel(request.getLabel().trim());
        address.setContactName(request.getContactName().trim());
        address.setContactPhone(request.getContactPhone().trim());
        address.setContactEmail(normalize(request.getContactEmail()));
        address.setAddress(request.getAddress().trim());
        address.setCity(normalize(request.getCity()));
        address.setState(normalize(request.getState()));
        address.setPostalCode(normalize(request.getPostalCode()));
        address.setDefaultAddress(request.isDefaultAddress());
        if (address.isDefaultAddress()) {
            userAddressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDescIdDesc(user.getId())
                    .forEach(existing -> {
                        existing.setDefaultAddress(false);
                        userAddressRepository.save(existing);
                    });
            user.setPreferredDeliveryAddress(address.getAddress());
            userRepository.save(user);
        }
        return toAddress(userAddressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(Long id) {
        User user = userContextService.getCurrentUser();
        userAddressRepository.deleteByUserIdAndId(user.getId(), id);
    }

    private UserProfileResponse toProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(publicEmail(user))
                .phone(user.getPhone())
                .preferredContactName(user.getPreferredContactName())
                .preferredContactPhone(user.getPreferredContactPhone())
                .preferredContactEmail(user.getPreferredContactEmail())
                .addresses(userAddressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDescIdDesc(user.getId()).stream().map(this::toAddress).toList())
                .build();
    }

    private UserAddressResponse toAddress(UserAddress address) {
        return UserAddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .contactName(address.getContactName())
                .contactPhone(address.getContactPhone())
                .contactEmail(address.getContactEmail())
                .address(address.getAddress())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .defaultAddress(address.isDefaultAddress())
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String publicEmail(User user) {
        if (user == null || user.getEmail() == null) {
            return null;
        }
        if (isInternalPhoneLoginEmail(user)) {
            return null;
        }
        return user.getEmail();
    }

    private boolean isInternalPhoneLoginEmail(User user) {
        String email = user.getEmail().toLowerCase(Locale.ROOT);
        String phone = user.getPhone() == null ? "" : user.getPhone().toLowerCase(Locale.ROOT);
        if (!phone.isBlank() && email.equals(phone)) {
            return true;
        }
        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.endsWith(".local");
    }
}
