package com.beautytextile.controller;

import com.beautytextile.config.JwtService;
import com.beautytextile.dto.AuthRequest;
import com.beautytextile.dto.AuthResponse;
import com.beautytextile.model.AdminUser;
import com.beautytextile.repository.AdminUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AdminUserRepository adminUserRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          AdminUserRepository adminUserRepo,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService            = jwtService;
        this.adminUserRepo         = adminUserRepo;
        this.passwordEncoder       = passwordEncoder;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        String token = jwtService.generateToken(req.username());
        String role = adminUserRepo.findByUsername(req.username())
                .map(u -> u.getRole() == null ? "ADMIN" : u.getRole())
                .orElse("ADMIN");
        return new AuthResponse(token, req.username(), role);
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
        String username = req.get("username");
        String password = req.get("password");
        String role     = req.getOrDefault("role", "BILLING");
        if (adminUserRepo.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        AdminUser user = AdminUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role.toUpperCase())
                .build();
        AdminUser saved = adminUserRepo.save(user);
        return Map.of("id", saved.getId(), "username", saved.getUsername(), "role", saved.getRole());
    }

    /** Delete a user by ID. Cannot delete self (handled on frontend). */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminUserRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Change a user's password. */
    @PutMapping("/users/{id}/password")
    public Map<String, Object> changePassword(@PathVariable Long id,
                                               @RequestBody Map<String, String> req) {
        AdminUser user = adminUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setPassword(passwordEncoder.encode(req.get("password")));
        adminUserRepo.save(user);
        return Map.of("success", true);
    }
}
