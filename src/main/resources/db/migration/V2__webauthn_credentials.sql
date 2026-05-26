-- WebAuthn / Passkey / Fingerprint credentials per user
CREATE TABLE IF NOT EXISTS webauthn_credentials (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    credential_id   TEXT            NOT NULL,
    public_key_cose TEXT            NOT NULL,
    sign_count      BIGINT          NOT NULL DEFAULT 0,
    nickname        VARCHAR(100),
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at    DATETIME,
    CONSTRAINT fk_webauthn_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_webauthn_user ON webauthn_credentials(user_id);
