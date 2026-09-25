package com.hospital.his.appointments.dto;

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
public class FollowUpAppointmentContextResponse {

    private Long parentConsultationId;

    private Long parentAppointmentId;

    private Long patientUserId;

    private String patientName;

    private String patientDpi;

    private Long doctorUserId;

    private String doctorName;

    private Long specialtyId;

    private String specialtyName;

    private Long branchId;

    private String branchName;

    private String consultationStatus;

    private String appointmentStatus;

    private String diagnosis;

    private String treatmentPlan;

    /**
     * Verdadero cuando el backend permite al médico
     * programar una cita de seguimiento.
     */
    private Boolean canSchedule;

    private String message;
}