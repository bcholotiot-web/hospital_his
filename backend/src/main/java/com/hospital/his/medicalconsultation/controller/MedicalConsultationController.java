package com.hospital.his.medicalconsultation.controller;

import com.hospital.his.medicalconsultation.dto.DoctorQueueResponse;
import com.hospital.his.medicalconsultation.dto.Icd10CodeResponse;
import com.hospital.his.medicalconsultation.dto.MedicalConsultationResponse;
import com.hospital.his.medicalconsultation.dto.SaveMedicalConsultationRequest;
import com.hospital.his.medicalconsultation.dto.StartConsultationResponse;
import com.hospital.his.medicalconsultation.service.MedicalConsultationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class MedicalConsultationController {

    private final MedicalConsultationService
            medicalConsultationService;

    public MedicalConsultationController(
            MedicalConsultationService medicalConsultationService
    ) {
        this.medicalConsultationService =
                medicalConsultationService;
    }

    @GetMapping("/consultations/queue")
    public ResponseEntity<List<DoctorQueueResponse>>
    getDoctorQueue(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalConsultationService
                        .getDoctorQueue(
                                authentication.getName()
                        )
        );
    }

    @PatchMapping(
            "/appointments/{appointmentId}/start"
    )
    public ResponseEntity<StartConsultationResponse>
    startConsultation(
            @PathVariable Long appointmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalConsultationService
                        .startConsultation(
                                appointmentId,
                                authentication.getName()
                        )
        );
    }

    @GetMapping(
            "/appointments/{appointmentId}/consultation"
    )
    public ResponseEntity<MedicalConsultationResponse>
    getConsultation(
            @PathVariable Long appointmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalConsultationService
                        .getConsultationByAppointment(
                                appointmentId,
                                authentication.getName()
                        )
        );
    }

    @PutMapping(
            "/appointments/{appointmentId}/consultation"
    )
    public ResponseEntity<MedicalConsultationResponse>
    saveConsultation(
            @PathVariable Long appointmentId,
            @RequestBody
            SaveMedicalConsultationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalConsultationService
                        .saveConsultation(
                                appointmentId,
                                request,
                                authentication.getName()
                        )
        );
    }

    @PatchMapping(
            "/appointments/{appointmentId}/finish-care"
    )
    public ResponseEntity<DoctorQueueResponse>
    finishCare(
            @PathVariable Long appointmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalConsultationService
                        .finishCare(
                                appointmentId,
                                authentication.getName()
                        )
        );
    }

    @PatchMapping(
            "/appointments/{appointmentId}/no-show"
    )
    public ResponseEntity<DoctorQueueResponse>
    markNoShow(
            @PathVariable Long appointmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicalConsultationService
                        .markNoShow(
                                appointmentId,
                                authentication.getName()
                        )
        );
    }

    @GetMapping("/icd10/search")
    public ResponseEntity<List<Icd10CodeResponse>>
    searchIcd10(
            @RequestParam String query
    ) {
        return ResponseEntity.ok(
                medicalConsultationService
                        .searchIcd10(query)
        );
    }
}