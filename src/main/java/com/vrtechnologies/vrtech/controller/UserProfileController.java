package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.UserAddressRequest;
import com.vrtechnologies.vrtech.dto.request.UserProfileRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.UserAddressResponse;
import com.vrtechnologies.vrtech.dto.response.UserProfileResponse;
import com.vrtechnologies.vrtech.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/api/users/profile")
    public ApiResponse<UserProfileResponse> profile() {
        return ApiResponse.ok("Profile fetched", userProfileService.getProfile());
    }

    @PutMapping("/api/users/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        return ApiResponse.ok("Profile updated", userProfileService.updateProfile(request));
    }

    @PostMapping("/api/users/addresses")
    public ApiResponse<UserAddressResponse> createAddress(@Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.ok("Address saved", userProfileService.saveAddress(request, null));
    }

    @PutMapping("/api/users/addresses/{id}")
    public ApiResponse<UserAddressResponse> updateAddress(@PathVariable Long id, @Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.ok("Address saved", userProfileService.saveAddress(request, id));
    }

    @DeleteMapping("/api/users/addresses/{id}")
    public ApiResponse<Object> deleteAddress(@PathVariable Long id) {
        userProfileService.deleteAddress(id);
        return ApiResponse.ok("Address deleted", null);
    }
}
