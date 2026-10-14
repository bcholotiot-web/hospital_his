package com.hospital.his.medicalagenda.controller;

import com.hospital.his.medicalagenda.dto.CreateMedicalAgendaEventRequest;
import com.hospital.his.medicalagenda.dto.MedicalAgendaEventResponse;
import com.hospital.his.medicalagenda.dto.UpdateMedicalAgendaEventRequest;
import com.hospital.his.medicalagenda.service.MedicalAgendaService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CU-16:
 * Gestión de eventos personales de la
 * agenda médica.
 */
@RestController
@RequestMapping(
        "/api/doctor/agenda/events"
)
public class MedicalAgendaController {

    private final MedicalAgendaService
            medicalAgendaService;

    public MedicalAgendaController(
            MedicalAgendaService
                    medicalAgendaService
    ) {
        this.medicalAgendaService =
                medicalAgendaService;
    }

    /**
     * Lista eventos activos del médico
     * dentro del rango solicitado.
     *
     * Ejemplo:
     *
     * GET /api/doctor/agenda/events
     *     ?start=2026-10-01T00:00:00
     *     &end=2026-11-01T00:00:00
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_READ')"
    )
    @GetMapping
    public ResponseEntity<
            List<MedicalAgendaEventResponse>
            >
    getMyEvents(
            @RequestParam
            @DateTimeFormat(
                    iso =
                            DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(
                    iso =
                            DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime end,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalAgendaService
                        .getMyEvents(
                                start,
                                end,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Obtiene un evento propio.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_READ')"
    )
    @GetMapping("/{eventId}")
    public ResponseEntity<
            MedicalAgendaEventResponse
            >
    getMyEventById(
            @PathVariable
            Long eventId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalAgendaService
                        .getMyEventById(
                                eventId,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Crea un evento personal.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_EVENT_CREATE')"
    )
    @PostMapping
    public ResponseEntity<
            MedicalAgendaEventResponse
            >
    createEvent(
            @Valid
            @RequestBody
            CreateMedicalAgendaEventRequest
                    request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalAgendaService
                        .createEvent(
                                request,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Actualiza un evento propio.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_EVENT_UPDATE')"
    )
    @PutMapping("/{eventId}")
    public ResponseEntity<
            MedicalAgendaEventResponse
            >
    updateEvent(
            @PathVariable
            Long eventId,

            @Valid
            @RequestBody
            UpdateMedicalAgendaEventRequest
                    request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalAgendaService
                        .updateEvent(
                                eventId,
                                request,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Activa o desactiva lógicamente
     * un evento propio.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_EVENT_DELETE')"
    )
    @PatchMapping(
            "/{eventId}/status"
    )
    public ResponseEntity<
            MedicalAgendaEventResponse
            >
    changeEventStatus(
            @PathVariable
            Long eventId,

            @RequestParam
            Boolean active,

            @RequestParam
            Long version,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalAgendaService
                        .changeEventStatus(
                                eventId,
                                active,
                                version,
                                authentication
                                        .getName()
                        )
        );
    }
}