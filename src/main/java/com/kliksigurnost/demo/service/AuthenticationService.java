package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.controller.auth.AuthenticationRequest;
import com.kliksigurnost.demo.controller.auth.AuthenticationResponse;
import com.kliksigurnost.demo.controller.auth.RegisterRequest;
import com.kliksigurnost.demo.controller.auth.RegisterResponse;
import com.kliksigurnost.demo.exception.InvalidTokenException;
import jakarta.transaction.Transactional;

public interface AuthenticationService {
    RegisterResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    @Transactional
    void verifyAccount(String token) throws InvalidTokenException;

    void forgotPassword(String email);
    void resetPassword(String token, String newPassword) throws InvalidTokenException;
    }
