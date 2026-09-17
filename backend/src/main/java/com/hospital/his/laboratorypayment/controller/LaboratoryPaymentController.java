package com.hospital.his.laboratorypayment.controller;

import com.hospital.his.laboratorypayment.dto.LaboratoryOrderPaymentDetailResponse;
import com.hospital.his.laboratorypayment.dto.LaboratoryOrderPaymentSearchResponse;
import com.hospital.his.laboratorypayment.dto.LaboratoryPaymentResponse;
import com.hospital.his.laboratorypayment.dto.RegisterLaboratoryPaymentRequest;
import com.hospital.his.laboratorypayment.service.LaboratoryPaymentService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/cashier/laboratory-payments"
)
public class LaboratoryPaymentController {

    private final LaboratoryPaymentService
            laboratoryPaymentService;

    public LaboratoryPaymentController(
            LaboratoryPaymentService
                    laboratoryPaymentService
    ) {
        this.laboratoryPaymentService =
                laboratoryPaymentService;
    }

    /**
     * Busca órdenes pendientes mediante DPI
     * o número de orden.
     *
     * Ejemplos:
     *
     * GET /orders/search?criteria=DPI&value=1234567890123
     *
     * GET /orders/search?criteria=ORDER_NUMBER&value=LAB-000001
     */
    @GetMapping("/orders/search")
    public ResponseEntity<
            List<LaboratoryOrderPaymentSearchResponse>
            >
    searchPendingOrders(
            @RequestParam
            String criteria,

            @RequestParam
            String value,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryPaymentService
                        .searchPendingOrders(
                                criteria,
                                value,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Obtiene los datos y exámenes de una orden
     * seleccionada para cobro.
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<
            LaboratoryOrderPaymentDetailResponse
            >
    getOrderDetail(
            @PathVariable
            Long orderId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryPaymentService
                        .getOrderDetail(
                                orderId,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Registra el cobro.
     *
     * La identidad del cajero se obtiene del JWT.
     * Nunca se recibe desde el cuerpo de la petición.
     */
    @PostMapping
    public ResponseEntity<
            LaboratoryPaymentResponse
            >
    registerPayment(
            @Valid
            @RequestBody
            RegisterLaboratoryPaymentRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryPaymentService
                        .registerPayment(
                                request,
                                authentication
                                        .getName()
                        )
        );
    }
}