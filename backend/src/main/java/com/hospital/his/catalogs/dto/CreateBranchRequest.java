package com.hospital.his.catalogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;

/**
 * Request para crear una sucursal.
 *
 * El estado no se recibe desde React.
 * Toda sucursal nueva se crea activa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchRequest {

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

    /**
     * Campo opcional.
     *
     * Cuando se proporciona, debe contener
     * exactamente ocho dígitos.
     */
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
}