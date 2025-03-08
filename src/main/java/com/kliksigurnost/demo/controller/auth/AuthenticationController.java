package com.kliksigurnost.demo.controller.auth;

import com.kliksigurnost.demo.config.JwtService;
import com.kliksigurnost.demo.service.AuthenticationService;
import com.kliksigurnost.demo.service.ConfirmationTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ConfirmationTokenService confirmationTokenService;
    @Value("${frontend.uri}")
    private String frontendUri;

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
                            .error("Registration failed due to an internal error")
                            .build()
            );
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> confirm(@RequestParam("token") String token) {
        try {
            confirmationTokenService.confirmToken(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUri + "/login"))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
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
                            .error("Authentication failed")
                            .build()
            );
        }
    }

    @GetMapping("/authenticate/google")
    public ResponseEntity<String> initiateGoogleAuthentication(HttpServletResponse response) throws IOException {
        log.info("Initiating Google OAuth2 authentication");
        try {
            response.sendRedirect("/oauth2/authorization/google");
            return ResponseEntity.ok("Redirecting to Google for authentication");
        } catch (Exception e) {
            log.error("Google OAuth2 initiation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to initiate Google OAuth2");
        }
    }

    @GetMapping("/authenticationSuccess")
    public ResponseEntity<Void> handleGoogleAuthenticationSuccess(OAuth2AuthenticationToken oAuth2AuthenticationToken, HttpServletResponse response) {
        log.info("Handling Google OAuth2 authentication success");
        try {
            AuthenticationResponse authResponse = authenticationService.authenticateRegisterOAuth2Google(oAuth2AuthenticationToken);

            if (authResponse.getError() == null) {
                // Store tokens in secure HTTP-only cookies
                Cookie accessTokenCookie = new Cookie("access_token", authResponse.getToken());
                accessTokenCookie.setPath("/");
                response.addCookie(accessTokenCookie);

                Cookie refreshTokenCookie = new Cookie("refresh_token", authResponse.getRefreshToken());
                refreshTokenCookie.setPath("/");
                response.addCookie(refreshTokenCookie);

                // Redirect to the frontend without tokens in the URL
                response.sendRedirect(frontendUri + "/oauth-success");
                return ResponseEntity.status(HttpStatus.FOUND).build();
            }

            log.warn("Google OAuth2 authentication failed, redirecting to login");
            response.sendRedirect(frontendUri + "/login");
            return ResponseEntity.status(HttpStatus.FOUND).build();
        } catch (Exception e) {
            log.error("Google OAuth2 success handling error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/authenticationFailure")
    public ResponseEntity<Void> handleGoogleAuthenticationFailure() {
        log.warn("Google OAuth2 authentication failed, redirecting to login");
        try {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUri + "/login"))
                    .build();
        } catch (Exception e) {
            log.error("Google OAuth2 failure handling error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        AuthenticationResponse.builder()
                                .error("Invalid refresh token")
                                .build()
                );
            }

            // Extract email from the refresh token
            String email = jwtService.extractUsername(refreshToken);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        AuthenticationResponse.builder()
                                .error("Invalid refresh token")
                                .build()
                );
            }

            // Load UserDetails from the database
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Validate the refresh token with UserDetails
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        AuthenticationResponse.builder()
                                .error("Invalid refresh token")
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
                            .error("Failed to refresh token")
                            .build()
            );
        }
    }
}