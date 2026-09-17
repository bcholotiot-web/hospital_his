package com.hospital.his.laboratory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLaboratoryOrderRequest {

    /**
     * Cita de la cual se obtuvo la consulta médica.
     *
     * El backend recuperará desde aquí:
     * paciente, médico, sucursal y consulta.
     */
    @NotNull(message = "Debe indicar la cita médica.")
    private Long appointmentId;

    /**
     * Identificadores de los exámenes seleccionados.
     *
     * Debe existir al menos uno y ninguno puede ser null.
     */
    @NotEmpty(message = "Debe seleccionar al menos un examen de laboratorio.") @Size(max = 50, message = "No puede seleccionar más de 50 exámenes por orden.")
    private List<@NotNull(message = "El identificador del examen no puede estar vacío.") Long> laboratoryTestIds;

    /**
     * true:
     * el paciente realizará los exámenes fuera del hospital.
     *
     * false o null:
     * los exámenes serán procesados por el laboratorio interno.
     */
    private Boolean externalOrder;

    /**
     * Indicaciones opcionales para el laboratorio.
     */
    @Size(max = 1000, message = "Las observaciones no pueden exceder los 1000 caracteres.")
    private String notes;
}