package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.UserRepository;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    @Override
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return repository.findByEmail(email).orElseThrow();
    }

    @Override
    public User updateUser(User user) {
        return repository.save(user);
    }
}
