package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    List<WebAuthnCredential> findByUserId(Long userId);
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);
    void deleteByIdAndUserId(Long id, Long userId);
    boolean existsByUserId(Long userId);
}
