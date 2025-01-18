package com.kliksigurnost.demo.controller.auth;

import com.kliksigurnost.demo.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final AuthenticationService service;
    @Value("${frontend.uri}")
    private String frontendUri;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        var resp = service.register(request);
        if (resp.getError() != null) {
            return ResponseEntity.status(401).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        var resp = service.authenticate(request);
        if (resp.getError() != null) {
            return ResponseEntity.status(401).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/authenticate/google")
    public ResponseEntity<String> authenticateGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
        return ResponseEntity.ok("Redirecting to google");


    }

    @GetMapping("/authenticationSuccess")
    public ResponseEntity<AuthenticationResponse> handleGoogleSuccess(OAuth2AuthenticationToken oAuth2AuthenticationToken) throws IOException {
        var resp = service.authenticateRegisterOAuth2Google(oAuth2AuthenticationToken);

        if (resp.getError() == null) {
            // Ensure the redirect is executed properly, passing the token as a query parameter
            String redirectUrl = frontendUri + "/home?token=" + resp.getToken();
            return ResponseEntity.status(302)  // 302 for temporary redirect
                    .location(URI.create(redirectUrl))
                    .build();
        }

        // If there's an error, redirect to the login page
        return ResponseEntity.status(302)
                .location(URI.create(frontendUri + "/login"))
                .build();
    }


}
