package com.hospital.his.medicalagenda.controller;

import com.hospital.his.medicalagenda.dto.MedicalAgendaCalendarResponse;
import com.hospital.his.medicalagenda.service.MedicalAgendaCalendarService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * CU-16:
 * Controlador del calendario unificado
 * del médico autenticado.
 */
@RestController
@RequestMapping(
        "/api/doctor/agenda"
)
public class MedicalAgendaCalendarController {

    private final MedicalAgendaCalendarService
            medicalAgendaCalendarService;

    public MedicalAgendaCalendarController(
            MedicalAgendaCalendarService
                    medicalAgendaCalendarService
    ) {
        this.medicalAgendaCalendarService =
                medicalAgendaCalendarService;
    }

    /**
     * Obtiene citas y eventos personales
     * dentro del rango solicitado.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_READ')"
    )
    @GetMapping
    public ResponseEntity<
            MedicalAgendaCalendarResponse
            >
    getMyCalendar(
            @RequestParam
            @DateTimeFormat(
                    iso =
                            DateTimeFormat
                                    .ISO
                                    .DATE_TIME
            )
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(
                    iso =
                            DateTimeFormat
                                    .ISO
                                    .DATE_TIME
            )
            LocalDateTime end,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalAgendaCalendarService
                        .getMyCalendar(
                                start,
                                end,
                                authentication
                                        .getName()
                        )
        );
    }
}