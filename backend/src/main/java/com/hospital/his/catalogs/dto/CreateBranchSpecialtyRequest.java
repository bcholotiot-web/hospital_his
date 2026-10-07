package com.hospital.his.catalogs.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Datos recibidos para asignar una especialidad
 * a una sede.
 *
 * El frontend no envía:
 *
 * - Estado.
 * - Usuario.
 * - Fechas.
 * - Versión.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchSpecialtyRequest {

    /**
     * Identificador de la sede activa.
     */
    @NotNull(
            message =
                    "Debe seleccionar una sede."
    )
    private Long branchId;

    /**
     * Identificador de la especialidad activa.
     */
    @NotNull(
            message =
                    "Debe seleccionar una especialidad."
    )
    private Long specialtyId;
}