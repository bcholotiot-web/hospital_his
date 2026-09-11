package com.hospital.his.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(
            message = "El nombre de usuario es obligatorio."
    )
    private String username;

    @NotBlank(
            message = "La contraseña es obligatoria."
    )
    private String password;
}