package com.hospital.his.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class VerifyDpiRequest {

    @NotBlank(
            message =
                    "El campo DPI es obligatorio. " +
                            "Por favor, ingrese su número de DPI."
    )
    @Size(
            min = 13,
            max = 13,
            message =
                    "El DPI debe contener exactamente 13 dígitos."
    )
    @Pattern(
            regexp = "^\\d{13}$",
            message =
                    "El DPI debe contener únicamente números. " +
                            "No se permiten letras ni caracteres especiales."
    )
    private String dpi;
}