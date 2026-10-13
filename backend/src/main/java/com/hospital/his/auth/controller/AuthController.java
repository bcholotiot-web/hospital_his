package com.hospital.his.auth.controller;

import com.hospital.his.auth.dto.LoginRequest;
import com.hospital.his.auth.dto.LoginResponse;
import com.hospital.his.auth.dto.RegisterRequest;
import com.hospital.his.auth.dto.RegisterResponse;
import com.hospital.his.auth.service.AuthService;
import com.hospital.his.auth.dto.VerifyDpiRequest;
import com.hospital.his.auth.dto.VerifyDpiResponse;
import com.hospital.his.auth.dto.AuthenticatedUserResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService
    ) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody RegisterRequest request
    ) {
        RegisterResponse response =
                authService.register(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Devuelve los datos y permisos efectivos
     * de la sesión autenticada.
     *
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse>
    getAuthenticatedUser(
            Authentication authentication
    ) {
        if (
                authentication == null ||
                        !authentication.isAuthenticated()
        ) {
            throw new RuntimeException(
                    "No existe una sesión autenticada."
            );
        }

        return ResponseEntity.ok(
                authService
                        .getAuthenticatedUser(
                                authentication.getName()
                        )
        );
    }

    /**
     * CU-**: Verificar DPI antes de agendar **a cita.
     *
     * Este endpoint es pú**ico porque se utiliza antes
     * de**inicio de sesión.
     */
    @PostMapping("/verify-dpi")
    public ResponseEntity<VerifyDpiResponse> verifyDpi(
            @Valid
            @RequestBody VerifyDpiRequest request
    ) {
        VerifyDpiResponse response =
                authService.verifyDpi(request);

        return ResponseEntity.ok(response);
    }
}
