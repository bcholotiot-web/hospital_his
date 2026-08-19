package com.hospital.his.appointments.controller;

import com.hospital.his.appointments.dto.AppointmentResponse;
import com.hospital.his.appointments.dto.CreateAppointmentRequest;
import com.hospital.his.appointments.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hospital.his.appointments.dto.AvailableSlotResponse;
import com.hospital.his.appointments.dto.DoctorResponse;
import com.hospital.his.catalogs.dto.SpecialtyResponse;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService)
    {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@RequestBody CreateAppointmentRequest request)
    {
        return ResponseEntity.ok(appointmentService.createAppointment(request));
    }

    @GetMapping("/specialties")
    public ResponseEntity<List<SpecialtyResponse>> getSpecialtiesByBranch(
            @RequestParam Long branchId
    ) {
        return ResponseEntity.ok(
                appointmentService.getSpecialtiesByBranch(branchId)
        );
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByBranchAndSpecialty(
            @RequestParam Long branchId,
            @RequestParam Long specialtyId
    ) {
        return ResponseEntity.ok(
                appointmentService.getDoctorsByBranchAndSpecialty(
                        branchId,
                        specialtyId
                )
        );
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam String date
    ) {
        return ResponseEntity.ok(
                appointmentService.getAvailableSlots(
                        doctorId,
                        date
                )
        );
    }
}