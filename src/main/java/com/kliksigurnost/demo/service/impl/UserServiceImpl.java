package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.model.UserProfile;
import com.kliksigurnost.demo.repository.UserRepository;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<UserProfile> getAllUsers() {
        return repository.findAll().stream()
                .map(UserProfile::new)
                .collect(Collectors.toList());
    }

    @Override
    public User updateUser(User user) {
        return repository.save(user);
    }

    @Override
    public User getById(Integer id) {
        return repository.findById(id).orElseThrow();
    }

    @Override
    public User switchUserLocked(Integer id) {
        User user = getById(id);
        user.setLocked(!user.getLocked());
        return repository.save(user);
    }
}
