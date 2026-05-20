package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirebaseLoginRequest {
    @NotBlank
    private String firebaseIdToken;
}
