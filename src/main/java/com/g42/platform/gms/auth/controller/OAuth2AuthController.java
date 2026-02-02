package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth/oauth2")
public class OAuth2AuthController {
    @Operation(
            summary = "Login staff with Google OAuth2",
            description = """
            Redirect staff to Google OAuth2 login page.

            Flow:
            1. Frontend redirects user to:
               http://localhost:8080/oauth2/authorization/google
            2. User logs in with Google
            3. Backend validates staff provisioning
            4. Backend returns JWT in AuthResponse

            Note:
            - Staff must be pre-created by admin
            - No self-registration allowed
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Google OAuth2 login success",
                    content = @Content(
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Staff not provisioned or account locked")
    })
    @GetMapping("/google")
    public void googleLoginDoc() {
        // ❗ Swagger-only endpoint
        // ❗ OAuth2 flow is handled by Spring Security
    }
}
