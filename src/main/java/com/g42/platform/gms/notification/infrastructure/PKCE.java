package com.g42.platform.gms.notification.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PKCE {

    private PKCE() {}

    public static String generateVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public static String generateChallenge(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(
                    verifier.getBytes(StandardCharsets.US_ASCII)
            );
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate PKCE challenge", e);
        }
    }
}