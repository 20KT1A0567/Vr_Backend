package com.vrtechnologies.vrtech.dto.response;

public class LoginOutcome {

    private final AuthResponse authResponse;
    private final TwoFactorChallengeResponse twoFactorChallenge;

    private LoginOutcome(AuthResponse authResponse, TwoFactorChallengeResponse twoFactorChallenge) {
        this.authResponse = authResponse;
        this.twoFactorChallenge = twoFactorChallenge;
    }

    public static LoginOutcome authenticated(AuthResponse response) {
        return new LoginOutcome(response, null);
    }

    public static LoginOutcome twoFactor(TwoFactorChallengeResponse challenge) {
        return new LoginOutcome(null, challenge);
    }

    public boolean isTwoFactorRequired() {
        return twoFactorChallenge != null;
    }

    public AuthResponse getAuthResponse() {
        return authResponse;
    }

    public TwoFactorChallengeResponse getTwoFactorChallenge() {
        return twoFactorChallenge;
    }
}
