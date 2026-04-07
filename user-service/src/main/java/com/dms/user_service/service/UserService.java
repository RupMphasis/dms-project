package com.dms.user_service.service;

import com.dms.user_service.client.AuditClient;
import com.dms.user_service.dto.*;
import com.dms.user_service.entity.User;
import com.dms.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditClient auditClient;

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt: {}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException(
                        "No user found with username: " + request.getUsername()));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect password. Please try again.");
        }
        log.info("Login SUCCESS: {}", request.getUsername());
        auditClient.logEvent(new AuditEventDto(
                null,
                "LOGIN",
                request.getUsername(),
                "USER",
                String.valueOf(user.getId()),
                "User login successful",
                LocalDateTime.now()
        ));
        return new LoginResponse(
                user.getId(), user.getUsername(), user.getRole().name(),
                user.getFullName(), user.getEmail(), "Login successful");
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        log.info("Register attempt: {}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username '" + request.getUsername() + "' is already taken.");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email '" + request.getEmail() + "' is already registered.");
            }
        }
        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .build();
        User saved = userRepository.save(newUser);
        log.info("User registered with id: {}", saved.getId());
        auditClient.logEvent(new AuditEventDto(
                null,
                "USER_REGISTER",
                saved.getUsername(),
                "USER",
                String.valueOf(saved.getId()),
                "New user registered",
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public String changePassword(ChangePasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException(
                        "No user found with username: " + request.getUsername()));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password is incorrect.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password must be different from current password.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditClient.logEvent(new AuditEventDto(
                null,
                "PASSWORD_CHANGE",
                request.getUsername(),
                "USER",
                String.valueOf(user.getId()),
                "Password changed",
                LocalDateTime.now()
        ));
        return "Password changed successfully for user: " + request.getUsername();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> getDistributors() {
        return getUsersByRole(User.Role.DISTRIBUTOR);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("No user found with username: " + username));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No user found with id: " + id));
    }

    @Transactional
    public String deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No user found with id: " + id));
        if ("admin".equals(user.getUsername())) {
            throw new RuntimeException("Cannot delete the default system admin account.");
        }
        userRepository.deleteById(id);
        return "User deleted successfully. ID: " + id;
    }
}