package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.AuthType;
import com.g42.platform.gms.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Integer> {

    Optional<OtpVerification>
    findTopByAuthTypeAndAuthIdOrderByCreatedAtDesc(
            AuthType authType,
            Integer authId
    );
}
