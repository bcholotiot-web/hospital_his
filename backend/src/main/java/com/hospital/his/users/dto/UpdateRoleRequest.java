package com.hospital.his.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;

/**
 * Request para actualizar un rol.
 *
 * Los roles técnicos protegidos no podrán cambiar
 * su nombre, pero sí su descripción.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoleRequest {

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

    @NotNull(
            message =
                    "Debe indicar la versión del rol."
    )
    private Long version;
}