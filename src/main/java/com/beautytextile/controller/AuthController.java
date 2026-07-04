package com.beautytextile.controller;

import com.beautytextile.dto.AuthRequest;
import com.beautytextile.dto.AuthResponse;
import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.AdminUser;
import com.beautytextile.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository adminUserRepo;
    private final PasswordEncoder passwordEncoder;
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "BILLING");

    public AuthController(AuthenticationManager authenticationManager,
                          AdminUserRepository adminUserRepo,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.adminUserRepo         = adminUserRepo;
        this.passwordEncoder       = passwordEncoder;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        String username = requireUsername(req.username());
        String password = requirePassword(req.password());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        String role = adminUserRepo.findByUsername(username)
                .map(u -> normalizeRole(u.getRole(), "ADMIN"))
                .orElse("ADMIN");
        return new AuthResponse(username, role);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Map.of("success", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Not authenticated");
        }
        String username = authentication.getName();
        String role = adminUserRepo.findByUsername(username)
                .map(u -> normalizeRole(u.getRole(), "ADMIN"))
                .orElse("ADMIN");
        return Map.of("username", username, "role", role);
    }

    // ── Admin user management ─────────────────────────────────────────────

    /** List all admin/billing users (admin-only). */
    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        return adminUserRepo.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id",       u.getId(),
                        "username", u.getUsername(),
                        "role",     u.getRole() == null ? "ADMIN" : u.getRole()
                ))
                .toList();
    }

    /** Create billing or admin user. */
    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, String> req) {
        String username = requireUsername(req.get("username"));
        String password = requirePassword(req.get("password"));
        String role = normalizeRole(req.getOrDefault("role", "BILLING"), "BILLING");

        if (adminUserRepo.findByUsername(username).isPresent()) {
            throw new BusinessException("Username already exists");
        }

        AdminUser user = AdminUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();
        AdminUser saved = adminUserRepo.save(user);
        return Map.of("id", saved.getId(), "username", saved.getUsername(), "role", saved.getRole());
    }

    /** Delete a user by ID. Cannot delete self (handled on frontend). */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!adminUserRepo.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        adminUserRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Change a user's password. */
    @PutMapping("/users/{id}/password")
    public Map<String, Object> changePassword(@PathVariable Long id,
                                               @RequestBody Map<String, String> req) {
        AdminUser user = adminUserRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setPassword(passwordEncoder.encode(requirePassword(req.get("password"))));
        adminUserRepo.save(user);
        return Map.of("success", true);
    }

    private String requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Username is required");
        }
        String value = username.trim();
        if (value.length() < 3 || value.length() > 60) {
            throw new BusinessException("Username must be between 3 and 60 characters");
        }
        return value;
    }

    private String requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("Password is required");
        }
        String value = password.trim();
        if (value.length() < 6 || value.length() > 128) {
            throw new BusinessException("Password must be between 6 and 128 characters");
        }
        return value;
    }

    private String normalizeRole(String role, String defaultRole) {
        String normalized = role == null || role.isBlank()
                ? defaultRole
                : role.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new BusinessException("Role must be ADMIN or BILLING");
        }
        return normalized;
    }
}
