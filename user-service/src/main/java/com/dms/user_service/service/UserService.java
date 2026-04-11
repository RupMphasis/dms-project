package com.dms.user_service.service;

import com.dms.user_service.client.AuditClient;
import com.dms.user_service.dto.*;
import com.dms.user_service.entity.ApprovalStatus;
import com.dms.user_service.entity.User;
import com.dms.user_service.repository.UserRepository;
import com.dms.user_service.security.JwtUtils;
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
    private final JwtUtils jwtUtils;

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
        if (user.getRole() == User.Role.DISTRIBUTOR && user.getApprovalStatus() != ApprovalStatus.APPROVED) {
            if (user.getApprovalStatus() == ApprovalStatus.DENIED) {
                throw new RuntimeException("Distributor account has been denied.");
            }
            throw new RuntimeException("Distributor account is pending administrator approval.");
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

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());

        return new LoginResponse(
                user.getId(), user.getUsername(), user.getRole().name(),
                fullName, email, token, "Login successful");
    }

    @Transactional
    public User registerDistributor(DistributorRegisterRequest request) {
        log.info("Register distributor attempt: {}", request.getUsername());
        ensureUsernameAvailable(request.getUsername());
        ensurePasswordsMatch(request.getPassword(), request.getConfirmPassword());
        validateDistributorRegistration(request);

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.DISTRIBUTOR)
                .approvalStatus(ApprovalStatus.PENDING)
                .build();

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

        User saved = userRepository.save(newUser);
        log.info("Distributor saved with id: {}", saved.getId());
        auditClient.logEvent(new AuditEventDto(
                null,
                "DISTRIBUTOR_REGISTER",
                saved.getUsername(),
                "USER",
                String.valueOf(saved.getId()),
                "New distributor registered",
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public User registerAdmin(AdminRegisterRequest request) {
        log.info("Register admin attempt: {}", request.getUsername());
        ensureUsernameAvailable(request.getUsername());
        ensurePasswordsMatch(request.getPassword(), request.getConfirmPassword());
        ensureEmailAvailable(request.getEmail());

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ADMIN)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        User saved = userRepository.save(newUser);
        log.info("Admin saved with id: {}", saved.getId());
        auditClient.logEvent(new AuditEventDto(
                null,
                "ADMIN_REGISTER",
                saved.getUsername(),
                "USER",
                String.valueOf(saved.getId()),
                "New admin registered",
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (request.getRole() == null) {
            throw new RuntimeException("Role is required for registration.");
        }
        if (request.getRole() == User.Role.ADMIN) {
            AdminRegisterRequest adminRequest = new AdminRegisterRequest();
            adminRequest.setUsername(request.getUsername());
            adminRequest.setPassword(request.getPassword());
            adminRequest.setConfirmPassword(request.getConfirmPassword());
            adminRequest.setFullName(request.getFullName());
            adminRequest.setEmail(request.getEmail());
            return registerAdmin(adminRequest);
        } else if (request.getRole() == User.Role.DISTRIBUTOR) {
            DistributorRegisterRequest distributorRequest = new DistributorRegisterRequest();
            distributorRequest.setUsername(request.getUsername());
            distributorRequest.setPassword(request.getPassword());
            distributorRequest.setConfirmPassword(request.getConfirmPassword());
            distributorRequest.setDistributorName(request.getDistributorName());
            distributorRequest.setDistributorEmail(request.getDistributorEmail());
            distributorRequest.setDistributorCity(request.getDistributorCity());
            distributorRequest.setDistributorContact(request.getDistributorContact());
            return registerDistributor(distributorRequest);
        }
        throw new RuntimeException("Unsupported role for registration: " + request.getRole());
    }

    @Transactional
    public User updateUser(Long id, UserUpdateRequest request) {
        User user = getUserById(id);
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getUsername())) {
            ensureUsernameAvailable(request.getUsername());
            user.setUsername(request.getUsername());
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().isBlank() && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
                ensureEmailAvailable(request.getEmail());
                user.setEmail(request.getEmail());
            }
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            ensurePasswordsMatch(request.getNewPassword(), request.getConfirmPassword());
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        User saved = userRepository.save(user);
        auditClient.logEvent(new AuditEventDto(
                null,
                "USER_UPDATE",
                saved.getUsername(),
                "USER",
                String.valueOf(saved.getId()),
                "User updated",
                LocalDateTime.now()
        ));
        return saved;
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username '" + username + "' is already taken.");
        }
    }

    private void ensureEmailAvailable(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required for admin registration.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email '" + email + "' is already registered.");
        }
    }

    private void ensurePasswordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            throw new RuntimeException("Password and confirm password must match.");
        }
    }

    private void validateDistributorRegistration(DistributorRegisterRequest request) {
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
                distributor.getContact(),
                user.getApprovalStatus().name()
        );
    }

    private DistributorViewDto toDistributorView(User user) {
        DistributorDto distributor = getDistributorById(user.getDistributorId());
        return new DistributorViewDto(
                user.getId(),
                user.getUsername(),
                user.getDistributorId(),
                distributor != null ? distributor.getName() : null,
                distributor != null ? distributor.getEmail() : null,
                distributor != null ? distributor.getCity() : null,
                distributor != null ? distributor.getContact() : null,
                user.getApprovalStatus().name()
        );
    }

    public List<DistributorViewDto> getDistributorViews() {
        return getDistributors().stream()
                .map(this::toDistributorView)
                .collect(Collectors.toList());
    }

    public List<DistributorViewDto> getPendingDistributorViews() {
        return getPendingDistributors().stream()
                .map(this::toDistributorView)
                .collect(Collectors.toList());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean hasAdmin() {
        return userRepository.countByRole(User.Role.ADMIN) > 0;
    }

    public List<User> getUsers(String role) {
        if (role == null || role.isBlank()) {
            return getAllUsers();
        }
        try {
            return getUsersByRole(User.Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid role filter: " + role);
        }
    }

    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> getPendingDistributors() {
        return userRepository.findByRoleAndApprovalStatus(User.Role.DISTRIBUTOR, ApprovalStatus.PENDING);
    }

    public List<User> getDistributors() {
        return userRepository.findByRoleAndApprovalStatus(User.Role.DISTRIBUTOR, ApprovalStatus.APPROVED);
    }

    @Transactional
    public User approveDistributor(Long userId) {
        User user = getUserById(userId);
        if (user.getRole() != User.Role.DISTRIBUTOR) {
            throw new RuntimeException("Only distributor accounts can be approved.");
        }
        if (user.getApprovalStatus() == ApprovalStatus.APPROVED) {
            return user;
        }
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        User saved = userRepository.save(user);
        auditClient.logEvent(new AuditEventDto(
                null,
                "DISTRIBUTOR_APPROVED",
                "ADMIN",
                "USER",
                String.valueOf(saved.getId()),
                "Distributor account approved",
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public User denyDistributor(Long userId) {
        User user = getUserById(userId);
        if (user.getRole() != User.Role.DISTRIBUTOR) {
            throw new RuntimeException("Only distributor accounts can be denied.");
        }
        if (user.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException("Only pending distributor accounts can be denied.");
        }
        if (user.getDistributorId() != null) {
            restTemplate.delete(distributorServiceUrl + "/distributors/" + user.getDistributorId());
        }
        user.setApprovalStatus(ApprovalStatus.DENIED);
        User saved = userRepository.save(user);
        auditClient.logEvent(new AuditEventDto(
                null,
                "DISTRIBUTOR_DENIED",
                "ADMIN",
                "USER",
                String.valueOf(saved.getId()),
                "Distributor account denied",
                LocalDateTime.now()
        ));
        return saved;
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