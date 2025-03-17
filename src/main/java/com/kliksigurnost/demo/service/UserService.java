package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.model.UserProfile;

import java.util.List;

public interface UserService {
    User getCurrentUser();

    List<UserProfile> getAllUsers();

    User updateUser(User user);

    User getById(Integer id);

    User switchUserLocked(Integer id);
}
