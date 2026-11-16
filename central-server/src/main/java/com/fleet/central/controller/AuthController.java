package com.fleet.central.controller;

import com.fleet.central.domain.User;
import com.fleet.central.dto.AuthRequest;
import com.fleet.central.dto.AuthResponse;
import com.fleet.central.dto.RegisterRequest;
import com.fleet.central.repository.UserRepository;
import com.fleet.central.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User & Admin JWT Authentication")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          JwtService jwtService,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT token")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUsername()));

        String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getFullName());

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getUsername(),
                user.getRole(),
                user.getFullName(),
                86400000L
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User newUser = new User(
                "usr_" + UUID.randomUUID().toString().substring(0, 8),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                "ROLE_USER",
                request.getFullName()
        );
        userRepository.save(newUser);

        String token = jwtService.generateToken(newUser.getUsername(), newUser.getRole(), newUser.getFullName());

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                token,
                newUser.getUsername(),
                newUser.getRole(),
                newUser.getFullName(),
                86400000L
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userRepository.findByUsername(auth.getName()).orElse(null));
    }
}
