package com.hospital.his.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;

/**
 * Request para crear un rol.
 *
 * El backend crea todos los roles nuevos
 * con estado activo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoleRequest {

    /**
     * El nombre técnico admite letras y guion bajo.
     *
     * Ejemplos:
     *
     * ADMIN
     * MEDICO
     * SOPORTE_TECNICO
     */
    @NotBlank(
            message =
                    "El nombre del rol es obligatorio."
    )
    @Size(
            max = 50,
            message =
                    "El nombre no puede exceder los 50 caracteres."
    )
    @Pattern(
            regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ_ ]+$",
            message =
                    "El nombre del rol solamente puede contener letras, espacios y guion bajo."
    )
    private String name;

    @NotBlank(
            message =
                    "La descripción del rol es obligatoria."
    )
    @Size(
            max = 250,
            message =
                    "La descripción no puede exceder los 250 caracteres."
    )
    private String description;
}