package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.config.JwtService;
import com.kliksigurnost.demo.controller.auth.AuthenticationRequest;
import com.kliksigurnost.demo.controller.auth.AuthenticationResponse;
import com.kliksigurnost.demo.controller.auth.RegisterRequest;
import com.kliksigurnost.demo.model.*;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.UserRepository;
import com.kliksigurnost.demo.service.AuthenticationService;
import com.kliksigurnost.demo.service.CloudflareAccountService;
import com.kliksigurnost.demo.service.CloudflarePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CloudflareAccountRepository cloudflareAccountRepository;
    private final CloudflareAccountService cloudflareService;
    private final CloudflarePolicyService cloudflarePolicyService;

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return buildErrorResponse("User with that email already exists");
        }

        Optional<CloudflareAccount> cloudflareAccountOpt = cloudflareAccountRepository.findFirstByUserNumIsLessThan(50);
        if (cloudflareAccountOpt.isEmpty()) {
            return buildErrorResponse("No more slots, try again later");
        }

        CloudflareAccount cloudflareAccount = cloudflareAccountOpt.get();
        User user = createUser(request, cloudflareAccount);
        userRepository.save(user);

        updateCloudflareAccount(cloudflareAccount, request.getEmail());

        createDefaultPolicy(user);

        return buildJwtResponse(user);
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return buildJwtResponse(user);
    }

    @Override
    public AuthenticationResponse authenticateRegisterOAuth2Google(OAuth2AuthenticationToken authToken) {
        OAuth2User oAuth2User = authToken.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            return buildJwtResponse(existingUser.get());
        }

        Optional<CloudflareAccount> cloudflareAccountOpt = cloudflareAccountRepository.findFirstByUserNumIsLessThan(50);
        if (cloudflareAccountOpt.isEmpty()) {
            return buildErrorResponse("No more slots, try again later");
        }

        CloudflareAccount cloudflareAccount = cloudflareAccountOpt.get();
        User user = createOAuthUser(email, cloudflareAccount);
        userRepository.save(user);

        updateCloudflareAccount(cloudflareAccount, email);
        createDefaultPolicy(user);

        return buildJwtResponse(user);
    }

    private User createUser(RegisterRequest request, CloudflareAccount cloudflareAccount) {
        return User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .cloudflareAccount(cloudflareAccount)
                .isSetUp(false)
                .build();
    }

    private User createOAuthUser(String email, CloudflareAccount cloudflareAccount) {
        return User.builder()
                .email(email)
                .role(Role.USER)
                .authProvider(AuthProvider.GOOGLE)
                .cloudflareAccount(cloudflareAccount)
                .isSetUp(false)
                .build();
    }

    private void updateCloudflareAccount(CloudflareAccount cloudflareAccount, String email) {
        cloudflareService.updateEnrollmentPolicyAddEmail(cloudflareAccount.getAccountId(), email);
        cloudflareAccount.setUserNum(cloudflareAccount.getUserNum() + 1);
        cloudflareAccountRepository.save(cloudflareAccount);
    }

    private AuthenticationResponse buildErrorResponse(String errorMessage) {
        return AuthenticationResponse.builder().error(errorMessage).build();
    }

    private AuthenticationResponse buildJwtResponse(User user) {
        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user); // Generate refresh token
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void createDefaultPolicy(User user) {
        String defaultTrafficString = "any(dns.content_category[*] in {2 67 125 133 8 99})"; //adult themes and gambling
        CloudflarePolicy policy = CloudflarePolicy.builder()
                .action("block")
                .schedule(null)
                .traffic(defaultTrafficString)
                .build();
        cloudflarePolicyService.createPolicy(policy, user);
    }
}