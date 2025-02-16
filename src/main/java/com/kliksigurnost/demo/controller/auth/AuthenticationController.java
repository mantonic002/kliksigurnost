package com.kliksigurnost.demo.controller.auth;

import com.kliksigurnost.demo.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    @Value("${frontend.uri}")
    private String frontendUri;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> registerUser(@RequestBody RegisterRequest request) {
        log.info("Registering user with email: {}", request.getEmail());
        AuthenticationResponse response = authenticationService.register(request);

        if (response.getError() != null) {
            log.warn("Registration failed for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        log.info("User registered successfully: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticateUser(@RequestBody AuthenticationRequest request) {
        log.info("Authenticating user with email: {}", request.getEmail());
        AuthenticationResponse response = authenticationService.authenticate(request);

        if (response.getError() != null) {
            log.warn("Authentication failed for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        log.info("User authenticated successfully: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/authenticate/google")
    public ResponseEntity<String> initiateGoogleAuthentication(HttpServletResponse response) throws IOException {
        log.info("Initiating Google OAuth2 authentication");
        response.sendRedirect("/oauth2/authorization/google");
        return ResponseEntity.ok("Redirecting to Google for authentication");
    }

    @GetMapping("/authenticationSuccess")
    public ResponseEntity<Void> handleGoogleAuthenticationSuccess(OAuth2AuthenticationToken oAuth2AuthenticationToken) {
        log.info("Handling Google OAuth2 authentication success");
        AuthenticationResponse response = authenticationService.authenticateRegisterOAuth2Google(oAuth2AuthenticationToken);

        if (response.getError() == null) {
            String redirectUrl = frontendUri + "/home?token=" + response.getToken();
            log.info("Redirecting to frontend: {}", redirectUrl);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        }

        log.warn("Google OAuth2 authentication failed, redirecting to login");
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUri + "/login"))
                .build();
    }

    @GetMapping("/authenticationFailure")
    public ResponseEntity<Void> handleGoogleAuthenticationFailure() {
        log.warn("Google OAuth2 authentication failed, redirecting to login");
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUri + "/login"))
                .build();
    }
}