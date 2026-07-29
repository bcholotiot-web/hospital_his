package com.hospital.his.auth.controller;

import com.hospital.his.auth.dto.RegisterRequest;
import com.hospital.his.auth.dto.RegisterResponse;
import com.hospital.his.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
    @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
}
