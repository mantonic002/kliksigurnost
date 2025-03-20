package com.kliksigurnost.demo.config.oauth2;

import com.kliksigurnost.demo.model.*;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.UserRepository;
import com.kliksigurnost.demo.service.CloudflareAccountService;
import com.kliksigurnost.demo.service.CloudflarePolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudflareAccountRepository cloudflareAccountRepository;
    private final CloudflareAccountService cloudflareService;
    private final CloudflarePolicyService cloudflarePolicyService;
    private final Environment env;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Determine the provider (Google or Facebook)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String email;
        if (registrationId.equals("google")) {
            email = oAuth2User.getAttribute("email");
        } else if (registrationId.equals("facebook")) {
            email = oAuth2User.getAttribute("email");
            log.debug("Facebook email: {}", email);
        } else {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
                if (!user.isAccountNonLocked()) {
                    throw new OAuth2AuthenticationException("Account is locked");
                }
        } else {
            Optional<CloudflareAccount> cloudflareAccountOpt = cloudflareAccountRepository.findFirstByUserNumIsLessThan(50);
            if (cloudflareAccountOpt.isEmpty()) {
                throw new OAuth2AuthenticationException(env.getProperty("no-more-slots"));
            }

            CloudflareAccount cloudflareAccount = cloudflareAccountOpt.get();

            user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(Role.USER)
                    .cloudflareAccount(cloudflareAccount)
                    .authProvider(registrationId.equals("google") ? AuthProvider.GOOGLE : AuthProvider.FACEBOOK)
                    .isSetUp(false)
                    .enabled(true)
                    .build();

            cloudflareService.updateEnrollmentPolicyAddEmail(cloudflareAccount, email);

            User registeredUser = userRepository.save(user);

            cloudflarePolicyService.createDefaultPolicy(registeredUser);
        }

        return new CustomOAuth2User(oAuth2User, user);
    }


}