package com.hospital.his.reception.controller;

import com.hospital.his.reception.dto.CreateEmergencyReceptionRequest;
import com.hospital.his.reception.dto.EmergencyReceptionResponse;
import com.hospital.his.reception.service.EmergencyReceptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reception/emergencies")
public class EmergencyReceptionController {

    private final EmergencyReceptionService
            emergencyReceptionService;

    public EmergencyReceptionController(
            EmergencyReceptionService emergencyReceptionService
    ) {
        this.emergencyReceptionService =
                emergencyReceptionService;
    }

    @PostMapping
    public ResponseEntity<EmergencyReceptionResponse>
    registerEmergency(
            @RequestBody CreateEmergencyReceptionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                emergencyReceptionService.registerEmergency(
                        request,
                        authentication.getName()
                )
        );
    }
}