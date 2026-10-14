package com.hospital.his.medicalagenda.service;

import com.hospital.his.appointments.dto.AppointmentStatusPresentationResponse;
import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.appointments.service.AppointmentStatusPresentationService;
import com.hospital.his.medicalagenda.dto.MedicalAgendaAppointmentResponse;
import com.hospital.his.medicalagenda.dto.MedicalAgendaCalendarResponse;
import com.hospital.his.medicalagenda.dto.MedicalAgendaEventResponse;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * CU-16:
 * Construye la respuesta unificada del calendario
 * del médico autenticado.
 *
 * El calendario contiene:
 *
 * - Citas programadas del médico.
 * - Eventos personales activos.
 *
 * Este servicio no modifica citas ni eventos.
 * Solamente consulta y transforma información
 * para la interfaz del calendario.
 */
@Service
public class MedicalAgendaCalendarService {

    /**
     * Duración visual de una cita en minutos.
     *
     * La entidad Appointment actualmente almacena
     * únicamente la fecha y hora inicial.
     *
     * Por esa razón, para representar la cita en
     * el calendario se calcula una hora final:
     *
     * appointmentDateTime + 30 minutos.
     *
     * Este valor debe mantenerse sincronizado con
     * AppointmentService mientras no exista una
     * configuración centralizada.
     */
    private static final long
            APPOINTMENT_DURATION_MINUTES = 30L;

    /**
     * Rango máximo permitido en una consulta.
     *
     * Permite consultar:
     *
     * - Vista diaria.
     * - Vista semanal.
     * - Vista mensual.
     * - Períodos amplios de hasta un año.
     *
     * Evita consultas sin límite sobre todas
     * las citas históricas.
     */
    private static final long
            MAX_RANGE_DAYS = 370L;

    /**
     * Color de respaldo utilizado cuando el estado
     * de una cita no tiene configuración visual
     * activa para el personal.
     */
    private static final String
            DEFAULT_APPOINTMENT_COLOR = "#64748B";

    /**
     * Estados técnicos que no deben aparecer
     * en la agenda operativa del médico.
     *
     * PENDIENTE_DE_PAGO:
     * la cita todavía no está confirmada.
     *
     * NO_ASISTIO:
     * el paciente no se presentó.
     *
     * CANCELADA:
     * la cita fue cancelada.
     *
     * EXPIRADA:
     * la reserva venció sin completar el flujo.
     */
    private static final Set<AppointmentStatus>
            EXCLUDED_APPOINTMENT_STATUSES =
            EnumSet.of(
                    AppointmentStatus
                            .PENDIENTE_DE_PAGO,

                    AppointmentStatus
                            .NO_ASISTIO,

                    AppointmentStatus
                            .CANCELADA,

                    AppointmentStatus
                            .EXPIRADA
            );

    private final AppointmentRepository
            appointmentRepository;

    private final UserRepository
            userRepository;

    private final MedicalAgendaService
            medicalAgendaService;

    private final AppointmentStatusPresentationService
            appointmentStatusPresentationService;

    /**
     * Constructor utilizado por Spring para
     * inyectar las dependencias del calendario.
     */
    public MedicalAgendaCalendarService(
            AppointmentRepository
                    appointmentRepository,

            UserRepository
                    userRepository,

            MedicalAgendaService
                    medicalAgendaService,

            AppointmentStatusPresentationService
                    appointmentStatusPresentationService
    ) {
        this.appointmentRepository =
                appointmentRepository;

        this.userRepository =
                userRepository;

        this.medicalAgendaService =
                medicalAgendaService;

        this.appointmentStatusPresentationService =
                appointmentStatusPresentationService;
    }

    /**
     * Obtiene el calendario unificado del médico
     * autenticado dentro de un rango.
     *
     * Incluye:
     *
     * - Citas activas y visibles.
     * - Eventos personales activos.
     *
     * No incluye:
     *
     * - Citas pendientes de pago.
     * - Citas canceladas.
     * - Citas expiradas.
     * - Citas marcadas como no asistió.
     * - Eventos personales inactivos.
     */
    @Transactional(readOnly = true)
    public MedicalAgendaCalendarResponse
    getMyCalendar(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            String username
    ) {
        /**
         * Validar primero al médico autenticado.
         */
        User doctor =
                validateDoctor(
                        username
                );

        /**
         * Evitar fechas nulas, rangos invertidos
         * o consultas excesivamente amplias.
         */
        validateRange(
                rangeStart,
                rangeEnd
        );

        /**
         * Recuperar únicamente las citas activas
         * del médico dentro del período.
         */
        List<Appointment> appointments =
                appointmentRepository
                        .findDoctorCalendarAppointments(
                                doctor.getId(),
                                rangeStart,
                                rangeEnd,
                                EXCLUDED_APPOINTMENT_STATUSES
                        );

        /**
         * Transformar las entidades de cita en
         * elementos preparados para el calendario.
         */
        List<MedicalAgendaAppointmentResponse>
                appointmentResponses =
                appointments
                        .stream()
                        .map(
                                this::toAppointmentResponse
                        )
                        .toList();

        /**
         * Recuperar los eventos personales activos
         * del mismo médico y rango.
         *
         * Este método debe existir dentro de:
         *
         * MedicalAgendaService.java
         */
        List<MedicalAgendaEventResponse>
                eventResponses =
                medicalAgendaService
                        .getActiveEventsForDoctor(
                                doctor.getId(),
                                rangeStart,
                                rangeEnd
                        );

        return MedicalAgendaCalendarResponse
                .builder()
                .rangeStart(
                        rangeStart.toString()
                )
                .rangeEnd(
                        rangeEnd.toString()
                )
                .doctorId(
                        doctor.getId()
                )
                .doctorName(
                        doctor.getFullName()
                )
                .appointmentCount(
                        appointmentResponses.size()
                )
                .eventCount(
                        eventResponses.size()
                )
                .appointments(
                        appointmentResponses
                )
                .events(
                        eventResponses
                )
                .build();
    }

    /**
     * Convierte una cita existente en un elemento
     * visual del calendario médico.
     */
    private MedicalAgendaAppointmentResponse
    toAppointmentResponse(
            Appointment appointment
    ) {
        if (appointment == null) {
            throw new RuntimeException(
                    "No fue posible convertir una cita vacía."
            );
        }

        /**
         * Resolver nombre visible, descripción y color
         * de acuerdo con la configuración para personal.
         */
        AppointmentStatusPresentationResponse
                statusPresentation =
                appointmentStatusPresentationService
                        .resolveForStaff(
                                appointment.getStatus()
                        );

        LocalDateTime startDateTime =
                appointment
                        .getAppointmentDateTime();

        /**
         * Appointment todavía no guarda una fecha
         * final. Se calcula un bloque de 30 minutos.
         */
        LocalDateTime endDateTime =
                startDateTime != null
                        ? startDateTime
                        .plusMinutes(
                                APPOINTMENT_DURATION_MINUTES
                        )
                        : null;

        boolean followUp =
                Boolean.TRUE.equals(
                        appointment.getFollowUp()
                );

        return MedicalAgendaAppointmentResponse
                .builder()
                .appointmentId(
                        appointment.getId()
                )
                .title(
                        buildAppointmentTitle(
                                appointment
                        )
                )
                .patientId(
                        appointment.getPatient() != null
                                ? appointment
                                .getPatient()
                                .getId()
                                : null
                )
                .patientName(
                        appointment.getPatient() != null
                                ? appointment
                                .getPatient()
                                .getFullName()
                                : "Paciente no disponible"
                )
                .branchId(
                        appointment.getBranch() != null
                                ? appointment
                                .getBranch()
                                .getId()
                                : null
                )
                .branchName(
                        appointment.getBranch() != null
                                ? appointment
                                .getBranch()
                                .getName()
                                : null
                )
                .specialtyId(
                        appointment.getSpecialty() != null
                                ? appointment
                                .getSpecialty()
                                .getId()
                                : null
                )
                .specialtyName(
                        appointment.getSpecialty() != null
                                ? appointment
                                .getSpecialty()
                                .getName()
                                : null
                )
                .reason(
                        appointment.getReason()
                )
                .priority(
                        appointment.getPriority() != null
                                ? appointment
                                .getPriority()
                                .name()
                                : null
                )
                /**
                 * Código técnico.
                 *
                 * El frontend continuará utilizando
                 * este campo para reglas funcionales.
                 */
                .status(
                        appointment.getStatus() != null
                                ? appointment
                                .getStatus()
                                .name()
                                : null
                )
                /**
                 * Nombre visible, descripción y color
                 * configurables.
                 */
                .statusPresentation(
                        statusPresentation
                )
                .startDateTime(
                        startDateTime != null
                                ? startDateTime
                                .toString()
                                : null
                )
                .endDateTime(
                        endDateTime != null
                                ? endDateTime
                                .toString()
                                : null
                )
                .allDay(false)
                .followUp(
                        followUp
                )
                .followUpType(
                        appointment
                                .getFollowUpType() != null
                                ? appointment
                                .getFollowUpType()
                                .name()
                                : null
                )
                .parentConsultationId(
                        appointment
                                .getParentConsultation() != null
                                ? appointment
                                .getParentConsultation()
                                .getId()
                                : null
                )
                .color(
                        resolveAppointmentColor(
                                statusPresentation
                        )
                )
                .calendarItemType(
                        "APPOINTMENT"
                )
                .build();
    }

    /**
     * Construye el título utilizado dentro
     * del bloque del calendario.
     *
     * Citas regulares:
     *
     * Consulta - Nombre del paciente
     *
     * Seguimientos:
     *
     * Seguimiento - Nombre del paciente
     */
    private String buildAppointmentTitle(
            Appointment appointment
    ) {
        String patientName =
                appointment.getPatient() != null &&
                        appointment
                                .getPatient()
                                .getFullName() != null
                        ? appointment
                        .getPatient()
                        .getFullName()
                        : "Paciente";

        boolean followUp =
                Boolean.TRUE.equals(
                        appointment.getFollowUp()
                );

        return (
                followUp
                        ? "Seguimiento - "
                        : "Consulta - "
        ) + patientName;
    }

    /**
     * Obtiene el color configurado para el estado.
     *
     * Si la configuración es nula o inválida,
     * utiliza un color gris de respaldo.
     */
    private String resolveAppointmentColor(
            AppointmentStatusPresentationResponse
                    presentation
    ) {
        if (
                presentation == null ||
                        presentation.getColor() == null ||
                        !presentation
                                .getColor()
                                .matches(
                                        "^#[0-9A-Fa-f]{6}$"
                                )
        ) {
            return DEFAULT_APPOINTMENT_COLOR;
        }

        return presentation
                .getColor()
                .toUpperCase();
    }

    /**
     * Valida que el usuario autenticado:
     *
     * - Exista.
     * - Esté activo.
     * - Tenga un rol activo.
     * - Tenga rol MEDICO.
     */
    private User validateDoctor(
            String username
    ) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al médico autenticado."
            );
        }

        User doctor =
                userRepository
                        .findByUsername(
                                username
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Usuario autenticado no encontrado."
                                        )
                        );

        if (
                !Boolean.TRUE.equals(
                        doctor.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del médico se encuentra inactiva."
            );
        }

        if (
                doctor.getRole() == null ||
                        !Boolean.TRUE.equals(
                                doctor
                                        .getRole()
                                        .getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El médico no tiene un rol activo."
            );
        }

        String roleName =
                doctor
                        .getRole()
                        .getName();

        if (
                roleName == null ||
                        !"MEDICO".equalsIgnoreCase(
                                roleName.trim()
                        )
        ) {
            throw new RuntimeException(
                    "Solamente un médico puede consultar esta agenda."
            );
        }

        return doctor;
    }

    /**
     * Valida el período visible del calendario.
     */
    private void validateRange(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        if (
                rangeStart == null ||
                        rangeEnd == null
        ) {
            throw new RuntimeException(
                    "Debe indicar el rango del calendario."
            );
        }

        if (
                !rangeEnd.isAfter(
                        rangeStart
                )
        ) {
            throw new RuntimeException(
                    "La fecha de fin debe ser posterior "
                            + "a la fecha de inicio."
            );
        }

        /**
         * Utilizamos minutos para evitar que un rango
         * de 370 días y varias horas adicionales pase
         * inadvertidamente por un cálculo truncado.
         */
        long rangeMinutes =
                ChronoUnit.MINUTES.between(
                        rangeStart,
                        rangeEnd
                );

        long maximumMinutes =
                MAX_RANGE_DAYS
                        * 24L
                        * 60L;

        if (
                rangeMinutes >
                        maximumMinutes
        ) {
            throw new RuntimeException(
                    "El rango del calendario no puede exceder los "
                            + MAX_RANGE_DAYS
                            + " días."
            );
        }
    }
}