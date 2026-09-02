package com.hospital.his.medicalconsultation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveMedicalConsultationRequest {

    private String visitReason;

    private String clinicalFindings;

    private String icd10Code;

    private String icd10Description;

    private String diagnosis;

    private String treatmentPlan;

    private String additionalNotes;

    /*
     * EN_CURSO o FINALIZADA.
     */
    private String status;
}