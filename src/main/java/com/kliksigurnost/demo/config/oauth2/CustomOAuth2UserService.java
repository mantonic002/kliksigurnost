package com.kliksigurnost.demo.config.oauth2;

import com.kliksigurnost.demo.model.AuthProvider;
import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.Role;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.UserRepository;
import com.kliksigurnost.demo.service.CloudflareAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudflareAccountRepository cloudflareAccountRepository;
    private final CloudflareAccountService cloudflareService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            Optional<CloudflareAccount> cloudflareAccountOpt = cloudflareAccountRepository.findFirstByUserNumIsLessThan(50);
            if (cloudflareAccountOpt.isEmpty()) {
                throw new OAuth2AuthenticationException("No more slots available.");
            }

            CloudflareAccount cloudflareAccount = cloudflareAccountOpt.get();

            user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(Role.USER)
                    .cloudflareAccount(cloudflareAccount)
                    .authProvider(AuthProvider.GOOGLE)
                    .isSetUp(false)
                    .enabled(true)
                    .build();

            cloudflareService.updateEnrollmentPolicyAddEmail(cloudflareAccount, email);

            userRepository.save(user);
        }

        return new CustomOAuth2User(oAuth2User, user);
    }
}