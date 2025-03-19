package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.exception.InvalidTokenException;
import com.kliksigurnost.demo.model.ConfirmationToken;
import com.kliksigurnost.demo.repository.ConfirmationTokenRepository;
import com.kliksigurnost.demo.service.ConfirmationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConfirmationTokenServiceImpl implements ConfirmationTokenService {

    private final ConfirmationTokenRepository confirmationTokenRepository;

    @Override
    public ConfirmationToken save(ConfirmationToken confirmationToken) {
        confirmationTokenRepository.save(confirmationToken);
        return confirmationToken;
    }

    @Override
    public ConfirmationToken confirmToken(String token) throws InvalidTokenException {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Confirmation token not found"));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new InvalidTokenException("Token is already confirmed");
        }

        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token is expired");
        }

        confirmationToken.setConfirmedAt(LocalDateTime.now());
        confirmationTokenRepository.save(confirmationToken);

        return confirmationToken;
    }
}
