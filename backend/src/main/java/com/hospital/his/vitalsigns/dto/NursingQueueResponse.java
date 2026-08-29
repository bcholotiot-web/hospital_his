package com.hospital.his.vitalsigns.dto;

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
public class NursingQueueResponse {

    /*
     * APPOINTMENT o EMERGENCY_RECEPTION.
     */
    private String sourceType;

    /*
     * Identificador del elemento de origen.
     * Puede ser appointmentId o emergencyReceptionId.
     */
    private Long sourceId;

    private Long appointmentId;

    private Long emergencyReceptionId;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String specialty;

    private String branch;

    private String priority;

    private String status;

    private String appointmentDateTime;

    private String arrivalTime;

    private Boolean emergency;

    private Boolean registeredPatient;

    private Boolean canCallPatient;

    private Boolean canRegisterVitalSigns;
}