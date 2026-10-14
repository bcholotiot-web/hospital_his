package com.hospital.his.pharmacy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Solicitud para registrar manualmente
 * un movimiento de inventario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInventoryMovementRequest {

    /**
     * Obligatorio para ADMIN.
     *
     * Para FARMACIA se utiliza automáticamente
     * la sucursal asignada al usuario.
     */
    private Long branchId;

    /**
     * Medicamento afectado.
     */
    @NotNull(
            message =
                    "Debe seleccionar un medicamento."
    )
    private Long medicationId;

    /**
     * Tipo manual:
     *
     * COMPRA
     * DEVOLUCION
     * VENTA
     * RECLAMO
     * AJUSTE_POSITIVO
     * AJUSTE_NEGATIVO
     */
    @NotNull(
            message =
                    "Debe seleccionar el tipo de movimiento."
    )
    private String movementType;

    /**
     * Cantidad absoluta.
     */
    @NotNull(
            message =
                    "La cantidad es obligatoria."
    )
    @Min(
            value = 1,
            message =
                    "La cantidad debe ser un número entero positivo."
    )
    private Integer quantity;

    /**
     * Obligatorio únicamente para COMPRA.
     */
    @DecimalMin(
            value = "0.01",
            inclusive = true,
            message =
                    "El costo unitario debe ser mayor a 0."
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message =
                    "El costo unitario debe tener máximo 2 decimales."
    )
    private BigDecimal unitCost;

    /**
     * Referencia funcional, como factura,
     * venta, devolución o reclamo.
     */
    @Size(
            max = 100,
            message =
                    "El número de referencia no puede exceder los 100 caracteres."
    )
    private String referenceNumber;

    /**
     * Motivo del movimiento.
     *
     * El servicio determinará cuándo es obligatorio.
     */
    @Size(
            max = 500,
            message =
                    "El motivo no puede exceder los 500 caracteres."
    )
    private String reason;

    /**
     * Versión del inventario mostrada durante
     * la vista previa.
     */
    @NotNull(
            message =
                    "La versión del inventario es obligatoria."
    )
    private Long inventoryVersion;
}