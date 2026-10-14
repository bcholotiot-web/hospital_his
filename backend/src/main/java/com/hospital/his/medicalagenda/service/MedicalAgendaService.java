package com.hospital.his.medicalagenda.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.medicalagenda.dto.CreateMedicalAgendaEventRequest;
import com.hospital.his.medicalagenda.dto.MedicalAgendaEventResponse;
import com.hospital.his.medicalagenda.dto.UpdateMedicalAgendaEventRequest;
import com.hospital.his.medicalagenda.entity.MedicalAgendaEvent;
import com.hospital.his.medicalagenda.entity.MedicalAgendaEventType;
import com.hospital.his.medicalagenda.repository.MedicalAgendaEventRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import com.hospital.his.medicalagenda.dto.MedicalAgendaBusyPeriodResponse;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Servicio de eventos personales
 * de la agenda médica.
 */
@Service
public class MedicalAgendaService {

    /**
     * Color obligatorio de los eventos personales.
     */
    private static final String EVENT_COLOR =
            "#8B5CF6";

    /**
     * Máximo período permitido en una sola
     * consulta de calendario.
     *
     * Permite consultar vistas mensuales,
     * semanales y diarias sin solicitar rangos
     * excesivamente grandes.
     */
    private static final long MAX_RANGE_DAYS =
            370L;

    private final MedicalAgendaEventRepository
            eventRepository;

    private final UserRepository
            userRepository;

    private final AuditService
            auditService;

    public MedicalAgendaService(
            MedicalAgendaEventRepository
                    eventRepository,

            UserRepository userRepository,

            AuditService auditService
    ) {
        this.eventRepository =
                eventRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /**
     * Lista los eventos activos del médico
     * dentro del rango solicitado.
     */
    @Transactional(readOnly = true)
    public List<MedicalAgendaEventResponse>
    getMyEvents(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            String username
    ) {
        User doctor =
                validateDoctor(
                        username
                );

        validateCalendarRange(
                rangeStart,
                rangeEnd
        );

        return eventRepository
                .findActiveEventsWithinRange(
                        doctor.getId(),
                        rangeStart,
                        rangeEnd
                )
                .stream()
                .map(
                        event ->
                                toResponse(
                                        event,
                                        null
                                )
                )
                .toList();
    }

    /**
     * Obtiene los eventos personales activos de
     * un médico dentro de un rango determinado.
     *
     * Este método se utiliza internamente para
     * construir el calendario unificado:
     *
     * - Citas programadas.
     * - Eventos personales.
     *
     * A diferencia de getMyEvents(), este método
     * recibe directamente el ID del médico porque
     * el usuario ya fue validado previamente por
     * MedicalAgendaCalendarService.
     */
    @Transactional(readOnly = true)
    public List<MedicalAgendaEventResponse>
    getActiveEventsForDoctor(
            Long doctorId,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        if (doctorId == null) {
            throw new RuntimeException(
                    "Debe indicar el médico."
            );
        }

        /**
         * Reutiliza la misma validación aplicada
         * en la consulta directa de eventos.
         */
        validateCalendarRange(
                rangeStart,
                rangeEnd
        );

        return eventRepository
                .findActiveEventsWithinRange(
                        doctorId,
                        rangeStart,
                        rangeEnd
                )
                .stream()
                .map(
                        event ->
                                toResponse(
                                        event,
                                        null
                                )
                )
                .toList();
    }

    /**
     * Obtiene los períodos bloqueados por eventos
     * personales activos del médico.
     *
     * Se utiliza durante la generación de horarios
     * disponibles para evitar una consulta SQL por
     * cada bloque de cita.
     */
    @Transactional(readOnly = true)
    public List<MedicalAgendaBusyPeriodResponse>
    getActiveBusyPeriodsForDoctor(
            Long doctorId,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        if (doctorId == null) {
            throw new RuntimeException(
                    "Debe indicar el médico."
            );
        }

        validateCalendarRange(
                rangeStart,
                rangeEnd
        );

        return eventRepository
                .findActiveEventsWithinRange(
                        doctorId,
                        rangeStart,
                        rangeEnd
                )
                .stream()
                .map(
                        event ->
                                MedicalAgendaBusyPeriodResponse
                                        .builder()
                                        .eventId(
                                                event.getId()
                                        )
                                        .startDateTime(
                                                event.getStartDateTime()
                                        )
                                        .endDateTime(
                                                event.getEndDateTime()
                                        )
                                        .allDay(
                                                event.getAllDay()
                                        )
                                        .eventType(
                                                event.getEventType() != null
                                                        ? event
                                                        .getEventType()
                                                        .name()
                                                        : null
                                        )
                                        .build()
                )
                .toList();
    }


    /**
     * Indica si un médico tiene un evento personal
     * activo que se superpone con el período.
     *
     * Se utiliza desde:
     *
     * - Consulta de horarios disponibles.
     * - Creación de citas regulares.
     * - Creación de citas de seguimiento.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveEventOverlap(
            Long doctorId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        if (doctorId == null) {
            throw new RuntimeException(
                    "Debe indicar el médico."
            );
        }

        if (
                startDateTime == null ||
                        endDateTime == null
        ) {
            throw new RuntimeException(
                    "Debe indicar el período que desea validar."
            );
        }

        if (
                !endDateTime.isAfter(
                        startDateTime
                )
        ) {
            throw new RuntimeException(
                    "La fecha de fin debe ser posterior "
                            + "a la fecha de inicio."
            );
        }

        return eventRepository
                .existsActiveOverlap(
                        doctorId,
                        startDateTime,
                        endDateTime
                );
    }


    /**
     * Obtiene un evento propio por ID.
     */
    @Transactional(readOnly = true)
    public MedicalAgendaEventResponse
    getMyEventById(
            Long eventId,
            String username
    ) {
        if (eventId == null) {
            throw new RuntimeException(
                    "Debe indicar el evento."
            );
        }

        User doctor =
                validateDoctor(
                        username
                );

        MedicalAgendaEvent event =
                eventRepository
                        .findByIdAndDoctor_Id(
                                eventId,
                                doctor.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Evento de agenda no encontrado."
                                )
                        );

        return toResponse(
                event,
                null
        );
    }

    /**
     * Crea un evento personal.
     */
    @Transactional
    public MedicalAgendaEventResponse
    createEvent(
            CreateMedicalAgendaEventRequest request,
            String username
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del evento son obligatorios."
            );
        }

        User doctor =
                validateDoctor(
                        username
                );

        String cleanTitle =
                normalizeRequiredText(
                        request.getTitle(),
                        150,
                        "El título del evento"
                );

        String cleanDescription =
                normalizeOptionalText(
                        request.getDescription(),
                        500,
                        "La descripción"
                );

        MedicalAgendaEventType eventType =
                parseEventType(
                        request.getEventType()
                );

        Boolean allDay =
                Boolean.TRUE.equals(
                        request.getAllDay()
                );

        NormalizedEventPeriod period =
                normalizeAndValidatePeriod(
                        request.getStartDateTime(),
                        request.getEndDateTime(),
                        allDay
                );

        validateNoEventOverlap(
                doctor.getId(),
                null,
                period.startDateTime(),
                period.endDateTime()
        );

        LocalDateTime now =
                LocalDateTime.now();

        MedicalAgendaEvent event =
                MedicalAgendaEvent
                        .builder()
                        .doctor(
                                doctor
                        )
                        .title(
                                cleanTitle
                        )
                        .description(
                                cleanDescription
                        )
                        .startDateTime(
                                period
                                        .startDateTime()
                        )
                        .endDateTime(
                                period
                                        .endDateTime()
                        )
                        .allDay(
                                allDay
                        )
                        .eventType(
                                eventType
                        )
                        .color(
                                EVENT_COLOR
                        )
                        .active(true)
                        .createdAt(
                                now
                        )
                        .createdBy(
                                username
                        )
                        .updatedAt(
                                now
                        )
                        .updatedBy(
                                username
                        )
                        .build();

        event =
                eventRepository
                        .saveAndFlush(
                                event
                        );

        auditService.log(
                username,
                "CREATE_MEDICAL_AGENDA_EVENT",
                "MEDICAL_AGENDA",
                "Evento de agenda creado. ID: "
                        + event.getId()
                        + ". Título: "
                        + event.getTitle()
                        + ". Inicio: "
                        + event.getStartDateTime()
                        + ". Fin: "
                        + event.getEndDateTime()
                        + "."
        );

        return toResponse(
                event,
                buildSuccessMessage(
                        "Evento creado exitosamente",
                        event
                )
        );
    }

    /**
     * Actualiza un evento propio.
     */
    @Transactional
    public MedicalAgendaEventResponse
    updateEvent(
            Long eventId,
            UpdateMedicalAgendaEventRequest request,
            String username
    ) {
        if (eventId == null) {
            throw new RuntimeException(
                    "Debe indicar el evento."
            );
        }

        if (request == null) {
            throw new RuntimeException(
                    "Los datos del evento son obligatorios."
            );
        }

        User doctor =
                validateDoctor(
                        username
                );

        MedicalAgendaEvent event =
                eventRepository
                        .findByIdAndDoctor_IdAndActiveTrue(
                                eventId,
                                doctor.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Evento activo no encontrado."
                                )
                        );

        validateVersion(
                event,
                request.getVersion()
        );

        String cleanTitle =
                normalizeRequiredText(
                        request.getTitle(),
                        150,
                        "El título del evento"
                );

        String cleanDescription =
                normalizeOptionalText(
                        request.getDescription(),
                        500,
                        "La descripción"
                );

        MedicalAgendaEventType eventType =
                parseEventType(
                        request.getEventType()
                );

        Boolean allDay =
                Boolean.TRUE.equals(
                        request.getAllDay()
                );

        NormalizedEventPeriod period =
                normalizeAndValidatePeriod(
                        request.getStartDateTime(),
                        request.getEndDateTime(),
                        allDay
                );

        validateNoEventOverlap(
                doctor.getId(),
                event.getId(),
                period.startDateTime(),
                period.endDateTime()
        );

        event.setTitle(
                cleanTitle
        );

        event.setDescription(
                cleanDescription
        );

        event.setStartDateTime(
                period.startDateTime()
        );

        event.setEndDateTime(
                period.endDateTime()
        );

        event.setAllDay(
                allDay
        );

        event.setEventType(
                eventType
        );

        /**
         * El color siempre se restablece al
         * valor obligatorio.
         */
        event.setColor(
                EVENT_COLOR
        );

        event.setUpdatedAt(
                LocalDateTime.now()
        );

        event.setUpdatedBy(
                username
        );

        try {
            event =
                    eventRepository
                            .saveAndFlush(
                                    event
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El evento fue modificado por otro usuario. "
                            + "Actualice la información antes de continuar."
            );
        }

        auditService.log(
                username,
                "UPDATE_MEDICAL_AGENDA_EVENT",
                "MEDICAL_AGENDA",
                "Evento de agenda actualizado. ID: "
                        + event.getId()
                        + ". Título: "
                        + event.getTitle()
                        + "."
        );

        return toResponse(
                event,
                "Evento actualizado exitosamente."
        );
    }

    /**
     * Activa o desactiva lógicamente un evento.
     */
    @Transactional
    public MedicalAgendaEventResponse
    changeEventStatus(
            Long eventId,
            Boolean active,
            Long version,
            String username
    ) {
        if (eventId == null) {
            throw new RuntimeException(
                    "Debe indicar el evento."
            );
        }

        if (active == null) {
            throw new RuntimeException(
                    "Debe indicar el nuevo estado del evento."
            );
        }

        User doctor =
                validateDoctor(
                        username
                );

        MedicalAgendaEvent event =
                eventRepository
                        .findByIdAndDoctor_Id(
                                eventId,
                                doctor.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Evento de agenda no encontrado."
                                )
                        );

        validateVersion(
                event,
                version
        );

        if (
                Boolean.TRUE.equals(
                        event.getActive()
                ) ==
                        Boolean.TRUE.equals(
                                active
                        )
        ) {
            throw new RuntimeException(
                    active
                            ? "El evento ya se encuentra activo."
                            : "El evento ya se encuentra inactivo."
            );
        }

        /**
         * Al reactivar validamos nuevamente
         * que no exista otro evento ocupando
         * el mismo período.
         */
        if (Boolean.TRUE.equals(active)) {
            validateNoEventOverlap(
                    doctor.getId(),
                    event.getId(),
                    event.getStartDateTime(),
                    event.getEndDateTime()
            );
        }

        event.setActive(
                active
        );

        event.setUpdatedAt(
                LocalDateTime.now()
        );

        event.setUpdatedBy(
                username
        );

        try {
            event =
                    eventRepository
                            .saveAndFlush(
                                    event
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El evento fue modificado por otro usuario. "
                            + "Actualice la información antes de continuar."
            );
        }

        auditService.log(
                username,
                active
                        ? "REACTIVATE_MEDICAL_AGENDA_EVENT"
                        : "DEACTIVATE_MEDICAL_AGENDA_EVENT",
                "MEDICAL_AGENDA",
                (
                        active
                                ? "Evento reactivado. "
                                : "Evento desactivado. "
                )
                        + "ID: "
                        + event.getId()
                        + ". Título: "
                        + event.getTitle()
                        + "."
        );

        return toResponse(
                event,
                active
                        ? "Evento reactivado exitosamente."
                        : "Evento eliminado exitosamente."
        );
    }

    /**
     * Valida que la cuenta corresponda con
     * un médico activo.
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

        User user =
                userRepository
                        .findByUsername(
                                username
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Usuario autenticado no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        user.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del médico se encuentra inactiva."
            );
        }

        if (
                user.getRole() == null ||
                        !Boolean.TRUE.equals(
                                user.getRole()
                                        .getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El usuario no tiene un rol activo."
            );
        }

        String roleName =
                user.getRole()
                        .getName();

        if (
                roleName == null ||
                        !"MEDICO".equalsIgnoreCase(
                                roleName.trim()
                        )
        ) {
            throw new RuntimeException(
                    "Solamente un médico puede administrar su agenda."
            );
        }

        return user;
    }

    /**
     * Valida el rango solicitado durante la
     * consulta del calendario.
     */
    private void validateCalendarRange(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        if (
                rangeStart == null ||
                        rangeEnd == null
        ) {
            throw new RuntimeException(
                    "Debe indicar el rango de fechas del calendario."
            );
        }

        if (
                !rangeEnd.isAfter(
                        rangeStart
                )
        ) {
            throw new RuntimeException(
                    "La fecha de fin debe ser posterior a la fecha de inicio."
            );
        }

        if (
                rangeEnd.isAfter(
                        rangeStart
                                .plusDays(
                                        MAX_RANGE_DAYS
                                )
                )
        ) {
            throw new RuntimeException(
                    "El rango del calendario no puede exceder los "
                            + MAX_RANGE_DAYS
                            + " días."
            );
        }
    }

    /**
     * Normaliza y valida el período.
     */
    private NormalizedEventPeriod
    normalizeAndValidatePeriod(
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd,
            Boolean allDay
    ) {
        if (requestedStart == null) {
            throw new RuntimeException(
                    "La fecha de inicio es obligatoria."
            );
        }

        if (requestedEnd == null) {
            throw new RuntimeException(
                    "La fecha de fin es obligatoria."
            );
        }

        LocalDateTime normalizedStart;
        LocalDateTime normalizedEnd;

        if (Boolean.TRUE.equals(allDay)) {
            LocalDate startDate =
                    requestedStart
                            .toLocalDate();

            LocalDate endDate =
                    requestedEnd
                            .toLocalDate();

            if (
                    endDate.isBefore(
                            startDate
                    )
            ) {
                throw new RuntimeException(
                        "La fecha de fin debe ser posterior a la fecha de inicio."
                );
            }

            normalizedStart =
                    startDate.atStartOfDay();

            /**
             * FullCalendar utiliza el fin exclusivo
             * para eventos de todo el día.
             *
             * Por eso se guarda el inicio del día
             * posterior a la última fecha elegida.
             */
            normalizedEnd =
                    endDate
                            .plusDays(1)
                            .atStartOfDay();

        } else {
            normalizedStart =
                    requestedStart;

            normalizedEnd =
                    requestedEnd;
        }

        if (
                !normalizedEnd.isAfter(
                        normalizedStart
                )
        ) {
            throw new RuntimeException(
                    "La fecha de fin debe ser posterior a la fecha de inicio."
            );
        }

        return new NormalizedEventPeriod(
                normalizedStart,
                normalizedEnd
        );
    }

    /**
     * Impide que dos eventos personales activos
     * ocupen el mismo período.
     */
    private void validateNoEventOverlap(
            Long doctorId,
            Long excludedEventId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        boolean overlap;

        if (excludedEventId == null) {
            overlap =
                    eventRepository
                            .existsActiveOverlap(
                                    doctorId,
                                    startDateTime,
                                    endDateTime
                            );

        } else {
            overlap =
                    eventRepository
                            .existsActiveOverlapExcludingEvent(
                                    doctorId,
                                    excludedEventId,
                                    startDateTime,
                                    endDateTime
                            );
        }

        if (overlap) {
            throw new RuntimeException(
                    "El período seleccionado se superpone con otro evento activo de la agenda."
            );
        }
    }

    private MedicalAgendaEventType
    parseEventType(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    "El tipo de evento es obligatorio."
            );
        }

        try {
            return MedicalAgendaEventType
                    .valueOf(
                            value
                                    .trim()
                                    .toUpperCase(
                                            Locale.ROOT
                                    )
                    );

        } catch (
                IllegalArgumentException exception
        ) {
            throw new RuntimeException(
                    "El tipo de evento no es válido."
            );
        }
    }

    private void validateVersion(
            MedicalAgendaEvent event,
            Long requestVersion
    ) {
        if (requestVersion == null) {
            throw new RuntimeException(
                    "La versión del evento es obligatoria."
            );
        }

        if (
                event.getVersion() == null ||
                        !event.getVersion()
                                .equals(
                                        requestVersion
                                )
        ) {
            throw new RuntimeException(
                    "El evento fue modificado por otro usuario. "
                            + "Actualice la información antes de continuar."
            );
        }
    }

    private String normalizeRequiredText(
            String value,
            int maximumLength,
            String fieldName
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    fieldName
                            + " es obligatorio."
            );
        }

        String normalized =
                value.trim();

        if (
                normalized.length() >
                        maximumLength
        ) {
            throw new RuntimeException(
                    fieldName
                            + " no puede exceder los "
                            + maximumLength
                            + " caracteres."
            );
        }

        return normalized;
    }

    private String normalizeOptionalText(
            String value,
            int maximumLength,
            String fieldName
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        String normalized =
                value.trim();

        if (
                normalized.length() >
                        maximumLength
        ) {
            throw new RuntimeException(
                    fieldName
                            + " no puede exceder los "
                            + maximumLength
                            + " caracteres."
            );
        }

        return normalized;
    }

    private MedicalAgendaEventResponse
    toResponse(
            MedicalAgendaEvent event,
            String message
    ) {
        return MedicalAgendaEventResponse
                .builder()
                .eventId(
                        event.getId()
                )
                .doctorId(
                        event
                                .getDoctor()
                                .getId()
                )
                .doctorName(
                        event
                                .getDoctor()
                                .getFullName()
                )
                .title(
                        event.getTitle()
                )
                .description(
                        event.getDescription()
                )
                .startDateTime(
                        event.getStartDateTime() != null
                                ? event
                                .getStartDateTime()
                                .toString()
                                : null
                )
                .endDateTime(
                        event.getEndDateTime() != null
                                ? event
                                .getEndDateTime()
                                .toString()
                                : null
                )
                .allDay(
                        event.getAllDay()
                )
                .eventType(
                        event
                                .getEventType()
                                .name()
                )
                .eventTypeLabel(
                        getEventTypeLabel(
                                event.getEventType()
                        )
                )
                .color(
                        EVENT_COLOR
                )
                .active(
                        event.getActive()
                )
                .createdAt(
                        event.getCreatedAt() != null
                                ? event
                                .getCreatedAt()
                                .toString()
                                : null
                )
                .createdBy(
                        event.getCreatedBy()
                )
                .updatedAt(
                        event.getUpdatedAt() != null
                                ? event
                                .getUpdatedAt()
                                .toString()
                                : null
                )
                .updatedBy(
                        event.getUpdatedBy()
                )
                .version(
                        event.getVersion()
                )
                .message(
                        message
                )
                .build();
    }

    private String getEventTypeLabel(
            MedicalAgendaEventType eventType
    ) {
        return switch (eventType) {
            case REUNION ->
                    "Reunión";

            case DESCANSO ->
                    "Descanso";

            case CAPACITACION ->
                    "Capacitación";

            case PERSONAL ->
                    "Personal";

            case OTRO ->
                    "Otro";
        };
    }

    private String buildSuccessMessage(
            String prefix,
            MedicalAgendaEvent event
    ) {
        return prefix
                + ". "
                + event.getTitle()
                + " — "
                + event
                .getStartDateTime()
                .toLocalDate()
                + " a "
                + event
                .getEndDateTime()
                .toLocalDate()
                + ".";
    }

    /**
     * Período normalizado del evento.
     */
    private record NormalizedEventPeriod(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
    }
}