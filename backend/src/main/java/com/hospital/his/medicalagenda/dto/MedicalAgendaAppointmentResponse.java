package com.hospital.his.medicalagenda.dto;

import com.hospital.his.appointments.dto
        .AppointmentStatusPresentationResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representación de una cita médica dentro
 * del calendario de la agenda.
 *
 * Este DTO no sustituye AppointmentResponse.
 *
 * Está diseñado específicamente para que
 * el frontend pueda representar una cita
 * como un elemento del calendario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalAgendaAppointmentResponse {

    /**
     * Identificador de la cita.
     */
    private Long appointmentId;

    /**
     * Texto principal que se mostrará
     * dentro del calendario.
     *
     * Ejemplos:
     *
     * Consulta - Nombre del paciente
     * Seguimiento - Nombre del paciente
     */
    private String title;

    /**
     * Identificador del paciente.
     */
    private Long patientId;

    /**
     * Nombre completo del paciente.
     */
    private String patientName;

    /**
     * Identificador de la sucursal.
     */
    private Long branchId;

    /**
     * Nombre visible de la sucursal.
     */
    private String branchName;

    /**
     * Identificador de la especialidad.
     */
    private Long specialtyId;

    /**
     * Nombre visible de la especialidad.
     */
    private String specialtyName;

    /**
     * Motivo de la cita.
     */
    private String reason;

    /**
     * Prioridad técnica de la cita.
     *
     * Valores esperados:
     *
     * NORMAL
     * EMERGENCIA
     */
    private String priority;

    /**
     * Código técnico del estado de la cita.
     *
     * Ejemplos:
     *
     * CONFIRMADA
     * PACIENTE_PRESENTE
     * SIGNOS_REGISTRADOS
     * CONSULTA_MEDICA
     */
    private String status;

    /**
     * Presentación visual configurada para
     * el personal interno.
     *
     * Contiene:
     *
     * - Nombre visible.
     * - Descripción.
     * - Color.
     * - Orden visual.
     * - Indicador de configuración.
     */
    private AppointmentStatusPresentationResponse
            statusPresentation;

    /**
     * Fecha y hora inicial de la cita.
     *
     * Se devuelve en formato ISO:
     *
     * 2026-10-20T09:00:00
     */
    private String startDateTime;

    /**
     * Fecha y hora final calculada para
     * la representación en el calendario.
     *
     * Actualmente:
     *
     * startDateTime + 30 minutos
     */
    private String endDateTime;

    /**
     * Las citas siempre utilizan false.
     *
     * Se incluye para mantener un contrato
     * compatible con bibliotecas de calendario.
     */
    private Boolean allDay;

    /**
     * Indica si la cita fue creada como
     * seguimiento de una consulta anterior.
     */
    private Boolean followUp;

    /**
     * Tipo técnico de seguimiento.
     *
     * Puede ser null para citas regulares.
     */
    private String followUpType;

    /**
     * Consulta médica que originó
     * el seguimiento.
     *
     * Será null para citas regulares.
     */
    private Long parentConsultationId;

    /**
     * Color hexadecimal utilizado por
     * el calendario.
     *
     * Se obtiene desde la presentación
     * configurada del estado.
     */
    private String color;

    /**
     * Tipo de elemento dentro del calendario.
     *
     * Para este DTO siempre será:
     *
     * APPOINTMENT
     */
    private String calendarItemType;
}