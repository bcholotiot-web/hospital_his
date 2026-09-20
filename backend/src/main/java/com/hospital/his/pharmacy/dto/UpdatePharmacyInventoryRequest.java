package com.hospital.his.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class UpdatePharmacyInventoryRequest {

    /**
     * Nueva existencia disponible.
     */
    @NotNull(
            message =
                    "Debe indicar el stock actual."
    )
    @Min(
            value = 0,
            message =
                    "El stock actual no puede ser negativo."
    )
    private Integer currentStock;

    /**
     * Nuevo nivel mínimo para generar alertas.
     */
    @NotNull(
            message =
                    "Debe indicar el stock mínimo."
    )
    @Min(
            value = 0,
            message =
                    "El stock mínimo no puede ser negativo."
    )
    private Integer minimumStock;

    /**
     * Versión que recibió el frontend.
     *
     * Si otra persona modificó el inventario antes,
     * el número ya no coincidirá y la actualización
     * será rechazada.
     */
    @NotNull(
            message =
                    "Debe indicar la versión del inventario."
    )
    private Long version;
}