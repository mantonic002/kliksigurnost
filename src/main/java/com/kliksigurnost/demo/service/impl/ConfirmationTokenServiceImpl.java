package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.exception.NotFoundException;
import com.kliksigurnost.demo.model.ConfirmationToken;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.ConfirmationTokenRepository;
import com.kliksigurnost.demo.service.ConfirmationTokenService;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConfirmationTokenServiceImpl implements ConfirmationTokenService {

    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final UserService userService;

    @Override
    public ConfirmationToken save(ConfirmationToken confirmationToken) {
        confirmationTokenRepository.save(confirmationToken);
        return confirmationToken;
    }

    @Override
    public String confirmToken(String token) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Confirmation token not found"));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new IllegalStateException("Token is already confirmed");
        }

        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token is expired");
        }

        User user = confirmationToken.getUser();
        user.setEnabled(true);
        userService.updateUser(user);

        confirmationToken.setConfirmedAt(LocalDateTime.now());
        confirmationTokenRepository.save(confirmationToken);

        return confirmationToken.getToken();
    }
}
