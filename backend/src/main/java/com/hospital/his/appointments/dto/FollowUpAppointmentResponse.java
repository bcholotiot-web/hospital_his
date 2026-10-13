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
public class FollowUpAppointmentResponse {

    private Long appointmentId;

    private Long parentConsultationId;

    private Long parentAppointmentId;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String specialty;

    private String branch;

    private String appointmentDateTime;

    private String reason;

    private String priority;

    private String status;

    /**
     * Presentación visual del estado de la cita
     * de seguimiento.
     *
     * status conserva el código técnico.
     */
    private AppointmentStatusPresentationResponse statusPresentation;


    private String origin;

    private Boolean followUp;

    private String followUpType;

    private String reservationExpiresAt;

    /**
     * Indica si la cita todavía puede pagarse
     * mediante el flujo normal.
     */
    private Boolean pendingPayment;

    /**
     * Indica si la cita continúa activa.
     */
    private Boolean active;

    private String message;
}