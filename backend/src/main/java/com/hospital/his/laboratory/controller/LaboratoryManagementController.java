package com.hospital.his.laboratory.controller;

import com.hospital.his.laboratory.dto.LaboratoryOrderItemResponse;
import com.hospital.his.laboratory.dto.LaboratoryOrderResponse;
import com.hospital.his.laboratory.dto.LaboratoryOrderSummaryResponse;
import com.hospital.his.laboratory.dto.SaveLaboratoryResultRequest;
import com.hospital.his.laboratory.service.LaboratoryManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/laboratory")
public class LaboratoryManagementController {

    private final LaboratoryManagementService
            laboratoryManagementService;

    public LaboratoryManagementController(
            LaboratoryManagementService laboratoryManagementService
    ) {
        this.laboratoryManagementService =
                laboratoryManagementService;
    }

    /**
     * Ejemplos:
     *
     * GET /api/laboratory/orders
     * GET /api/laboratory/orders?status=EN_PROCESO
     * GET /api/laboratory/orders?patient=Paciente
     * GET /api/laboratory/orders?doctor=Medico
     */
    @GetMapping("/orders")
    public ResponseEntity<
            List<LaboratoryOrderSummaryResponse>
            >
    getLaboratoryOrders(
            @RequestParam(
                    required = false
            )
            String status,

            @RequestParam(
                    required = false
            )
            String patient,

            @RequestParam(
                    required = false
            )
            String doctor,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryManagementService
                        .getLaboratoryOrders(
                                status,
                                patient,
                                doctor,
                                authentication
                                        .getName()
                        )
        );
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<LaboratoryOrderResponse>
    getLaboratoryOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryManagementService
                        .getLaboratoryOrder(
                                orderId,
                                authentication
                                        .getName()
                        )
        );
    }

    @PutMapping(
            "/orders/{orderId}/items/{itemId}/result"
    )
    public ResponseEntity<LaboratoryOrderItemResponse>
    saveResult(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestBody
            SaveLaboratoryResultRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryManagementService
                        .saveResult(
                                orderId,
                                itemId,
                                request,
                                authentication
                                        .getName()
                        )
        );
    }

    @PatchMapping(
            "/orders/{orderId}/items/{itemId}/publish"
    )
    public ResponseEntity<LaboratoryOrderResponse>
    publishResult(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryManagementService
                        .publishResult(
                                orderId,
                                itemId,
                                authentication
                                        .getName()
                        )
        );
    }
}