package com.hospital.his.cashier.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.payments.dto.CashAppointmentResponse;
import com.hospital.his.payments.dto.PaymentResponse;
import com.hospital.his.payments.dto.RegisterCashPaymentRequest;
import com.hospital.his.payments.entity.Payment;
import com.hospital.his.payments.entity.PaymentMethod;
import com.hospital.his.payments.entity.PaymentStatus;
import com.hospital.his.payments.gateway.GatewayPaymentResult;
import com.hospital.his.payments.gateway.PaymentGateway;
import com.hospital.his.payments.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CashierService {

    private final AppointmentRepository appointmentRepository;

    private final PaymentRepository paymentRepository;

    private final PaymentGateway paymentGateway;

    private final AuditService auditService;

    private final BigDecimal consultationAmount;

    private final String currency;

    public CashierService(
            AppointmentRepository appointmentRepository,
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            AuditService auditService,
            @Value("${payments.consultation-amount}")
            BigDecimal consultationAmount,
            @Value("${payments.currency}")
            String currency
    ) {
        this.appointmentRepository =
                appointmentRepository;

        this.paymentRepository =
                paymentRepository;

        this.paymentGateway =
                paymentGateway;

        this.auditService =
                auditService;

        this.consultationAmount =
                consultationAmount;

        this.currency =
                currency;
    }

    /*
     * Busca una cita pendiente por número de cita
     * o por DPI del paciente.
     */
    @Transactional(readOnly = true)
    public CashAppointmentResponse searchPendingAppointment(
            String type,
            String value
    ) {
        validateSearchParameters(
                type,
                value
        );

        String normalizedType =
                type.trim()
                        .toUpperCase(Locale.ROOT);

        String cleanValue =
                value.trim();

        Appointment appointment =
                switch (normalizedType) {

                    case "APPOINTMENT_ID" ->
                            searchByAppointmentId(
                                    cleanValue
                            );

                    case "DPI" ->
                            searchByDpi(
                                    cleanValue
                            );

                    default ->
                            throw new RuntimeException(
                                    "El tipo de búsqueda no es válido. Use DPI o APPOINTMENT_ID."
                            );
                };

        return toCashAppointmentResponse(
                appointment
        );
    }

    /*
     * Registra el pago presencial.
     */
    @Transactional
    public PaymentResponse registerPayment(
            RegisterCashPaymentRequest request,
            String cashierUsername
    ) {
        validatePaymentRequest(
                request,
                cashierUsername
        );

        Payment existingPayment =
                paymentRepository
                        .findByIdempotencyKey(
                                request.getIdempotencyKey()
                        )
                        .orElse(null);

        if (existingPayment != null) {
            return toPaymentResponse(
                    existingPayment,
                    buildExistingPaymentMessage(
                            existingPayment
                    )
            );
        }

        Appointment appointment =
                appointmentRepository
                        .findById(request.getAppointmentId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cita no encontrada."
                                )
                        );

        validateAppointmentForPayment(
                appointment
        );

        boolean alreadyPaid =
                paymentRepository
                        .existsByAppointment_IdAndStatus(
                                appointment.getId(),
                                PaymentStatus.APROBADO
                        );

        if (alreadyPaid) {
            throw new RuntimeException(
                    "La cita ya cuenta con un pago aprobado."
            );
        }

        PaymentMethod paymentMethod =
                parsePaymentMethod(
                        request.getPaymentMethod()
                );

        if (paymentMethod == PaymentMethod.EN_LINEA) {
            throw new RuntimeException(
                    "El método EN_LINEA no está disponible en caja."
            );
        }

        if (paymentMethod == PaymentMethod.EFECTIVO) {
            return registerCashPayment(
                    appointment,
                    request,
                    cashierUsername
            );
        }

        return registerCardPayment(
                appointment,
                request,
                paymentMethod,
                cashierUsername
        );
    }

    private Appointment searchByAppointmentId(
            String value
    ) {
        Long appointmentId;

        try {
            appointmentId =
                    Long.valueOf(value);

        } catch (NumberFormatException exception) {
            throw new RuntimeException(
                    "El número de cita debe contener únicamente números."
            );
        }

        return appointmentRepository
                .findByIdAndStatusAndActiveTrue(
                        appointmentId,
                        AppointmentStatus.PENDIENTE_DE_PAGO
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "No se encontraron citas pendientes de pago para el criterio ingresado."
                        )
                );
    }

    private Appointment searchByDpi(
            String dpi
    ) {
        if (!dpi.matches("\\d{13}")) {
            throw new RuntimeException(
                    "El DPI debe contener exactamente 13 dígitos numéricos."
            );
        }

        List<Appointment> appointments =
                appointmentRepository
                        .findByPatient_DpiAndStatusAndActiveTrueOrderByAppointmentDateTimeDesc(
                                dpi,
                                AppointmentStatus.PENDIENTE_DE_PAGO
                        );

        if (appointments.isEmpty()) {
            throw new RuntimeException(
                    "No se encontraron citas pendientes de pago para el criterio ingresado."
            );
        }

        return appointments.get(0);
    }

    private PaymentResponse registerCashPayment(
            Appointment appointment,
            RegisterCashPaymentRequest request,
            String cashierUsername
    ) {
        BigDecimal receivedAmount =
                request.getReceivedAmount();

        if (receivedAmount == null) {
            throw new RuntimeException(
                    "Debe ingresar el monto recibido."
            );
        }

        receivedAmount =
                receivedAmount.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal total =
                consultationAmount.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        if (receivedAmount.compareTo(total) < 0) {
            throw new RuntimeException(
                    "El monto recibido (Q"
                            + receivedAmount
                            + ") es menor al monto a cobrar (Q"
                            + total
                            + ")."
            );
        }

        BigDecimal changeAmount =
                receivedAmount
                        .subtract(total)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        Payment payment =
                buildBasePayment(
                        appointment,
                        request.getIdempotencyKey(),
                        PaymentMethod.EFECTIVO,
                        cashierUsername
                );

        payment.setStatus(
                PaymentStatus.APROBADO
        );

        payment.setReceivedAmount(
                receivedAmount
        );

        payment.setChangeAmount(
                changeAmount
        );

        payment.setTransactionNumber(
                generateTransactionNumber()
        );

        payment.setGatewayReference(
                "CASH-"
                        + UUID.randomUUID()
        );

        payment.setProcessedAt(
                LocalDateTime.now()
        );

        payment = savePaymentSafely(
                payment
        );

        confirmAppointment(
                appointment
        );

        auditService.log(
                cashierUsername,
                "CASH_PAYMENT_APPROVED",
                "CASHIER",
                "Pago en efectivo registrado para la cita ID "
                        + appointment.getId()
                        + ". Monto recibido: Q"
                        + receivedAmount
                        + ". Cambio: Q"
                        + changeAmount
                        + ". Transacción: "
                        + payment.getTransactionNumber()
                        + "."
        );

        return toPaymentResponse(
                payment,
                "¡Pago registrado exitosamente! Paciente: "
                        + appointment
                        .getPatient()
                        .getFullName()
                        + ". La cita ha sido actualizada a estado Confirmada. "
                        + "Monto recibido: Q"
                        + receivedAmount
                        + ". Cambio a devolver: Q"
                        + changeAmount
                        + "."
        );
    }

    private PaymentResponse registerCardPayment(
            Appointment appointment,
            RegisterCashPaymentRequest request,
            PaymentMethod paymentMethod,
            String cashierUsername
    ) {
        if (request.getCardLastFour() == null ||
                !request.getCardLastFour()
                        .trim()
                        .matches("\\d{4}")) {

            throw new RuntimeException(
                    "Debe ingresar los últimos 4 dígitos de la tarjeta."
            );
        }

        if (request.getPaymentToken() == null ||
                request.getPaymentToken().isBlank()) {

            throw new RuntimeException(
                    "Debe indicar el resultado de la transacción de tarjeta."
            );
        }

        Payment payment =
                buildBasePayment(
                        appointment,
                        request.getIdempotencyKey(),
                        paymentMethod,
                        cashierUsername
                );

        payment.setCardLastFour(
                request.getCardLastFour().trim()
        );

        payment.setStatus(
                PaymentStatus.PROCESANDO
        );

        payment = savePaymentSafely(
                payment
        );

        GatewayPaymentResult gatewayResult;

        try {
            gatewayResult =
                    paymentGateway.processPayment(
                            request.getPaymentToken(),
                            consultationAmount,
                            currency
                    );

        } catch (RuntimeException exception) {
            payment.setStatus(
                    PaymentStatus.ERROR
            );

            payment.setFailureCode(
                    "COMMUNICATION_ERROR"
            );

            payment.setFailureMessage(
                    "Error de comunicación con la pasarela de pago. Intente nuevamente."
            );

            payment.setProcessedAt(
                    LocalDateTime.now()
            );

            paymentRepository.saveAndFlush(
                    payment
            );

            auditService.log(
                    cashierUsername,
                    "CASHIER_CARD_PAYMENT_ERROR",
                    "CASHIER",
                    "Error al procesar el pago con tarjeta para la cita ID "
                            + appointment.getId()
                            + "."
            );

            return toPaymentResponse(
                    payment,
                    "Error de comunicación con la pasarela de pago. Intente nuevamente."
            );
        }

        payment.setGatewayReference(
                gatewayResult.getGatewayReference()
        );

        payment.setProcessedAt(
                LocalDateTime.now()
        );

        if (!Boolean.TRUE.equals(
                gatewayResult.getApproved())) {

            payment.setStatus(
                    PaymentStatus.RECHAZADO
            );

            payment.setFailureCode(
                    gatewayResult.getFailureCode()
            );

            payment.setFailureMessage(
                    gatewayResult.getFailureMessage()
            );

            paymentRepository.saveAndFlush(
                    payment
            );

            auditService.log(
                    cashierUsername,
                    "CASHIER_CARD_PAYMENT_REJECTED",
                    "CASHIER",
                    "Pago con tarjeta rechazado para la cita ID "
                            + appointment.getId()
                            + ". Código: "
                            + gatewayResult.getFailureCode()
                            + "."
            );

            return toPaymentResponse(
                    payment,
                    "La transacción con tarjeta fue rechazada por el banco. "
                            + "Solicite al paciente otro método de pago."
            );
        }

        payment.setStatus(
                PaymentStatus.APROBADO
        );

        payment.setTransactionNumber(
                gatewayResult.getTransactionNumber()
        );

        payment.setFailureCode(null);
        payment.setFailureMessage(null);

        payment =
                paymentRepository.saveAndFlush(
                        payment
                );

        confirmAppointment(
                appointment
        );

        auditService.log(
                cashierUsername,
                "CASHIER_CARD_PAYMENT_APPROVED",
                "CASHIER",
                "Pago con "
                        + paymentMethod.name()
                        + " aprobado para la cita ID "
                        + appointment.getId()
                        + ". Transacción: "
                        + payment.getTransactionNumber()
                        + "."
        );

        return toPaymentResponse(
                payment,
                "¡Pago registrado exitosamente! Paciente: "
                        + appointment
                        .getPatient()
                        .getFullName()
                        + ". La cita ha sido actualizada a estado Confirmada."
        );
    }

    private Payment buildBasePayment(
            Appointment appointment,
            UUID idempotencyKey,
            PaymentMethod paymentMethod,
            String cashierUsername
    ) {
        return Payment.builder()
                .appointment(
                        appointment
                )
                .amount(
                        consultationAmount.setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                )
                .currency(
                        currency
                                .trim()
                                .toUpperCase(
                                        Locale.ROOT
                                )
                )
                .status(
                        PaymentStatus.PENDIENTE
                )
                .paymentMethod(
                        paymentMethod
                )
                .receivedAmount(null)
                .changeAmount(null)
                .idempotencyKey(
                        idempotencyKey
                )
                .transactionNumber(null)
                .gatewayReference(null)
                .cardLastFour(null)
                .cardholderName(null)
                .cashierUsername(
                        cashierUsername
                )
                .failureCode(null)
                .failureMessage(null)
                .createdAt(
                        LocalDateTime.now()
                )
                .processedAt(null)
                .build();
    }

    private Payment savePaymentSafely(
            Payment payment
    ) {
        try {
            return paymentRepository
                    .saveAndFlush(payment);

        } catch (DataIntegrityViolationException exception) {
            return paymentRepository
                    .findByIdempotencyKey(
                            payment.getIdempotencyKey()
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "La solicitud de pago ya está siendo procesada."
                            )
                    );
        }
    }

    private void confirmAppointment(
            Appointment appointment
    ) {
        appointment.setStatus(
                AppointmentStatus.CONFIRMADA
        );

        appointment.setActive(true);

        appointmentRepository.saveAndFlush(
                appointment
        );
    }

    private void validateAppointmentForPayment(
            Appointment appointment
    ) {
        if (!Boolean.TRUE.equals(
                appointment.getActive())) {

            throw new RuntimeException(
                    "La cita no se encuentra activa."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.CONFIRMADA ||
                appointment.getStatus()
                        == AppointmentStatus.PACIENTE_PRESENTE) {

            throw new RuntimeException(
                    "La cita ya se encuentra confirmada."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.CANCELADA) {

            throw new RuntimeException(
                    "La cita se encuentra cancelada."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.EXPIRADA) {

            throw new RuntimeException(
                    "La cita se encuentra expirada."
            );
        }

        if (appointment.getStatus()
                != AppointmentStatus.PENDIENTE_DE_PAGO) {

            throw new RuntimeException(
                    "La cita no se encuentra pendiente de pago."
            );
        }
    }

    private void validateSearchParameters(
            String type,
            String value
    ) {
        if (type == null ||
                type.isBlank()) {

            throw new RuntimeException(
                    "Debe seleccionar un tipo de búsqueda."
            );
        }

        if (value == null ||
                value.isBlank()) {

            throw new RuntimeException(
                    "Debe ingresar un criterio de búsqueda."
            );
        }

        if (value.trim().length() > 25) {
            throw new RuntimeException(
                    "El criterio de búsqueda no puede exceder los 25 caracteres."
            );
        }
    }

    private void validatePaymentRequest(
            RegisterCashPaymentRequest request,
            String cashierUsername
    ) {
        if (cashierUsername == null ||
                cashierUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al cajero autenticado."
            );
        }

        if (request == null) {
            throw new RuntimeException(
                    "Los datos del pago son obligatorios."
            );
        }

        if (request.getAppointmentId() == null) {
            throw new RuntimeException(
                    "Debe indicar la cita que desea cobrar."
            );
        }

        if (request.getPaymentMethod() == null ||
                request.getPaymentMethod().isBlank()) {

            throw new RuntimeException(
                    "Debe seleccionar un método de pago."
            );
        }

        if (request.getIdempotencyKey() == null) {
            throw new RuntimeException(
                    "La llave de idempotencia es obligatoria."
            );
        }
    }

    private PaymentMethod parsePaymentMethod(
            String paymentMethod
    ) {
        try {
            return PaymentMethod.valueOf(
                    paymentMethod
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException exception) {
            throw new RuntimeException(
                    "El método de pago no es válido. "
                            + "Seleccione EFECTIVO, VISA, MASTERCARD o DEBITO."
            );
        }
    }

    private String generateTransactionNumber() {
        return "CASH-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    }

    private String buildExistingPaymentMessage(
            Payment payment
    ) {
        return switch (payment.getStatus()) {

            case APROBADO ->
                    "La solicitud de pago ya fue procesada exitosamente.";

            case RECHAZADO ->
                    payment.getFailureMessage() != null
                            ? payment.getFailureMessage()
                            : "La solicitud ya fue procesada y rechazada.";

            case ERROR ->
                    payment.getFailureMessage() != null
                            ? payment.getFailureMessage()
                            : "La solicitud presentó un error.";

            case PROCESANDO ->
                    "La solicitud de pago se encuentra en procesamiento.";

            case PENDIENTE ->
                    "La solicitud de pago se encuentra pendiente.";
        };
    }

    private CashAppointmentResponse
    toCashAppointmentResponse(
            Appointment appointment
    ) {
        return CashAppointmentResponse.builder()
                .appointmentId(
                        appointment.getId()
                )
                .patientName(
                        appointment
                                .getPatient()
                                .getFullName()
                )
                .patientDpi(
                        appointment
                                .getPatient()
                                .getDpi()
                )
                .specialty(
                        appointment
                                .getSpecialty()
                                .getName()
                )
                .doctorName(
                        appointment
                                .getDoctor()
                                .getFullName()
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
                .message(
                        "Cita pendiente de pago encontrada."
                )
                .build();
    }

    private PaymentResponse toPaymentResponse(
            Payment payment,
            String message
    ) {
        Appointment appointment =
                payment.getAppointment();

        return PaymentResponse.builder()
                .paymentId(
                        payment.getId()
                )
                .appointmentId(
                        appointment.getId()
                )
                .transactionNumber(
                        payment.getTransactionNumber()
                )
                .amount(
                        payment.getAmount()
                )
                .currency(
                        payment.getCurrency()
                )
                .status(
                        payment.getStatus().name()
                )
                .paymentMethod(
                        payment.getPaymentMethod() != null
                                ? payment
                                .getPaymentMethod()
                                .name()
                                : null
                )
                .receivedAmount(
                        payment.getReceivedAmount()
                )
                .changeAmount(
                        payment.getChangeAmount()
                )
                .cashierUsername(
                        payment.getCashierUsername()
                )
                .cardLastFour(
                        payment.getCardLastFour()
                )
                .processedAt(
                        payment.getProcessedAt() != null
                                ? payment
                                .getProcessedAt()
                                .toString()
                                : null
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
                .message(message)
                .build();
    }
}