package com.hospital.his.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyInventoryResponse {

    private Long inventoryId;

    private Long branchId;

    private String branchName;

    private Long medicationId;

    private String medicationCode;

    private String medicationName;

    private String activeIngredient;

    private String concentration;

    private String presentation;

    /**
     * Unidad utilizada para expresar las existencias.
     *
     * Ejemplos:
     * TABLETA, CÁPSULA, FRASCO, MILILITRO.
     */
    private String medicationUnit;

    private BigDecimal unitPrice;

    private Boolean controlled;

    private Integer currentStock;

    private Integer minimumStock;

    /**
     * Verdadero cuando el stock disponible es cero.
     */
    private Boolean outOfStock;

    /**
     * Verdadero cuando el stock disponible es mayor
     * que cero.
     */
    private Boolean available;

    /**
     * Verdadero cuando el stock disponible es igual
     * o inferior al stock mínimo configurado.
     */
    private Boolean lowStock;

    private Boolean active;

    private String updatedAt;

    private String updatedBy;

    /**
     * Valor utilizado por Hibernate para detectar
     * actualizaciones concurrentes.
     */
    private Long version;

    /**
     * Mensaje informativo para el frontend.
     */
    private String message;
}