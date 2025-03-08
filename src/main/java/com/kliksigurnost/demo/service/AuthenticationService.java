package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.controller.auth.AuthenticationRequest;
import com.kliksigurnost.demo.controller.auth.AuthenticationResponse;
import com.kliksigurnost.demo.controller.auth.RegisterRequest;
import com.kliksigurnost.demo.controller.auth.RegisterResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public interface AuthenticationService {
    RegisterResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse authenticateRegisterOAuth2Google(OAuth2AuthenticationToken auth2AuthenticationToken);
    }
