package com.hospital.his.reception.controller;

import com.hospital.his.reception.dto.ReceptionAppointmentResponse;
import com.hospital.his.reception.dto.ReceptionSearchResponse;
import com.hospital.his.reception.dto.ReassignDoctorRequest;
import com.hospital.his.reception.dto.ReceptionDoctorResponse;
import com.hospital.his.reception.service.ReceptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reception")
public class ReceptionController {

    private final ReceptionService receptionService;

    public ReceptionController(
            ReceptionService receptionService
    ) {
        this.receptionService =
                receptionService;
    }

    @GetMapping("/appointments/search")
    public ResponseEntity<ReceptionSearchResponse>
    searchAppointment(
            @RequestParam String type,
            @RequestParam String value
    ) {
        return ResponseEntity.ok(
                receptionService.searchAppointment(
                        type,
                        value
                )
        );
    }

    @PatchMapping(
            "/appointments/{appointmentId}/arrival"
    )
    public ResponseEntity<ReceptionAppointmentResponse>
    registerArrival(
            @PathVariable Long appointmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                receptionService.registerArrival(
                        appointmentId,
                        authentication.getName()
                )
        );
    }

    @GetMapping(
            "/appointments/{appointmentId}/available-doctors"
    )
    public ResponseEntity<List<ReceptionDoctorResponse>>
    getAvailableDoctorsForReassignment(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(
                receptionService
                        .getAvailableDoctorsForReassignment(
                                appointmentId
                        )
        );
    }

    @PatchMapping(
            "/appointments/{appointmentId}/reassign-doctor"
    )
    public ResponseEntity<ReceptionAppointmentResponse>
    reassignDoctor(
            @PathVariable Long appointmentId,
            @RequestBody ReassignDoctorRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                receptionService.reassignDoctor(
                        appointmentId,
                        request,
                        authentication.getName()
                )
        );
    }
}