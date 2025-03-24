package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.config.JwtService;
import com.kliksigurnost.demo.controller.auth.AuthenticationRequest;
import com.kliksigurnost.demo.controller.auth.AuthenticationResponse;
import com.kliksigurnost.demo.controller.auth.RegisterRequest;
import com.kliksigurnost.demo.controller.auth.RegisterResponse;
import com.kliksigurnost.demo.exception.InvalidTokenException;
import com.kliksigurnost.demo.helper.EmailTemplateService;
import com.kliksigurnost.demo.helper.EmailValidator;
import com.kliksigurnost.demo.model.*;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.UserRepository;
import com.kliksigurnost.demo.service.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final EmailValidator emailValidator;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CloudflareAccountRepository cloudflareAccountRepository;
    private final CloudflareAccountService cloudflareService;
    private final CloudflarePolicyService cloudflarePolicyService;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailSenderService emailSenderService;
    private final EmailTemplateService emailTemplateService;

    private final Environment env;

    @Value("${frontend.url}")
    private String frontendUrl;


    @Value("${backend.url}")
    private String backendUrl;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!emailValidator.test(request.getEmail())) {
            return RegisterResponse.builder().error(env.getProperty("email-invalid")).build();
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return RegisterResponse.builder().error(env.getProperty("email-user-exists")).build();
        }

        Optional<CloudflareAccount> cloudflareAccountOpt = cloudflareAccountRepository.findFirstByUserNumIsLessThan(50);
        if (cloudflareAccountOpt.isEmpty()) {
            return RegisterResponse.builder().error(env.getProperty("no-more-slots")).build();
        }

        CloudflareAccount cloudflareAccount = cloudflareAccountOpt.get();
        User user = createUser(request, cloudflareAccount);

        cloudflareService.updateEnrollmentPolicyAddEmail(cloudflareAccount, request.getEmail());

        User registeredUser = userRepository.save(user);

        ConfirmationToken token = createConfirmationToken(registeredUser);
        emailSenderService.sendEmail(
                user.getEmail(),
                env.getProperty("verify-email-title"),
                emailTemplateService.buildAccVerificationEmail(user.getEmail(), backendUrl + "/api/auth/verify?token=" + token.getToken())
        );
        cloudflarePolicyService.createDefaultPolicy(registeredUser);

        return RegisterResponse.builder().message(env.getProperty("verify-email-sent")).build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            return buildErrorResponse(e.getMessage());
        }
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return buildJwtResponse(user);
    }


    @Override
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            ConfirmationToken confirmationToken = ConfirmationToken.builder()
                    .token(token)
                    .tokenType(TokenType.PASSWORD_RESET)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .user(user)
                    .build();
            confirmationTokenService.save(confirmationToken);
            emailSenderService.sendEmail(
                    user.getEmail(),
                    env.getProperty("reset-pw-email-title"),
                    emailTemplateService.buildPasswordResetEmail(user.getEmail(), frontendUrl + "/reset-password?token=" + token)
            );
        });
    }

    @Transactional
    @Override
    public void verifyAccount(String token) throws InvalidTokenException {
        ConfirmationToken confirmationToken = confirmationTokenService.confirmToken(token);

        User user = confirmationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) throws InvalidTokenException {
        ConfirmationToken confirmationToken = confirmationTokenService.confirmToken(token);

        User user = confirmationToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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

    private ConfirmationToken createConfirmationToken(User user) {
        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = ConfirmationToken.builder()
                .token(token)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .user(user)
                .build();
        return confirmationTokenService.save(confirmationToken);
    }
}