package com.g42.platform.gms.analytics.repository;

import com.g42.platform.gms.analytics.entity.GoogleOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleOAuthTokenRepository extends JpaRepository<GoogleOAuthToken, Long> {

    Optional<GoogleOAuthToken> findByState(String state);

    Optional<GoogleOAuthToken> findTopByOrderByIdAsc();
}
