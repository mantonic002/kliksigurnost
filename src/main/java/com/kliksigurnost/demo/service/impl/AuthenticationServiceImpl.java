package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.config.JwtService;
import com.kliksigurnost.demo.controller.auth.AuthenticationRequest;
import com.kliksigurnost.demo.controller.auth.AuthenticationResponse;
import com.kliksigurnost.demo.controller.auth.RegisterRequest;
import com.kliksigurnost.demo.model.Role;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.UserRepository;
import com.kliksigurnost.demo.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CloudflareAccountRepository cloudflareAccountRepository;

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        var cloudflareAcc = cloudflareAccountRepository.findFirstByUserNumIsLessThan(50);
        if (cloudflareAcc.isEmpty()) {
            return null;
        }
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .cloudflareAccount(cloudflareAcc.get())
                .build();
        repository.save(user);

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail()).orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
