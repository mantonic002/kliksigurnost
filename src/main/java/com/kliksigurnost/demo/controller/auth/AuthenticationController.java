package com.kliksigurnost.demo.controller.auth;

import com.kliksigurnost.demo.config.JwtService;
import com.kliksigurnost.demo.exception.InvalidTokenException;
import com.kliksigurnost.demo.model.UserProfile;
import com.kliksigurnost.demo.service.AuthenticationService;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserService userService;

    @Value("${frontend.url}")
    private String frontendUrl;

    private final Environment env;


    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@RequestBody RegisterRequest request) {
        log.info("Registering user with email: {}", request.getEmail());
        try {
            RegisterResponse response = authenticationService.register(request);

            if (response.getError() != null) {
                log.warn("Registration failed for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            log.info("User registered successfully: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    RegisterResponse.builder()
                            .error(env.getProperty("registration-fail.internal"))
                            .build()
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getCurrentUser() {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(
                    new UserProfile(userService.getCurrentUser()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> confirm(@RequestParam("token") String token) {
        try {
            authenticationService.verifyAccount(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/prijava?success=true"))
                    .build();
        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Account verification error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(env.getProperty("verify-fail.internal"));
        }
    }


    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticateUser(@RequestBody AuthenticationRequest request) {
        try {
            log.info("Authenticating user with email: {}", request.getEmail());
            AuthenticationResponse response = authenticationService.authenticate(request);

            if (response.getError() != null) {
                log.warn("Authentication failed for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            log.info("User authenticated successfully: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    AuthenticationResponse.builder()
                            .error(env.getProperty("authentication-fail"))
                            .build()
            );
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        AuthenticationResponse.builder()
                                .error(env.getProperty("invalid-refresh-token"))
                                .build()
                );
            }

            // Extract email from the refresh token
            String email = jwtService.extractUsername(refreshToken);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        AuthenticationResponse.builder()
                                .error(env.getProperty("invalid-refresh-token"))
                                .build()
                );
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Validate the refresh token with UserDetails
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        AuthenticationResponse.builder()
                                .error(env.getProperty("invalid-refresh-token"))
                                .build()
                );
            }

            // Generate new access and refresh tokens
            String newAccessToken = jwtService.generateToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            return ResponseEntity.ok(
                    AuthenticationResponse.builder()
                            .token(newAccessToken)
                            .refreshToken(newRefreshToken)
                            .build()
            );
        } catch (Exception e) {
            log.error("Refresh token error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    AuthenticationResponse.builder()
                            .error(env.getProperty("refresh-token-fail"))
                            .build()
            );
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        try {
            authenticationService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(env.getProperty("forgotten-pw-message"));
        } catch (Exception e) {
            log.error("Forgot password error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(env.getProperty("forgotten-pw-internal"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authenticationService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(env.getProperty("reset-pw-message"));
        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Password reset error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(env.getProperty("reset-pw-internal"));
        }
    }
}