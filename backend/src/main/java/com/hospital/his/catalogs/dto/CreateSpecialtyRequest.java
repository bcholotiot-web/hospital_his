package com.hospital.his.catalogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request para crear una especialidad.
 *
 * El estado no se recibe desde el frontend.
 * Toda nueva especialidad se crea activa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSpecialtyRequest {

    @NotBlank(message = "El nombre de la especialidad es obligatorio.")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres.")
    private String name;

    @NotBlank(message = "La descripción de la especialidad es obligatoria.")
    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
    private String description;
}