package com.hospital.his.medicalagenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Respuesta unificada del calendario
 * del médico autenticado.
 *
 * Contiene:
 *
 * - Citas programadas.
 * - Eventos personales.
 *
 * El frontend transformará ambas listas
 * en elementos visuales del calendario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalAgendaCalendarResponse {

    /**
     * Inicio del rango consultado.
     *
     * Ejemplo:
     *
     * 2026-10-01T00:00:00
     */
    private String rangeStart;

    /**
     * Fin exclusivo del rango consultado.
     *
     * Ejemplo:
     *
     * 2026-11-01T00:00:00
     */
    private String rangeEnd;

    /**
     * Identificador del médico autenticado.
     */
    private Long doctorId;

    /**
     * Nombre completo del médico.
     */
    private String doctorName;

    /**
     * Cantidad de citas devueltas dentro
     * del rango consultado.
     */
    private Integer appointmentCount;

    /**
     * Cantidad de eventos personales activos
     * dentro del rango consultado.
     */
    private Integer eventCount;

    /**
     * Citas programadas del médico.
     *
     * No incluye:
     *
     * - PENDIENTE_DE_PAGO
     * - NO_ASISTIO
     * - CANCELADA
     * - EXPIRADA
     */
    private List<MedicalAgendaAppointmentResponse>
            appointments;

    /**
     * Eventos personales activos del médico.
     *
     * Estos eventos utilizan el color:
     *
     * #8B5CF6
     */
    private List<MedicalAgendaEventResponse>
            events;
}