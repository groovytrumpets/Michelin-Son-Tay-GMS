package com.g42.platform.gms.notification.infrastructure;

import com.g42.platform.gms.notification.api.dto.TokenRes;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import com.g42.platform.gms.notification.infrastructure.repository.ZaloTokenRepo;
import com.nimbusds.oauth2.sdk.TokenResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
@AllArgsConstructor
@Service
public class ZaloOAuthService {
    private ZaloTokenRepo zaloTokenRepo;
    public String buildAuthorizeUrl() {

        String verifier = PKCE.generateVerifier();
        String challenge = PKCE.generateChallenge(verifier);
        System.out.println("Verifier: "+verifier);
        System.out.println("Challenge: "+challenge);

        // TODO: lưu verifier (redis/db)
        ZaloToken zaloToken = new ZaloToken();
        zaloToken.setAccessToken(verifier);
        zaloToken.setState("verified");
        zaloTokenRepo.save(zaloToken);
        return "https://oauth.zaloapp.com/v4/oa/permission"
                + "?app_id=1162973287649757116"
                + "&redirect_uri=https://michelinsontay.vn/zalo/callback"
                + "&code_challenge=" + challenge
                + "&code_challenge_method=S256";
    }

    public TokenRes getAccessToken(String code, String codeVerifier) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("app_id", "1162973287649757116");
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("code_verifier", codeVerifier);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<?> request = new HttpEntity<>(body, headers);

        ResponseEntity<TokenRes> response =
                restTemplate.postForEntity(
                        "https://oauth.zaloapp.com/v4/access_token",
                        request,
                        TokenRes.class
                );

        return response.getBody();
    }
}
