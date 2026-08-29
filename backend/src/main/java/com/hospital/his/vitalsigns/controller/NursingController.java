package com.hospital.his.vitalsigns.controller;

import com.hospital.his.vitalsigns.dto.NursingQueueResponse;
import com.hospital.his.vitalsigns.dto.RegisterVitalSignsRequest;
import com.hospital.his.vitalsigns.dto.VitalSignsResponse;
import com.hospital.his.vitalsigns.service.NursingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nursing")
public class NursingController {

    private final NursingService nursingService;

    public NursingController(
            NursingService nursingService
    ) {
        this.nursingService =
                nursingService;
    }

    @GetMapping("/queue")
    public ResponseEntity<List<NursingQueueResponse>>
    getNursingQueue() {

        return ResponseEntity.ok(
                nursingService.getNursingQueue()
        );
    }

    @PatchMapping(
            "/appointments/{appointmentId}/call"
    )
    public ResponseEntity<NursingQueueResponse>
    callPatient(
            @PathVariable Long appointmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                nursingService.callPatient(
                        appointmentId,
                        authentication.getName()
                )
        );
    }

    @PostMapping("/vital-signs")
    public ResponseEntity<VitalSignsResponse>
    registerVitalSigns(
            @RequestBody
            RegisterVitalSignsRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                nursingService.registerVitalSigns(
                        request,
                        authentication.getName()
                )
        );
    }

    @GetMapping(
            "/appointments/{appointmentId}/vital-signs"
    )
    public ResponseEntity<VitalSignsResponse>
    getVitalSigns(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(
                nursingService
                        .getVitalSignsByAppointment(
                                appointmentId
                        )
        );
    }

    @PatchMapping("/emergencies/{emergencyReceptionId}/call")
    public ResponseEntity<NursingQueueResponse>
    callEmergencyPatient(@PathVariable Long emergencyReceptionId, Authentication authentication) {
        return ResponseEntity.ok(
                nursingService.callEmergencyPatient(
                        emergencyReceptionId,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/emergencies/{emergencyReceptionId}/vital-signs")
    public ResponseEntity<VitalSignsResponse> getEmergencyVitalSigns(@PathVariable Long emergencyReceptionId) {
        return ResponseEntity.ok(
                nursingService
                        .getVitalSignsByEmergency(
                                emergencyReceptionId
                        )
        );
    }
}