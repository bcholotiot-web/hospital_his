package com.hospital.his.medications.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;

import java.math.BigDecimal;

/**
 * Request administrativo para crear
 * un medicamento.
 *
 * El estado inicial es controlado por Spring Boot.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMedicationRequest {

    @NotBlank(
            message =
                    "El código del medicamento es obligatorio."
    )
    @Size(
            max = 40,
            message =
                    "El código no puede exceder los 40 caracteres."
    )
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message =
                    "El código solamente puede contener letras, números, guion y guion bajo."
    )
    private String code;

    @NotBlank(
            message =
                    "El nombre del medicamento es obligatorio."
    )
    @Size(
            max = 200,
            message =
                    "El nombre no puede exceder los 200 caracteres."
    )
    private String name;

    @NotBlank(
            message =
                    "El principio activo es obligatorio."
    )
    @Size(
            max = 200,
            message =
                    "El principio activo no puede exceder los 200 caracteres."
    )
    private String activeIngredient;

    @NotBlank(
            message =
                    "La concentración es obligatoria."
    )
    @Size(
            max = 100,
            message =
                    "La concentración no puede exceder los 100 caracteres."
    )
    private String concentration;

    @NotBlank(
            message =
                    "La presentación es obligatoria."
    )
    @Size(
            max = 100,
            message =
                    "La presentación no puede exceder los 100 caracteres."
    )
    private String presentation;

    @NotBlank(
            message =
                    "La unidad del medicamento es obligatoria."
    )
    @Size(
            max = 50,
            message =
                    "La unidad no puede exceder los 50 caracteres."
    )
    private String unit;

    @NotBlank(
            message =
                    "La descripción del medicamento es obligatoria."
    )
    @Size(
            max = 1000,
            message =
                    "La descripción no puede exceder los 1000 caracteres."
    )
    private String description;

    @NotNull(
            message =
                    "El precio unitario es obligatorio."
    )
    @DecimalMin(
            value = "0.01",
            message =
                    "El precio unitario debe ser mayor que cero."
    )
    @Digits(
            integer = 8,
            fraction = 2,
            message =
                    "El precio admite hasta 8 enteros y 2 decimales."
    )
    private BigDecimal unitPrice;

    @NotNull(
            message =
                    "Debe indicar si el medicamento es controlado."
    )
    private Boolean controlled;
}