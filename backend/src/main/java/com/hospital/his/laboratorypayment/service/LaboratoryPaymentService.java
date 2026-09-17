package com.hospital.his.laboratorypayment.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.laboratory.dto.LaboratoryOrderItemResponse;
import com.hospital.his.laboratory.entity.LaboratoryOrder;
import com.hospital.his.laboratory.entity.LaboratoryOrderItem;
import com.hospital.his.laboratory.entity.LaboratoryOrderStatus;
import com.hospital.his.laboratory.repository.LaboratoryOrderItemRepository;
import com.hospital.his.laboratory.repository.LaboratoryOrderRepository;
import com.hospital.his.laboratorypayment.dto.LaboratoryOrderPaymentDetailResponse;
import com.hospital.his.laboratorypayment.dto.LaboratoryOrderPaymentSearchResponse;
import com.hospital.his.laboratorypayment.dto.LaboratoryPaymentResponse;
import com.hospital.his.laboratorypayment.dto.RegisterLaboratoryPaymentRequest;
import com.hospital.his.laboratorypayment.entity.LaboratoryPayment;
import com.hospital.his.laboratorypayment.repository.LaboratoryPaymentRepository;
import com.hospital.his.payments.entity.PaymentMethod;
import com.hospital.his.payments.entity.PaymentStatus;
import com.hospital.his.payments.gateway.GatewayPaymentResult;
import com.hospital.his.payments.gateway.PaymentGateway;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LaboratoryPaymentService {

    private static final String CURRENCY = "GTQ";

    private static final String CASHIER_ROLE =
            "CAJERO";

    private static final String INVALID_DPI_MESSAGE =
            "El DPI debe contener exactamente 13 dígitos numéricos.";

    private final LaboratoryPaymentRepository
            laboratoryPaymentRepository;

    private final LaboratoryOrderRepository
            laboratoryOrderRepository;

    private final LaboratoryOrderItemRepository
            laboratoryOrderItemRepository;

    private final UserRepository userRepository;

    private final PaymentGateway paymentGateway;

    private final AuditService auditService;

    public LaboratoryPaymentService(
            LaboratoryPaymentRepository
                    laboratoryPaymentRepository,
            LaboratoryOrderRepository
                    laboratoryOrderRepository,
            LaboratoryOrderItemRepository
                    laboratoryOrderItemRepository,
            UserRepository userRepository,
            PaymentGateway paymentGateway,
            AuditService auditService
    ) {
        this.laboratoryPaymentRepository =
                laboratoryPaymentRepository;

        this.laboratoryOrderRepository =
                laboratoryOrderRepository;

        this.laboratoryOrderItemRepository =
                laboratoryOrderItemRepository;

        this.userRepository =
                userRepository;

        this.paymentGateway =
                paymentGateway;

        this.auditService =
                auditService;
    }

    /**
     * Busca órdenes pendientes de pago.
     *
     * Los criterios permitidos son:
     *
     * DPI
     * ORDER_NUMBER
     *
     * Las órdenes EN_PROCESO, COMPLETADAS o CANCELADAS
     * nunca son devueltas.
     */
    @Transactional(readOnly = true)
    public List<LaboratoryOrderPaymentSearchResponse> searchPendingOrders(String criteria, String value, String cashierUsername) {
        User cashier =
                validateCashier(
                        cashierUsername
                );

        String normalizedCriteria =
                normalizeRequiredText(
                        criteria,
                        "Debe seleccionar un criterio de búsqueda."
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        String normalizedValue =
                normalizeRequiredText(
                        value,
                        "Debe ingresar un criterio de búsqueda."
                );

        List<LaboratoryOrder> orders;

        switch (normalizedCriteria) {
            case "DPI":
                validateDpi(
                        normalizedValue
                );

                orders =
                        laboratoryOrderRepository
                                .findByPatient_DpiAndStatusAndActiveTrueOrderByCreatedAtDesc(
                                        normalizedValue,
                                        LaboratoryOrderStatus
                                                .PENDIENTE_DE_PAGO
                                );
                break;

            case "ORDER_NUMBER":
            case "ORDER":
            case "NUMERO_ORDEN":
                orders =
                        laboratoryOrderRepository
                                .findByOrderNumberIgnoreCaseAndStatusAndActiveTrue(
                                        normalizedValue,
                                        LaboratoryOrderStatus
                                                .PENDIENTE_DE_PAGO
                                )
                                .map(List::of)
                                .orElseGet(List::of);
                break;

            default:
                throw new RuntimeException(
                        "El criterio de búsqueda no es válido. " +
                                "Use DPI u ORDER_NUMBER."
                );
        }

        /*
         * Si el cajero tiene una sucursal asignada,
         * solo puede cobrar órdenes de esa sucursal.
         */
        return orders.stream()
                /*Solo se muestran órdenes correspondientesa la sucursal del cajero.*/
                .filter(order -> belongsToCashierBranch(order, cashier))
                /*Las órdenes externas no deben cobrarse en la caja del hospital.*/
                .filter(order -> !Boolean.TRUE.equals(order.getExternalOrder()))
                .map(this::toSearchResponse)
                .toList();
    }

    /**
     * Obtiene el detalle completo de una orden
     * seleccionada desde la tabla de resultados.
     */
    @Transactional(readOnly = true)
    public LaboratoryOrderPaymentDetailResponse
    getOrderDetail(
            Long orderId,
            String cashierUsername
    ) {
        User cashier = validateCashier(cashierUsername);

        LaboratoryOrder order =
                findActiveOrder(
                        orderId
                );

        validateCashierBranch(
                order,
                cashier
        );

        validateOrderPending(
                order
        );

        List<LaboratoryOrderItem> items =
                laboratoryOrderItemRepository
                        .findByLaboratoryOrder_IdOrderByIdAsc(
                                order.getId()
                        );

        return LaboratoryOrderPaymentDetailResponse
                .builder()
                .laboratoryOrderId(
                        order.getId()
                )
                .orderNumber(
                        order.getOrderNumber()
                )
                .patientName(
                        order.getPatient()
                                .getFullName()
                )
                .patientDpi(
                        order.getPatient()
                                .getDpi()
                )
                .doctorName(
                        order.getDoctor()
                                .getFullName()
                )
                .branch(
                        order.getAppointment()
                                .getBranch()
                                .getName()
                )
                .createdAt(
                        order.getCreatedAt()
                                .toString()
                )
                .status(
                        order.getStatus()
                                .name()
                )
                .totalTests(
                        items.size()
                )
                .totalAmount(
                        normalizeMoney(
                                order.getTotalAmount()
                        )
                )
                .currency(CURRENCY)
                .canPay(true)
                .items(
                        items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )
                .message(
                        "Orden de laboratorio disponible para cobro."
                )
                .build();
    }

    /**
     * Ejecuta el cobro completo.
     *
     * La anotación @Transactional garantiza que:
     *
     * 1. El pago se guarda.
     * 2. La orden cambia a EN_PROCESO.
     * 3. paidAt y updatedAt se actualizan.
     *
     * Si una operación falla, se revierten los cambios
     * realizados dentro de la transacción.
     */
    @Transactional
    public LaboratoryPaymentResponse registerPayment(
            RegisterLaboratoryPaymentRequest request,
            String cashierUsername
    ) {
        validateRegisterRequest(
                request
        );

        User cashier =
                validateCashier(
                        cashierUsername
                );

        /*
         * Idempotencia:
         * si la llave ya fue utilizada, se entrega
         * el resultado anterior sin cobrar nuevamente.
         */
        LaboratoryPayment existingPayment =
                laboratoryPaymentRepository
                        .findByIdempotencyKey(
                                request.getIdempotencyKey()
                        )
                        .orElse(null);

        if (existingPayment != null) {
            /*
             * La llave no puede reutilizarse para una
             * orden diferente.
             */
            if (!existingPayment
                    .getLaboratoryOrder()
                    .getId()
                    .equals(
                            request
                                    .getLaboratoryOrderId()
                    )) {
                throw new RuntimeException(
                        "La llave de idempotencia ya fue utilizada " +
                                "para otra orden de laboratorio."
                );
            }

            return toPaymentResponse(
                    existingPayment,
                    "La solicitud ya fue procesada anteriormente."
            );
        }

        /*
         * Bloquear la orden durante el cobro.
         */
        LaboratoryOrder order =
                laboratoryOrderRepository
                        .findByIdForPayment(
                                request
                                        .getLaboratoryOrderId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Orden de laboratorio no encontrada."
                                )
                        );

        validateCashierBranch(
                order,
                cashier
        );

        validateOrderPending(
                order
        );

        validateNoApprovedPayment(
                order
        );

        PaymentMethod paymentMethod =
                parsePaymentMethod(
                        request.getPaymentMethod()
                );

        if (
                paymentMethod ==
                        PaymentMethod.EFECTIVO
        ) {
            return processCashPayment(
                    order,
                    request,
                    cashier
            );
        }

        return processCardPayment(
                order,
                request,
                cashier,
                paymentMethod
        );
    }

    /**
     * Procesa un cobro en efectivo.
     *
     * Valida el monto recibido y calcula el cambio.
     * No utiliza la pasarela.
     */
    private LaboratoryPaymentResponse
    processCashPayment(
            LaboratoryOrder order,
            RegisterLaboratoryPaymentRequest request,
            User cashier
    ) {
        BigDecimal orderAmount =
                normalizeMoney(
                        order.getTotalAmount()
                );

        BigDecimal receivedAmount =
                request.getReceivedAmount();

        if (receivedAmount == null) {
            throw new RuntimeException(
                    "Debe ingresar el monto recibido."
            );
        }

        receivedAmount =
                normalizeMoney(
                        receivedAmount
                );

        if (
                receivedAmount.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {
            throw new RuntimeException(
                    "El monto recibido debe ser mayor que cero."
            );
        }

        if (
                receivedAmount.compareTo(
                        orderAmount
                ) < 0
        ) {
            throw new RuntimeException(
                    "El monto recibido (Q" +
                            formatMoney(receivedAmount) +
                            ") es menor al monto a cobrar (Q" +
                            formatMoney(orderAmount) +
                            ")."
            );
        }

        BigDecimal changeAmount =
                receivedAmount
                        .subtract(
                                orderAmount
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        LaboratoryPayment payment =
                createBasePayment(
                        order,
                        request,
                        cashier,
                        PaymentMethod.EFECTIVO
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

        payment.setProcessedAt(
                LocalDateTime.now()
        );

        return approvePayment(
                order,
                payment
        );
    }

    /**
     * Procesa VISA, MASTERCARD o DEBITO.
     *
     * Valida los cuatro últimos dígitos y el token.
     * Después delega el procesamiento a PaymentGateway.
     */
    private LaboratoryPaymentResponse
    processCardPayment(
            LaboratoryOrder order,
            RegisterLaboratoryPaymentRequest request,
            User cashier,
            PaymentMethod paymentMethod
    ) {
        validateCardData(
                request
        );

        LaboratoryPayment payment =
                createBasePayment(
                        order,
                        request,
                        cashier,
                        paymentMethod
                );

        payment.setStatus(
                PaymentStatus.PROCESANDO
        );

        payment.setCardLastFour(
                request.getCardLastFour()
        );

        /*
         * Guardar el intento antes de contactar
         * la pasarela.
         */
        payment =
                laboratoryPaymentRepository
                        .saveAndFlush(
                                payment
                        );

        GatewayPaymentResult gatewayResult;

        try {
            gatewayResult =
                    paymentGateway.processPayment(
                            request.getPaymentToken(),
                            order.getTotalAmount(),
                            CURRENCY
                    );

        } catch (RuntimeException exception) {
            payment.setStatus(
                    PaymentStatus.ERROR
            );

            payment.setFailureCode(
                    "GATEWAY_EXCEPTION"
            );

            payment.setFailureMessage(
                    "No fue posible comunicarse con la pasarela."
            );

            payment.setProcessedAt(
                    LocalDateTime.now()
            );

            laboratoryPaymentRepository
                    .saveAndFlush(payment);

            auditService.log(
                    cashier.getUsername(),
                    "LABORATORY_PAYMENT_ERROR",
                    "LABORATORY_PAYMENT",
                    "Error de pasarela para orden " +
                            order.getOrderNumber() + "."
            );

            return toPaymentResponse(
                    payment,
                    "Error de comunicación con la pasarela de pago. " +
                            "Solicite al paciente otro método de pago " +
                            "o intente nuevamente."
            );
        }

        if (
                !Boolean.TRUE.equals(
                        gatewayResult.getApproved()
                )
        ) {
            PaymentStatus finalStatus =
                    isTechnicalGatewayError(
                            gatewayResult
                    )
                            ? PaymentStatus.ERROR
                            : PaymentStatus.RECHAZADO;

            payment.setStatus(
                    finalStatus
            );

            payment.setGatewayReference(
                    gatewayResult
                            .getGatewayReference()
            );

            payment.setFailureCode(
                    gatewayResult
                            .getFailureCode()
            );

            payment.setFailureMessage(
                    gatewayResult
                            .getFailureMessage()
            );

            payment.setProcessedAt(
                    LocalDateTime.now()
            );

            laboratoryPaymentRepository
                    .saveAndFlush(
                            payment
                    );

            /*
             * La orden permanece PENDIENTE_DE_PAGO.
             */
            auditService.log(
                    cashier.getUsername(),
                    finalStatus ==
                            PaymentStatus.RECHAZADO
                            ? "LABORATORY_PAYMENT_REJECTED"
                            : "LABORATORY_PAYMENT_ERROR",
                    "LABORATORY_PAYMENT",
                    "Pago no aprobado para orden " +
                            order.getOrderNumber() +
                            ". Código: " +
                            gatewayResult.getFailureCode() + "."
            );

            return toPaymentResponse(
                    payment,
                    gatewayResult
                            .getFailureMessage()
            );
        }

        payment.setStatus(
                PaymentStatus.APROBADO
        );

        payment.setTransactionNumber(
                gatewayResult
                        .getTransactionNumber()
        );

        payment.setGatewayReference(
                gatewayResult
                        .getGatewayReference()
        );

        payment.setFailureCode(null);
        payment.setFailureMessage(null);

        payment.setProcessedAt(
                LocalDateTime.now()
        );

        return approvePayment(
                order,
                payment
        );
    }

    /**
     * Finaliza un pago aprobado.
     *
     * Cambia la orden a EN_PROCESO, lo que habilita
     * al laboratorio para registrar resultados.
     */
    private LaboratoryPaymentResponse
    approvePayment(
            LaboratoryOrder order,
            LaboratoryPayment payment
    ) {
        LocalDateTime now =
                LocalDateTime.now();

        payment.setProcessedAt(now);

        order.setStatus(
                LaboratoryOrderStatus.EN_PROCESO
        );

        order.setPaidAt(now);
        order.setUpdatedAt(now);

        try {
            payment =
                    laboratoryPaymentRepository
                            .saveAndFlush(
                                    payment
                            );

            laboratoryOrderRepository
                    .saveAndFlush(
                            order
                    );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "No fue posible registrar el pago. " +
                            "La solicitud puede haber sido procesada anteriormente."
            );
        }

        auditService.log(
                payment.getCashierUsername(),
                "LABORATORY_PAYMENT_APPROVED",
                "LABORATORY_PAYMENT",
                "Pago aprobado para orden " +
                        order.getOrderNumber() +
                        ". Método: " +
                        payment.getPaymentMethod().name() +
                        ". Transacción: " +
                        payment.getTransactionNumber() + "."
        );

        return toPaymentResponse(
                payment,
                "¡Pago de laboratorio registrado exitosamente! " +
                        "Paciente: " +
                        order.getPatient().getFullName() +
                        ". La orden ha sido actualizada a estado " +
                        "'En proceso'."
        );
    }

    /**
     * Construye un intento de pago con los campos comunes.
     */
    private LaboratoryPayment createBasePayment(
            LaboratoryOrder order,
            RegisterLaboratoryPaymentRequest request,
            User cashier,
            PaymentMethod paymentMethod
    ) {
        return LaboratoryPayment.builder()
                .laboratoryOrder(order)
                .amount(
                        normalizeMoney(
                                order.getTotalAmount()
                        )
                )
                .currency(CURRENCY)
                .paymentMethod(paymentMethod)
                .status(
                        PaymentStatus.PENDIENTE
                )
                .receivedAmount(null)
                .changeAmount(null)
                .cardLastFour(null)
                .transactionNumber(null)
                .gatewayReference(null)
                .idempotencyKey(
                        request.getIdempotencyKey()
                )
                .cashierUsername(
                        cashier.getUsername()
                )
                .failureCode(null)
                .failureMessage(null)
                .createdAt(
                        LocalDateTime.now()
                )
                .processedAt(null)
                .build();
    }

    /**
     * Valida la estructura básica de la solicitud.
     */
    private void validateRegisterRequest(
            RegisterLaboratoryPaymentRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del pago son obligatorios."
            );
        }

        if (
                request.getLaboratoryOrderId() == null
        ) {
            throw new RuntimeException(
                    "Debe indicar la orden de laboratorio."
            );
        }

        if (
                request.getPaymentMethod() == null ||
                        request.getPaymentMethod().isBlank()
        ) {
            throw new RuntimeException(
                    "Debe seleccionar un método de pago."
            );
        }

        if (
                request.getIdempotencyKey() == null
        ) {
            throw new RuntimeException(
                    "La llave de idempotencia es obligatoria."
            );
        }
    }

    /**
     * Valida al cajero autenticado.
     *
     * La identidad proviene del JWT y no del request.
     */
    private User validateCashier(
            String cashierUsername
    ) {
        if (
                cashierUsername == null ||
                        cashierUsername.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al cajero autenticado."
            );
        }

        User cashier =
                userRepository
                        .findByUsername(
                                cashierUsername
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cajero autenticado no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        cashier.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del cajero se encuentra inactiva."
            );
        }

        String roleName =
                cashier.getRole() != null
                        ? cashier
                        .getRole()
                        .getName()
                        : null;

        if (
                roleName == null ||
                        !CASHIER_ROLE.equals(
                                roleName.trim()
                                        .toUpperCase(
                                                Locale.ROOT
                                        )
                        )
        ) {
            throw new RuntimeException(
                    "Solamente un cajero puede realizar " +
                            "el cobro de laboratorio."
            );
        }

        return cashier;
    }

    /**
     * Valida que la orden continúe pendiente.
     */
    private void validateOrderPending(LaboratoryOrder order) {
        if (!Boolean.TRUE.equals(order.getActive())) {
            throw new RuntimeException("La orden de laboratorio no se encuentra activa.");
        }
        /**
         * Las órdenes externas se procesan fuera del hospital
         * y no deben cobrarse en caja.
         */
        if (Boolean.TRUE.equals(order.getExternalOrder())
        ) {
            throw new RuntimeException("La orden de laboratorio está marcada como externa y no puede cobrarse en la caja del hospital.");
        }

        if (order.getStatus() != LaboratoryOrderStatus.PENDIENTE_DE_PAGO) {
            throw new RuntimeException("La orden de laboratorio no se encuentra pendiente de pago.");
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La orden de laboratorio no tiene un monto válido para cobrar.");
        }
    }

    /**
     * Impide registrar un segundo pago aprobado.
     */
    private void validateNoApprovedPayment(
            LaboratoryOrder order
    ) {
        boolean approvedPaymentExists =
                laboratoryPaymentRepository
                        .existsByLaboratoryOrder_IdAndStatus(
                                order.getId(),
                                PaymentStatus.APROBADO
                        );

        if (approvedPaymentExists) {
            throw new RuntimeException(
                    "La orden de laboratorio ya tiene " +
                            "un pago aprobado."
            );
        }
    }

    /**
     * Convierte y valida el método de pago.
     */
    private PaymentMethod parsePaymentMethod(
            String value
    ) {
        final PaymentMethod paymentMethod;

        try {
            paymentMethod =
                    PaymentMethod.valueOf(
                            value.trim()
                                    .toUpperCase(
                                            Locale.ROOT
                                    )
                    );

        } catch (
                IllegalArgumentException exception
        ) {
            throw new RuntimeException(
                    "El método de pago no es válido. " +
                            "Use EFECTIVO, VISA, MASTERCARD o DEBITO."
            );
        }

        if (
                paymentMethod ==
                        PaymentMethod.EN_LINEA
        ) {
            throw new RuntimeException(
                    "El método EN_LINEA no está permitido " +
                            "para el cobro de laboratorio en caja."
            );
        }

        return paymentMethod;
    }

    /**
     * Valida los campos requeridos para una tarjeta.
     */
    private void validateCardData(
            RegisterLaboratoryPaymentRequest request
    ) {
        if (
                request.getCardLastFour() == null ||
                        !request.getCardLastFour()
                                .matches("^\\d{4}$")
        ) {
            throw new RuntimeException(
                    "Ingrese exactamente los últimos 4 " +
                            "dígitos de la tarjeta."
            );
        }

        if (
                request.getPaymentToken() == null ||
                        request.getPaymentToken()
                                .isBlank()
        ) {
            throw new RuntimeException(
                    "El token de pago es obligatorio " +
                            "para pagos con tarjeta."
            );
        }
    }

    /**
     * Determina si el resultado negativo corresponde
     * a un error técnico y no a un rechazo bancario.
     */
    private boolean isTechnicalGatewayError(
            GatewayPaymentResult result
    ) {
        String failureCode =
                result.getFailureCode();

        return "PROCESSING_ERROR"
                .equalsIgnoreCase(
                        failureCode
                )
                ||
                "COMMUNICATION_ERROR"
                        .equalsIgnoreCase(
                                failureCode
                        );
    }

    /**
     * Valida que la orden pertenezca a la sucursal
     * asignada al cajero.
     */
    private void validateCashierBranch(
            LaboratoryOrder order,
            User cashier
    ) {
        if (cashier.getBranch() == null) {
            throw new RuntimeException(
                    "El cajero no tiene una sucursal asignada."
            );
        }

        if (
                order.getAppointment() == null ||
                        order.getAppointment()
                                .getBranch() == null
        ) {
            throw new RuntimeException(
                    "La orden no tiene una sucursal asociada."
            );
        }

        if (
                !cashier.getBranch()
                        .getId()
                        .equals(
                                order.getAppointment()
                                        .getBranch()
                                        .getId()
                        )
        ) {
            throw new RuntimeException(
                    "La orden no pertenece a la sucursal " +
                            "asignada al cajero."
            );
        }
    }

    /**
     * Se utiliza durante la búsqueda para excluir órdenes
     * de otras sucursales.
     */
    private boolean belongsToCashierBranch(
            LaboratoryOrder order,
            User cashier
    ) {
        return cashier.getBranch() != null
                && order.getAppointment() != null
                && order.getAppointment()
                .getBranch() != null
                && cashier.getBranch()
                .getId()
                .equals(
                        order.getAppointment()
                                .getBranch()
                                .getId()
                );
    }

    /**
     * Busca una orden activa sin bloquearla.
     *
     * Se utiliza para presentar el detalle, no para cobrar.
     */
    private LaboratoryOrder findActiveOrder(
            Long orderId
    ) {
        if (orderId == null) {
            throw new RuntimeException(
                    "Debe indicar la orden de laboratorio."
            );
        }

        return laboratoryOrderRepository
                .findByIdAndActiveTrue(
                        orderId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Orden de laboratorio no encontrada."
                        )
                );
    }

    /**
     * Valida el formato completo del DPI.
     */
    private void validateDpi(
            String dpi
    ) {
        if (!dpi.matches("^\\d{13}$")) {
            throw new RuntimeException(
                    INVALID_DPI_MESSAGE
            );
        }
    }

    /**
     * Normaliza un texto obligatorio.
     */
    private String normalizeRequiredText(
            String value,
            String errorMessage
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    errorMessage
            );
        }

        return value.trim();
    }

    /**
     * Normaliza un valor monetario a dos decimales.
     */
    private BigDecimal normalizeMoney(
            BigDecimal amount
    ) {
        if (amount == null) {
            return BigDecimal.ZERO
                    .setScale(2);
        }

        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    /**
     * Da formato monetario utilizado en los mensajes.
     */
    private String formatMoney(
            BigDecimal amount
    ) {
        return normalizeMoney(
                amount
        ).toPlainString();
    }

    /**
     * Genera un número de transacción para pagos
     * en efectivo.
     *
     * Los pagos con tarjeta utilizan el número generado
     * por la pasarela.
     */
    private String generateTransactionNumber() {
        String date =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter
                                        .ofPattern(
                                                "yyyyMMddHHmmss"
                                        )
                        );

        String suffix =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8)
                        .toUpperCase(
                                Locale.ROOT
                        );

        return "LAB-" +
                date +
                "-" +
                suffix;
    }

    /**
     * Construye la fila mostrada en la búsqueda.
     */
    private LaboratoryOrderPaymentSearchResponse
    toSearchResponse(
            LaboratoryOrder order
    ) {
        int totalTests =
                laboratoryOrderItemRepository
                        .findByLaboratoryOrder_IdOrderByIdAsc(
                                order.getId()
                        )
                        .size();

        return LaboratoryOrderPaymentSearchResponse
                .builder()
                .laboratoryOrderId(
                        order.getId()
                )
                .orderNumber(
                        order.getOrderNumber()
                )
                .patientName(
                        order.getPatient()
                                .getFullName()
                )
                .patientDpi(
                        order.getPatient()
                                .getDpi()
                )
                .doctorName(
                        order.getDoctor()
                                .getFullName()
                )
                .branch(
                        order.getAppointment()
                                .getBranch()
                                .getName()
                )
                .totalTests(
                        totalTests
                )
                .totalAmount(
                        normalizeMoney(
                                order.getTotalAmount()
                        )
                )
                .currency(CURRENCY)
                .status(
                        order.getStatus()
                                .name()
                )
                .externalOrder(
                        order.getExternalOrder()
                )
                .createdAt(
                        order.getCreatedAt()
                                .toString()
                )
                .canPay(
                        order.getStatus() ==
                                LaboratoryOrderStatus
                                        .PENDIENTE_DE_PAGO
                                &&
                                Boolean.TRUE.equals(
                                        order.getActive()
                                )
                )
                .message(
                        "Orden pendiente de pago."
                )
                .build();
    }

    /**
     * Construye el elemento que aparecerá en el detalle
     * de la orden y posteriormente en el comprobante.
     *
     * Los precios se toman de unitPrice, que conserva el
     * valor histórico de la orden.
     */
    private LaboratoryOrderItemResponse
    toItemResponse(
            LaboratoryOrderItem item
    ) {
        return LaboratoryOrderItemResponse
                .builder()
                .itemId(
                        item.getId()
                )
                .laboratoryTestId(
                        item.getLaboratoryTest()
                                .getId()
                )
                .testCode(
                        item.getLaboratoryTest()
                                .getCode()
                )
                .testName(
                        item.getLaboratoryTest()
                                .getName()
                )
                .referenceRange(
                        item.getLaboratoryTest()
                                .getReferenceRange()
                )
                .unitPrice(
                        normalizeMoney(
                                item.getUnitPrice()
                        )
                )
                .status(
                        item.getStatus()
                                .name()
                )
                .published(
                        item.getPublished()
                )
                .canSaveResult(false)
                .canPublishResult(false)
                .build();
    }

    /**
     * Construye el comprobante del pago.
     */
    private LaboratoryPaymentResponse toPaymentResponse(LaboratoryPayment payment, String message) {
        LaboratoryOrder order =
                payment.getLaboratoryOrder();

        /*
         * Recupera todos los exámenes asociados a la orden
         * de laboratorio y los convierte a DTO.
         *
         * Esta información se utilizará en el comprobante.
         */
        List<LaboratoryOrderItemResponse> items =
                laboratoryOrderItemRepository
                        .findByLaboratoryOrder_IdOrderByIdAsc(
                                order.getId()
                        )
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        int totalTests =
                laboratoryOrderItemRepository
                        .findByLaboratoryOrder_IdOrderByIdAsc(
                                order.getId()
                        )
                        .size();

        return LaboratoryPaymentResponse
                .builder()
                .laboratoryPaymentId(
                        payment.getId()
                )
                .laboratoryOrderId(
                        order.getId()
                )
                .orderNumber(
                        order.getOrderNumber()
                )
                .patientName(
                        order.getPatient()
                                .getFullName()
                )
                .patientDpi(
                        order.getPatient()
                                .getDpi()
                )
                .doctorName(
                        order.getDoctor()
                                .getFullName()
                )
                .branch(
                        order.getAppointment()
                                .getBranch()
                                .getName()
                )
                .totalTests(
                        totalTests
                )
                .amount(
                        normalizeMoney(
                                payment.getAmount()
                        )
                )
                .currency(
                        payment.getCurrency()
                )
                .paymentMethod(
                        payment.getPaymentMethod()
                                .name()
                )
                .paymentStatus(
                        payment.getStatus()
                                .name()
                )
                .receivedAmount(
                        payment.getReceivedAmount()
                )
                .changeAmount(
                        payment.getChangeAmount()
                )
                .cardLastFour(
                        payment.getCardLastFour()
                )
                .transactionNumber(
                        payment.getTransactionNumber()
                )
                .cashierUsername(
                        payment.getCashierUsername()
                )
                .processedAt(
                        payment.getProcessedAt() != null
                                ? payment
                                .getProcessedAt()
                                .toString()
                                : null
                )
                .laboratoryOrderStatus(
                        order.getStatus()
                                .name()
                )
                .message(message)
                .build();
    }
}