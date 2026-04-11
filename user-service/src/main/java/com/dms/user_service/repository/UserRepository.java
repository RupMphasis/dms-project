package com.dms.user_service.repository;

import com.dms.user_service.entity.ApprovalStatus;
import com.dms.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRole(User.Role role);
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndApprovalStatus(User.Role role, ApprovalStatus approvalStatus);
}