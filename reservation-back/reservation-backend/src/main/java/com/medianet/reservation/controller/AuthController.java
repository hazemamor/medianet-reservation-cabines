package com.medianet.reservation.controller;

import com.medianet.reservation.dto.AuthResponse;
import com.medianet.reservation.dto.GoogleAuthRequest;
import com.medianet.reservation.dto.LoginRequest;
import com.medianet.reservation.dto.VerifyOtpRequest;
import com.medianet.reservation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> demanderOtp(@Valid @RequestBody LoginRequest request) {
        authService.demanderOtp(request.email());
        return ResponseEntity.ok(Map.of("message", "Un code OTP a été envoyé à " + request.email()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifierOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifierOtp(request.email(), request.code()));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> connexionGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.connexionAvecGoogle(request.idToken()));
    }
}
