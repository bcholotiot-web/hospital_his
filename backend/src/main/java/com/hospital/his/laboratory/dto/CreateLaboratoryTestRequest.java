package com.hospital.his.laboratory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;

import java.math.BigDecimal;

/**
 * Request administrativo para crear un examen.
 *
 * El estado lo controla el backend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLaboratoryTestRequest {

    @NotBlank(
            message =
                    "El código del examen es obligatorio."
    )
    @Size(
            max = 30,
            message =
                    "El código no puede exceder los 30 caracteres."
    )
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message =
                    "El código solamente puede contener letras, números, guion y guion bajo."
    )
    private String code;

    @NotBlank(
            message =
                    "El nombre del examen es obligatorio."
    )
    @Size(
            max = 200,
            message =
                    "El nombre no puede exceder los 200 caracteres."
    )
    private String name;

    @Size(
            max = 500,
            message =
                    "La descripción no puede exceder los 500 caracteres."
    )
    private String description;

    @Size(
            max = 200,
            message =
                    "El rango de referencia no puede exceder los 200 caracteres."
    )
    private String referenceRange;

    @Size(
            max = 50,
            message =
                    "La unidad no puede exceder los 50 caracteres."
    )
    private String defaultUnit;

    @NotNull(
            message =
                    "El precio del examen es obligatorio."
    )
    @DecimalMin(
            value = "0.01",
            message =
                    "El precio debe ser mayor que cero."
    )
    @Digits(
            integer = 8,
            fraction = 2,
            message =
                    "El precio admite hasta 8 enteros y 2 decimales."
    )
    private BigDecimal price;
}