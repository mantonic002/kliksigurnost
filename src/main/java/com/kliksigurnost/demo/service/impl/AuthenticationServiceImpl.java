package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.config.JwtService;
import com.kliksigurnost.demo.controller.auth.AuthenticationRequest;
import com.kliksigurnost.demo.controller.auth.AuthenticationResponse;
import com.kliksigurnost.demo.controller.auth.RegisterRequest;
import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.Role;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.UserRepository;
import com.kliksigurnost.demo.service.AuthenticationService;
import com.kliksigurnost.demo.service.CloudflareService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository repository;

    // security fields
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // cloudflare connection fields
    private final CloudflareAccountRepository cloudflareAccountRepository;
    private final CloudflareService cloudflareService;

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        // check if user already exists
        if(repository.existsByEmail(request.getEmail()))
        {
            return AuthenticationResponse.builder()
                    .error("User with that email already exists")
                    .build();
        }

        // find cloudflare account with free seat
        var cloudflareAcc = cloudflareAccountRepository.findFirstByUserNumIsLessThan(50);
        if (cloudflareAcc.isEmpty()) {
            return AuthenticationResponse.builder()
                    .error("No more slots, try again later")
                    .build();
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



        CloudflareAccount acc = cloudflareAcc.get();

        // update enrollment policy to contain registered user
        cloudflareService.updateEnrollmentPolicyAddEmail(acc.getAccountId(), request.getEmail());

        // when user is created update userNum in cloudflare acc
        acc.setUserNum(acc.getUserNum() + 1);
        cloudflareAccountRepository.save(acc);

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
