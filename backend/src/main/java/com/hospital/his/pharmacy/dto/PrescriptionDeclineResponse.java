package com.hospital.his.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDeclineResponse {

    private Long eventId;

    private Long prescriptionId;

    private String prescriptionNumber;

    private String patientName;

    private String branch;

    private String reason;

    private String pharmacyUsername;

    private String createdAt;

    /**
     * Estado que conserva la receta.
     *
     * Este evento no cancela automáticamente
     * la receta médica.
     */
    private String prescriptionStatus;

    private String message;
}