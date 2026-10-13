package com.hospital.his.catalogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request para actualizar una especialidad.
 *
 * El estado se modifica mediante un endpoint
 * separado para evitar actualizaciones accidentales.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSpecialtyRequest {

    @NotBlank(
            message =
                    "El nombre de la especialidad es obligatorio."
    )
    @Size(
            max = 100,
            message =
                    "El nombre no puede exceder los 100 caracteres."
    )
    private String name;

    @NotBlank(
            message =
                    "La descripción de la especialidad es obligatoria."
    )
    @Size(
            max = 500,
            message =
                    "La descripción no puede exceder los 500 caracteres."
    )
    private String description;

    /**
     * Versión recibida desde el listado o detalle.
     */
    @NotNull(
            message =
                    "Debe indicar la versión de la especialidad."
    )
    private Long version;
}