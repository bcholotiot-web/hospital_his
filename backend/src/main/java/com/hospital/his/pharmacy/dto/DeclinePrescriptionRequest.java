package com.hospital.his.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

//Para no adquirir medicamentos
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclinePrescriptionRequest {

    /**
     * Razón por la que no se realizará el despacho.
     */
    @NotBlank(
            message =
                    "Debe indicar el motivo por el cual "
                            + "el paciente no adquirirá los medicamentos."
    )
    @Size(
            max = 500,
            message =
                    "El motivo no puede exceder los 500 caracteres."
    )
    private String reason;
}