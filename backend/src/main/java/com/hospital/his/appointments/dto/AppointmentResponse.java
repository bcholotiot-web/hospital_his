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
public class AppointmentResponse {

    private Long id;

    private String patientName;

    private String doctorName;

    private String branch;

    private String specialty;

    private String appointmentDateTime;

    private String reason;

    private String status;

    /**
     * Presentación visual del estado técnico.
     *
     * El campo status conserva el código utilizado
     * por la máquina de estados.
     *
     * Este objeto aporta:
     *
     * - Nombre visible.
     * - Descripción.
     * - Color.
     * - Orden.
     * - Indicador de configuración.
     */
    private AppointmentStatusPresentationResponse statusPresentation;

    private String reservationExpiresAt;

    /**
     * Prioridad asignada a la cita.
     *
     * Valores actuales:
     *
     * NORMAL
     * EMERGENCIA
     */
    private String priority;

    /**
     * Origen de creación de la cita.
     *
     * Para seguimientos médicos utilizaremos INTERNA.
     */
    private String origin;

    /**
     * Indica si la cita fue generada como seguimiento
     * de una consulta médica anterior.
     */
    private Boolean followUp;

    /**
     * Tipo de seguimiento:
     *
     * MONITOREO_TRATAMIENTO
     * REVISION_RESULTADOS_LABORATORIO
     */
    private String followUpType;

    /**
     * Consulta médica que originó el seguimiento.
     *
     * Será null para citas regulares.
     */
    private Long parentConsultationId;

    /**
     * Cita originalmente atendida.
     *
     * Se obtiene desde la consulta médica padre.
     */
    private Long parentAppointmentId;

    /**
     * Mensaje funcional enviado al frontend.
     */
    private String message;
}