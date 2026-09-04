package com.hospital.his.laboratory.controller;

import com.hospital.his.laboratory.dto.CreateLaboratoryOrderRequest;
import com.hospital.his.laboratory.dto.LaboratoryOrderResponse;
import com.hospital.his.laboratory.dto.LaboratoryOrderSummaryResponse;
import com.hospital.his.laboratory.dto.LaboratoryTestResponse;
import com.hospital.his.laboratory.service.DoctorLaboratoryOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/laboratory")
public class DoctorLaboratoryOrderController {

    private final DoctorLaboratoryOrderService
            doctorLaboratoryOrderService;

    public DoctorLaboratoryOrderController(
            DoctorLaboratoryOrderService doctorLaboratoryOrderService
    ) {
        this.doctorLaboratoryOrderService =
                doctorLaboratoryOrderService;
    }

    @GetMapping("/tests")
    public ResponseEntity<List<LaboratoryTestResponse>>
    getActiveLaboratoryTests(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                doctorLaboratoryOrderService
                        .getActiveLaboratoryTests(
                                authentication.getName()
                        )
        );
    }

    @PostMapping("/orders")
    public ResponseEntity<LaboratoryOrderResponse>
    createLaboratoryOrder(
            @RequestBody
            CreateLaboratoryOrderRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                doctorLaboratoryOrderService
                        .createLaboratoryOrder(
                                request,
                                authentication.getName()
                        )
        );
    }

    @GetMapping("/orders")
    public ResponseEntity<
            List<LaboratoryOrderSummaryResponse>
            >
    getMyLaboratoryOrders(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                doctorLaboratoryOrderService
                        .getMyLaboratoryOrders(
                                authentication.getName()
                        )
        );
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<LaboratoryOrderResponse>
    getMyLaboratoryOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                doctorLaboratoryOrderService
                        .getMyLaboratoryOrder(
                                orderId,
                                authentication.getName()
                        )
        );
    }
}