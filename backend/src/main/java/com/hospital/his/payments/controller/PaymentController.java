package com.hospital.his.payments.controller;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.service.AppointmentExpirationService;
import com.hospital.his.payments.dto.PaymentResponse;
import com.hospital.his.payments.dto.PaymentSummaryResponse;
import com.hospital.his.payments.dto.ProcessPaymentRequest;
import com.hospital.his.payments.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AppointmentExpirationService appointmentExpirationService;

    public PaymentController(PaymentService paymentService, AppointmentExpirationService appointmentExpirationService
    ) {
        this.paymentService = paymentService;
        this.appointmentExpirationService = appointmentExpirationService;
    }


    /*
    /process

    ID de cita
    UUID de idempotencia
    Token simulado de la pasarela
    Nombre del titular
    Últimos cuatro dígitos*/
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody ProcessPaymentRequest request, Authentication authentication) {
        PaymentResponse response = paymentService.processPayment(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /*
     * Endpoint de consulta para comprobar que
     * la reserva todavía puede pagarse.
     */
    @GetMapping("/appointments/{appointmentId}/validate")
    public ResponseEntity<Map<String, Object>>
    validateAppointmentForPayment(@PathVariable Long appointmentId, Authentication authentication) {
        Appointment appointment = appointmentExpirationService.validateAppointmentBeforePayment(appointmentId, authentication.getName());

        return ResponseEntity.ok(
                Map.of("appointmentId", appointment.getId(),
                        "status", appointment.getStatus().name(),
                        "reservationExpiresAt", appointment.getReservationExpiresAt().toString(),
                        "message",
                        "La reserva se encuentra vigente y disponible para pago."
                )
        );
    }

    @GetMapping("/appointments/{appointmentId}/summary")
    public ResponseEntity<PaymentSummaryResponse> getPaymentSummary(@PathVariable Long appointmentId,Authentication authentication) {
        return ResponseEntity.ok(
                paymentService.getPaymentSummary(
                        appointmentId,
                        authentication.getName()
                )
        );
    }

}