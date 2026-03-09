package com.g42.platform.gms.notification.infrastructure;

import org.springframework.stereotype.Service;

@Service
public class ZaloOAuthService {
    public String buildAuthorizeUrl() {

        String verifier = PKCE.generateVerifier();
        String challenge = PKCE.generateChallenge(verifier);

        // TODO: lưu verifier (redis/db)

        return "https://oauth.zaloapp.com/v4/oa/permission"
                + "?app_id=YOUR_APP_ID"
                + "&redirect_uri=YOUR_CALLBACK_URL"
                + "&code_challenge=" + challenge
                + "&code_challenge_method=S256";
    }
}
