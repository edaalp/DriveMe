package com.driveme.backend.service;

import com.driveme.backend.dto.AdminSignUpRequest;
import com.driveme.backend.entity.Admin;
import com.driveme.backend.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing admin operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new admin account.
     *
     * @param request the sign-up request
     * @return the created admin
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public Admin signUp(AdminSignUpRequest request) {
        log.info("Attempting to register admin with email: {}", request.getEmail());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Admin admin = new Admin();
        admin.setEmail(request.getEmail());
        admin.setFullName(request.getFullName());
        admin.setUserName(request.getEmail()); // use email as username for admins
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setActive(true);

        Admin saved = adminRepository.save(admin);
        log.info("Admin registered with ID: {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }
}
