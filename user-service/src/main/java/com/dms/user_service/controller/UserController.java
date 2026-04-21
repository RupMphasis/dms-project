package com.dms.user_service.controller;

import com.dms.user_service.dto.AdminDistributorRegisterRequest;
import com.dms.user_service.dto.AdminRegisterRequest;
import com.dms.user_service.dto.DistributorRegisterRequest;
import com.dms.user_service.dto.LoginRequest;
import com.dms.user_service.dto.LoginResponse;
import com.dms.user_service.dto.UserUpdateRequest;
import com.dms.user_service.entity.User;
import com.dms.user_service.security.CustomUserDetails;
import com.dms.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerDistributor(@Valid @RequestBody DistributorRegisterRequest request) {
        return ResponseEntity.ok(userService.registerDistributor(request));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<User> registerAdmin(@Valid @RequestBody AdminRegisterRequest request,
                                              Authentication authentication) {
        if (userService.hasAdmin()) {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
            boolean hasAdminRole = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            if (!hasAdminRole) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(userService.registerAdmin(request));
    }

    @PostMapping("/admin/distributors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> registerDistributorByAdmin(@Valid @RequestBody AdminDistributorRegisterRequest request) {
        return ResponseEntity.ok(userService.registerDistributorByAdmin(request));
    }

    @GetMapping
    public ResponseEntity<List<User>> getUsers(@RequestParam(value = "role", required = false) String role) {
        return ResponseEntity.ok(userService.getUsers(role));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                           @Valid @RequestBody UserUpdateRequest request,
                                           Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean isSelfUpdate = false;
        if (authentication.getPrincipal() instanceof CustomUserDetails currentUser) {
            isSelfUpdate = currentUser.getId().equals(id);
        }
        return ResponseEntity.ok(userService.updateUser(id, request, isAdmin, isSelfUpdate));
    }

    @PutMapping("/distributors/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> approveDistributor(@PathVariable Long id) {
        return ResponseEntity.ok(userService.approveDistributor(id));
    }

    @PutMapping("/distributors/{id}/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> denyDistributor(@PathVariable Long id) {
        return ResponseEntity.ok(userService.denyDistributor(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", userService.deleteUser(id)));
    }
}