package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.ConfirmationToken;

public interface ConfirmationTokenService {
    ConfirmationToken save(ConfirmationToken confirmationToken);

    ConfirmationToken confirmToken(String token);
}
