package com.hospital.his.appointments.controller;

import com.hospital.his.appointments.dto.AvailableSlotResponse;
import com.hospital.his.appointments.dto.CreateFollowUpAppointmentRequest;
import com.hospital.his.appointments.dto.FollowUpAppointmentContextResponse;
import com.hospital.his.appointments.dto.FollowUpAppointmentResponse;
import com.hospital.his.appointments.service.FollowUpAppointmentService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/doctor/follow-up-appointments"
)
public class FollowUpAppointmentController {

    private final FollowUpAppointmentService
            followUpAppointmentService;

    public FollowUpAppointmentController(
            FollowUpAppointmentService
                    followUpAppointmentService
    ) {
        this.followUpAppointmentService =
                followUpAppointmentService;
    }

    /**
     * Obtiene paciente, médico, especialidad
     * y sucursal desde la consulta padre.
     */
    @GetMapping(
            "/context/{consultationId}"
    )
    public ResponseEntity<
            FollowUpAppointmentContextResponse
            >
    getContext(
            @PathVariable
            Long consultationId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                followUpAppointmentService
                        .getFollowUpContext(
                                consultationId,
                                authentication.getName()
                        )
        );
    }

    /**
     * Obtiene los horarios disponibles del mismo
     * médico que atendió la consulta.
     */
    @GetMapping("/availability")
    public ResponseEntity<
            List<AvailableSlotResponse>
            >
    getAvailability(
            @RequestParam
            Long consultationId,

            @RequestParam
            String date,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                followUpAppointmentService
                        .getAvailableSlots(
                                consultationId,
                                date,
                                authentication.getName()
                        )
        );
    }

    /**
     * Crea la cita de seguimiento.
     */
    @PostMapping
    public ResponseEntity<
            FollowUpAppointmentResponse
            >
    createFollowUpAppointment(
            @Valid
            @RequestBody
            CreateFollowUpAppointmentRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                followUpAppointmentService
                        .createFollowUpAppointment(
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Lista seguimientos programados desde
     * una consulta concreta.
     */
    @GetMapping(
            "/consultation/{consultationId}"
    )
    public ResponseEntity<
            List<FollowUpAppointmentResponse>
            >
    getFollowUpsByConsultation(
            @PathVariable
            Long consultationId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                followUpAppointmentService
                        .getFollowUpsByConsultation(
                                consultationId,
                                authentication.getName()
                        )
        );
    }
}