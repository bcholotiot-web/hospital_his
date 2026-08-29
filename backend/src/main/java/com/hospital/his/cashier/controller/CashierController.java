package com.hospital.his.cashier.controller;

import com.hospital.his.cashier.service.CashierService;
import com.hospital.his.payments.dto.CashAppointmentResponse;
import com.hospital.his.payments.dto.PaymentResponse;
import com.hospital.his.payments.dto.RegisterCashPaymentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cashier")
public class CashierController {

    private final CashierService cashierService;

    public CashierController(
            CashierService cashierService
    ) {
        this.cashierService =
                cashierService;
    }

    @GetMapping("/appointments/search")
    public ResponseEntity<CashAppointmentResponse>
    searchPendingAppointment(
            @RequestParam String type,
            @RequestParam String value
    ) {
        return ResponseEntity.ok(
                cashierService
                        .searchPendingAppointment(
                                type,
                                value
                        )
        );
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse>
    registerPayment(
            @RequestBody RegisterCashPaymentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                cashierService.registerPayment(
                        request,
                        authentication.getName()
                )
        );
    }
}