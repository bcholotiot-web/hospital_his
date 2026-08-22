package com.hospital.his.payments.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.appointments.service.AppointmentExpirationService;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.payments.dto.PaymentResponse;
import com.hospital.his.payments.dto.PaymentSummaryResponse;
import com.hospital.his.payments.dto.ProcessPaymentRequest;
import com.hospital.his.payments.entity.Payment;
import com.hospital.his.payments.entity.PaymentStatus;
import com.hospital.his.payments.gateway.GatewayPaymentResult;
import com.hospital.his.payments.gateway.PaymentGateway;
import com.hospital.his.payments.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentExpirationService appointmentExpirationService;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final BigDecimal consultationAmount;
    private final String currency;

    public PaymentService(
            PaymentRepository paymentRepository,
            AppointmentRepository appointmentRepository,
            AppointmentExpirationService appointmentExpirationService,
            PaymentGateway paymentGateway,
            AuditService auditService,
            @Value("${payments.consultation-amount}")
            BigDecimal consultationAmount,
            @Value("${payments.currency}")
            String currency
    ) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentExpirationService = appointmentExpirationService;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.consultationAmount = consultationAmount;
        this.currency = currency;
    }

    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request, String authenticatedUsername) {
        validateRequest(request, authenticatedUsername);

        /*
         * Si ya se procesó la misma llave,
         * devolvemos el resultado anterior.

         * Esto evita un segundo cobro por doble clic
         * o por repetición de la solicitud.
         */
        Payment existingPayment = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);

        if (existingPayment != null) {
            return toResponse(existingPayment, buildExistingPaymentMessage(existingPayment));
        }

        /*
         * Valida:
         * - Que la cita pertenezca al paciente.
         * - Que esté pendiente de pago.
         * - Que permanezca activa.
         * - Que la reserva no haya expirado.
         */
        Appointment appointment = appointmentExpirationService.validateAppointmentBeforePayment(request.getAppointmentId(), authenticatedUsername);

        boolean alreadyPaid = paymentRepository.existsByAppointment_IdAndStatus(appointment.getId(), PaymentStatus.APROBADO);

        if (alreadyPaid || appointment.getStatus() == AppointmentStatus.PAGADA || appointment.getStatus() == AppointmentStatus.CONFIRMADA) {
            throw new RuntimeException("La cita ya cuenta con un pago aprobado.");
        }

        String normalizedCardholderName = request.getCardholderName().trim().toUpperCase(Locale.ROOT);
        String normalizedLastFour = request.getCardLastFour().trim();

        Payment payment = Payment.builder().appointment(appointment)
                        .amount(consultationAmount)
                        .currency(currency.trim().toUpperCase(Locale.ROOT))
                        .status(PaymentStatus.PROCESANDO)
                        .idempotencyKey(request.getIdempotencyKey())
                        .transactionNumber(null)
                        .gatewayReference(null)
                        .cardLastFour(normalizedLastFour)
                        .cardholderName(normalizedCardholderName)
                        .failureCode(null)
                        .failureMessage(null)
                        .createdAt(LocalDateTime.now())
                        .processedAt(null)
                        .build();

        /*
         * Se guarda antes de invocar la pasarela
         * para reservar la llave de idempotencia.
         */
        try {
            payment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {

            Payment duplicatedPayment = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                            .orElseThrow(() -> new RuntimeException("La solicitud de pago ya está siendo procesada."));

            return toResponse(duplicatedPayment, buildExistingPaymentMessage(duplicatedPayment));
        }

        GatewayPaymentResult gatewayResult;

        try {
            gatewayResult =
                    paymentGateway.processPayment(
                            request.getPaymentToken(),
                            consultationAmount,
                            currency
                    );

        } catch (RuntimeException exception) {

            payment.setStatus(PaymentStatus.ERROR);
            payment.setFailureCode("GATEWAY_COMMUNICATION_ERROR");
            payment.setFailureMessage("Error de comunicación con la pasarela de pago. " + "Intente nuevamente en unos minutos.");
            payment.setProcessedAt(LocalDateTime.now());

            payment = paymentRepository.saveAndFlush(payment);

            auditService.log(
                    authenticatedUsername,
                    "PAYMENT_ERROR",
                    "PAYMENTS",
                    "Error al procesar el pago de la cita ID "
                            + appointment.getId()
                            + ". Intento de pago ID "
                            + payment.getId()
                            + ".");

            return toResponse(payment, payment.getFailureMessage());
        }

        payment.setGatewayReference(gatewayResult.getGatewayReference());

        payment.setProcessedAt(
                LocalDateTime.now()
        );

        if (Boolean.TRUE.equals(
                gatewayResult.getApproved())) {

            return approvePayment(
                    payment,
                    appointment,
                    gatewayResult,
                    authenticatedUsername
            );
        }

        return rejectPayment(
                payment,
                appointment,
                gatewayResult,
                authenticatedUsername
        );
    }

    /*La cita existe
    Pertenece al paciente autenticado
    Está activa
    Está PENDIENTE_DE_PAGO
    No está pagada
    No está cancelada
    La reserva todavía no venció*/
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary(Long appointmentId, String authenticatedUsername) {
        Appointment appointment =
                appointmentExpirationService
                        .validateAppointmentBeforePayment(
                                appointmentId,
                                authenticatedUsername
                        );

        return PaymentSummaryResponse.builder()
                .appointmentId(
                        appointment.getId()
                )
                .doctorName(
                        appointment
                                .getDoctor()
                                .getFullName()
                )
                .specialty(
                        appointment
                                .getSpecialty()
                                .getName()
                )
                .branch(
                        appointment
                                .getBranch()
                                .getName()
                )
                .appointmentDateTime(
                        appointment
                                .getAppointmentDateTime()
                                .toString()
                )
                .status(
                        appointment
                                .getStatus()
                                .name()
                )
                .amount(
                        consultationAmount
                )
                .currency(
                        currency
                )
                .reservationExpiresAt(
                        appointment
                                .getReservationExpiresAt()
                                .toString()
                )
                .build();
    }

    private PaymentResponse approvePayment(Payment payment,Appointment appointment,GatewayPaymentResult gatewayResult,String authenticatedUsername) {
        payment.setStatus(
                PaymentStatus.APROBADO
        );

        payment.setTransactionNumber(
                gatewayResult.getTransactionNumber()
        );

        payment.setFailureCode(null);
        payment.setFailureMessage(null);

        appointment.setStatus(
                AppointmentStatus.PAGADA
        );

        /*
         * La cita continúa activa porque el horario
         * debe permanecer reservado después del pago.
         */
        appointment.setActive(true);

        appointmentRepository.save(
                appointment
        );

        payment =
                paymentRepository.saveAndFlush(
                        payment
                );

        auditService.log(
                authenticatedUsername,
                "PAYMENT_APPROVED",
                "PAYMENTS",
                "Pago aprobado para la cita ID "
                        + appointment.getId()
                        + ". Número de transacción: "
                        + payment.getTransactionNumber()
                        + "."
        );

        return toResponse(
                payment,
                "¡Pago realizado exitosamente! Número de transacción: "
                        + payment.getTransactionNumber()
                        + ". Su cita ha sido confirmada."
        );
    }

    private PaymentResponse rejectPayment(Payment payment,Appointment appointment,GatewayPaymentResult gatewayResult,String authenticatedUsername) {
        payment.setStatus(
                PaymentStatus.RECHAZADO
        );

        payment.setTransactionNumber(null);

        payment.setFailureCode(
                gatewayResult.getFailureCode()
        );

        payment.setFailureMessage(
                gatewayResult.getFailureMessage()
        );

        payment =
                paymentRepository.saveAndFlush(
                        payment
                );

        /*
         * La cita permanece pendiente de pago.
         * El paciente puede reintentar mientras
         * reservationExpiresAt siga vigente.
         */
        appointment.setStatus(
                AppointmentStatus.PENDIENTE_DE_PAGO
        );

        appointmentRepository.save(
                appointment
        );

        auditService.log(
                authenticatedUsername,
                "PAYMENT_REJECTED",
                "PAYMENTS",
                "Pago rechazado para la cita ID "
                        + appointment.getId()
                        + ". Código: "
                        + gatewayResult.getFailureCode()
                        + "."
        );

        return toResponse(
                payment,
                gatewayResult.getFailureMessage()
        );
    }

    private void validateRequest(
            ProcessPaymentRequest request,
            String authenticatedUsername
    ) {
        if (authenticatedUsername == null ||
                authenticatedUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al paciente autenticado."
            );
        }

        if (request == null) {
            throw new RuntimeException(
                    "Los datos del pago son obligatorios."
            );
        }

        if (request.getAppointmentId() == null) {
            throw new RuntimeException(
                    "Debe indicar la cita que desea pagar."
            );
        }

        if (request.getIdempotencyKey() == null) {
            throw new RuntimeException(
                    "La llave de idempotencia es obligatoria."
            );
        }

        if (request.getPaymentToken() == null ||
                request.getPaymentToken().isBlank()) {

            throw new RuntimeException(
                    "El token de pago es obligatorio."
            );
        }

        if (request.getCardholderName() == null ||
                request.getCardholderName().isBlank()) {

            throw new RuntimeException(
                    "El nombre del titular es obligatorio."
            );
        }

        String cardholderName =
                request.getCardholderName()
                        .trim();

        if (cardholderName.length() < 5 ||
                cardholderName.length() > 100) {

            throw new RuntimeException(
                    "El nombre del titular debe contener entre 5 y 100 caracteres."
            );
        }

        if (request.getCardLastFour() == null ||
                !request.getCardLastFour()
                        .matches("\\d{4}")) {

            throw new RuntimeException(
                    "Los últimos cuatro dígitos de la tarjeta no son válidos."
            );
        }

        if (consultationAmount == null ||
                consultationAmount.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            throw new RuntimeException(
                    "El monto configurado para la consulta no es válido."
            );
        }

        if (currency == null ||
                currency.isBlank()) {

            throw new RuntimeException(
                    "La moneda configurada para el pago no es válida."
            );
        }
    }

    private String buildExistingPaymentMessage(Payment payment) {
        return switch (payment.getStatus()) {

            case APROBADO ->
                    "La solicitud ya fue procesada exitosamente.";

            case RECHAZADO ->
                    payment.getFailureMessage() != null
                            ? payment.getFailureMessage()
                            : "La solicitud ya fue procesada y rechazada.";

            case ERROR ->
                    payment.getFailureMessage() != null
                            ? payment.getFailureMessage()
                            : "La solicitud presentó un error de procesamiento.";

            case PROCESANDO ->
                    "La solicitud de pago se encuentra en procesamiento.";

            case PENDIENTE ->
                    "La solicitud de pago se encuentra pendiente.";
        };
    }

    private PaymentResponse toResponse(Payment payment,String message) {
        Appointment appointment =
                payment.getAppointment();

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .appointmentId(appointment.getId())
                .transactionNumber(payment.getTransactionNumber())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .cardLastFour(payment.getCardLastFour())
                .processedAt(payment.getProcessedAt() != null
                                ? payment
                                .getProcessedAt()
                                .toString()
                                : null
                )
                .doctorName(appointment.getDoctor().getFullName())
                .specialty(appointment.getSpecialty().getName())
                .branch(appointment.getBranch().getName())
                .appointmentDateTime(appointment.getAppointmentDateTime().toString()
                )
                .message(message)
                .build();
    }
}