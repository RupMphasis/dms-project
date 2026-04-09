package com.dms.user_service.service;

import com.dms.user_service.client.AuditClient;
import com.dms.user_service.dto.*;
import com.dms.user_service.entity.User;
import com.dms.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditClient auditClient;
    private final RestTemplate restTemplate;

    @Value("${distributor.service.url:http://localhost:8087}")
    private String distributorServiceUrl;

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

        String fullName = user.getFullName();
        String email = user.getEmail();
        if (user.getRole() == User.Role.DISTRIBUTOR && user.getDistributorId() != null) {
            DistributorDto distributor = getDistributorById(user.getDistributorId());
            if (distributor != null) {
                fullName = distributor.getName();
                email = distributor.getEmail();
            }
        }

        return new LoginResponse(
                user.getId(), user.getUsername(), user.getRole().name(),
                fullName, email, "Login successful");
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        log.info("Register attempt: {}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username '" + request.getUsername() + "' is already taken.");
        }
        if (request.getRole() == User.Role.ADMIN) {
            if (request.getFullName() == null || request.getFullName().isBlank()) {
                throw new RuntimeException("Full name is required for admin registration.");
            }
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new RuntimeException("Email is required for admin registration.");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email '" + request.getEmail() + "' is already registered.");
            }
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .fullName(request.getRole() == User.Role.ADMIN ? request.getFullName() : null)
                .email(request.getRole() == User.Role.ADMIN ? request.getEmail() : null)
                .build();

        if (request.getRole() == User.Role.DISTRIBUTOR) {
            validateDistributorRegistration(request);
            DistributorDto distributor = new DistributorDto();
            distributor.setName(request.getDistributorName());
            distributor.setEmail(request.getDistributorEmail());
            distributor.setCity(request.getDistributorCity());
            distributor.setContact(request.getDistributorContact());

            DistributorDto createdDistributor = restTemplate.postForObject(
                    distributorServiceUrl + "/distributors",
                    distributor,
                    DistributorDto.class
            );

            if (createdDistributor == null || createdDistributor.getId() == null) {
                throw new RuntimeException("Unable to create distributor details.");
            }
            newUser.setDistributorId(createdDistributor.getId());
        }

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

    private void validateDistributorRegistration(RegisterRequest request) {
        if (request.getDistributorName() == null || request.getDistributorName().isBlank()) {
            throw new RuntimeException("Distributor name is required for distributor registration.");
        }
        if (request.getDistributorEmail() == null || request.getDistributorEmail().isBlank()) {
            throw new RuntimeException("Distributor email is required for distributor registration.");
        }
        if (request.getDistributorCity() == null || request.getDistributorCity().isBlank()) {
            throw new RuntimeException("Distributor city is required for distributor registration.");
        }
        if (request.getDistributorContact() == null || request.getDistributorContact().isBlank()) {
            throw new RuntimeException("Distributor contact is required for distributor registration.");
        }
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

    @Transactional
    public User updateUserProfile(Long userId, UserProfileDto profileDto) {
        User user = getUserById(userId);
        if (profileDto.getEmail() != null && !profileDto.getEmail().isBlank()
                && !profileDto.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(profileDto.getEmail())) {
                throw new RuntimeException("Email '" + profileDto.getEmail() + "' is already registered.");
            }
            user.setEmail(profileDto.getEmail());
        }
        if (profileDto.getFullName() != null) {
            user.setFullName(profileDto.getFullName());
        }

        if (profileDto.getNewPassword() != null && !profileDto.getNewPassword().isBlank()) {
            if (profileDto.getOldPassword() == null || profileDto.getOldPassword().isBlank()) {
                throw new RuntimeException("Old password is required to change the password.");
            }
            if (!passwordEncoder.matches(profileDto.getOldPassword(), user.getPassword())) {
                throw new RuntimeException("Old password is incorrect.");
            }
            if (!profileDto.getNewPassword().equals(profileDto.getConfirmNewPassword())) {
                throw new RuntimeException("New password and confirmation do not match.");
            }
            if (passwordEncoder.matches(profileDto.getNewPassword(), user.getPassword())) {
                throw new RuntimeException("New password must be different from current password.");
            }
            user.setPassword(passwordEncoder.encode(profileDto.getNewPassword()));
        }

        User saved = userRepository.save(user);
        auditClient.logEvent(new AuditEventDto(
                null,
                "USER_PROFILE_UPDATED",
                saved.getUsername(),
                "USER",
                String.valueOf(saved.getId()),
                "User profile updated",
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public User updateDistributorDetails(Long distributorUserId, DistributorUpdateDto updateDto) {
        User user = getUserById(distributorUserId);
        if (user.getRole() != User.Role.DISTRIBUTOR) {
            throw new RuntimeException("Only distributors can be updated with this operation.");
        }
        if (user.getDistributorId() == null) {
            throw new RuntimeException("Distributor reference is missing for this user.");
        }

        DistributorDto distributor = new DistributorDto();
        distributor.setName(updateDto.getName());
        distributor.setEmail(updateDto.getEmail());
        distributor.setCity(updateDto.getCity());
        distributor.setContact(updateDto.getContact());

        restTemplate.put(distributorServiceUrl + "/distributors/" + user.getDistributorId(), distributor);

        User saved = userRepository.save(user);
        auditClient.logEvent(new AuditEventDto(
                null,
                "DISTRIBUTOR_UPDATED",
                saved.getUsername(),
                "USER",
                String.valueOf(saved.getId()),
                "Distributor details updated by admin",
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public User updateDistributorProfile(Long userId, DistributorProfileDto profileDto) {
        User user = getUserById(userId);
        if (user.getRole() != User.Role.DISTRIBUTOR) {
            throw new RuntimeException("Only distributors can update this profile.");
        }
        if (user.getDistributorId() == null) {
            throw new RuntimeException("Distributor reference is missing for this user.");
        }

        DistributorDto distributor = new DistributorDto();
        distributor.setName(profileDto.getName());
        distributor.setEmail(profileDto.getEmail());
        distributor.setCity(profileDto.getCity());
        distributor.setContact(profileDto.getContact());

        restTemplate.put(distributorServiceUrl + "/distributors/" + user.getDistributorId(), distributor);

        if (profileDto.getNewPassword() != null && !profileDto.getNewPassword().isBlank()) {
            if (profileDto.getOldPassword() == null || profileDto.getOldPassword().isBlank()) {
                throw new RuntimeException("Old password is required to change the password.");
            }
            if (!passwordEncoder.matches(profileDto.getOldPassword(), user.getPassword())) {
                throw new RuntimeException("Old password is incorrect.");
            }
            if (!profileDto.getNewPassword().equals(profileDto.getConfirmNewPassword())) {
                throw new RuntimeException("New password and confirmation do not match.");
            }
            if (passwordEncoder.matches(profileDto.getNewPassword(), user.getPassword())) {
                throw new RuntimeException("New password must be different from current password.");
            }
            user.setPassword(passwordEncoder.encode(profileDto.getNewPassword()));
        }

        User saved = userRepository.save(user);
        auditClient.logEvent(new AuditEventDto(
                null,
                "DISTRIBUTOR_PROFILE_UPDATED",
                saved.getUsername(),
                "USER",
                String.valueOf(saved.getId()),
                "Distributor profile updated",
                LocalDateTime.now()
        ));
        return saved;
    }

    public DistributorDto getDistributorById(Long distributorId) {
        if (distributorId == null) {
            return null;
        }
        return restTemplate.getForObject(distributorServiceUrl + "/distributors/" + distributorId, DistributorDto.class);
    }

    public DistributorViewDto getDistributorViewByUserId(Long userId) {
        User user = getUserById(userId);
        if (user.getRole() != User.Role.DISTRIBUTOR) {
            throw new RuntimeException("User is not a distributor.");
        }
        DistributorDto distributor = getDistributorById(user.getDistributorId());
        if (distributor == null) {
            throw new RuntimeException("Distributor details not found for user.");
        }
        return new DistributorViewDto(
                user.getId(),
                user.getUsername(),
                user.getDistributorId(),
                distributor.getName(),
                distributor.getEmail(),
                distributor.getCity(),
                distributor.getContact()
        );
    }

    public List<DistributorViewDto> getDistributorViews() {
        return getDistributors().stream()
                .map(user -> {
                    DistributorDto distributor = getDistributorById(user.getDistributorId());
                    return new DistributorViewDto(
                            user.getId(),
                            user.getUsername(),
                            user.getDistributorId(),
                            distributor != null ? distributor.getName() : null,
                            distributor != null ? distributor.getEmail() : null,
                            distributor != null ? distributor.getCity() : null,
                            distributor != null ? distributor.getContact() : null
                    );
                })
                .collect(Collectors.toList());
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