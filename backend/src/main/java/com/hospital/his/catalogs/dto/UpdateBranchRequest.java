package com.hospital.his.catalogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;

/**
 * Request para actualizar una sucursal.
 *
 * El estado se actualiza mediante un endpoint
 * separado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBranchRequest {

    @NotBlank(
            message =
                    "El nombre de la sucursal es obligatorio."
    )
    @Size(
            max = 100,
            message =
                    "El nombre no puede exceder los 100 caracteres."
    )
    private String name;

    @Pattern(
            regexp = "^$|^[0-9]{8}$",
            message =
                    "El teléfono debe contener exactamente 8 dígitos."
    )
    private String phone;

    @Size(
            max = 500,
            message =
                    "La dirección no puede exceder los 500 caracteres."
    )
    private String address;

    @Size(
            max = 250,
            message =
                    "La descripción no puede exceder los 250 caracteres."
    )
    private String description;

    /**
     * Versión recibida por el frontend.
     */
    @NotNull(
            message =
                    "Debe indicar la versión de la sucursal."
    )
    private Long version;
}