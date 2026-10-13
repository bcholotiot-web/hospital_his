package com.hospital.his.pharmacy.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.medications.entity.Medication;
import com.hospital.his.medications.repository.MedicationRepository;
import com.hospital.his.pharmacy.dto.AlternativeMedicationResponse;
import com.hospital.his.pharmacy.dto.PrescriptionDispensationDetailResponse;
import com.hospital.his.pharmacy.dto.PrescriptionDispensationItemResponse;
import com.hospital.his.pharmacy.dto.PrescriptionDispensationSearchResponse;
import com.hospital.his.pharmacy.entity.PharmacyInventory;
import com.hospital.his.pharmacy.entity.InventoryMovementType;
import com.hospital.his.pharmacy.repository.MedicationDispensationItemRepository;
import com.hospital.his.pharmacy.repository.MedicationDispensationRepository;
import com.hospital.his.pharmacy.repository.PharmacyInventoryRepository;
import com.hospital.his.prescriptions.entity.Prescription;
import com.hospital.his.prescriptions.entity.PrescriptionItem;
import com.hospital.his.prescriptions.entity.PrescriptionStatus;
import com.hospital.his.prescriptions.repository.PrescriptionItemRepository;
import com.hospital.his.prescriptions.repository.PrescriptionRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import com.hospital.his.payments.entity.PaymentMethod;
import com.hospital.his.payments.entity.PaymentStatus;
import com.hospital.his.payments.gateway.GatewayPaymentResult;
import com.hospital.his.payments.gateway.PaymentGateway;
import com.hospital.his.pharmacy.dto.ConfirmDispensationRequest;
import com.hospital.his.pharmacy.dto.DispensationItemRequest;
import com.hospital.his.pharmacy.dto.MedicationDispensationItemResponse;
import com.hospital.his.pharmacy.dto.MedicationDispensationResponse;
import com.hospital.his.pharmacy.entity.DispensationItemStatus;
import com.hospital.his.pharmacy.entity.DispensationStatus;
import com.hospital.his.pharmacy.entity.MedicationDispensation;
import com.hospital.his.pharmacy.entity.MedicationDispensationItem;
import com.hospital.his.pharmacy.dto.DeclinePrescriptionRequest;
import com.hospital.his.pharmacy.dto.PrescriptionDeclineResponse;
import com.hospital.his.pharmacy.entity.PrescriptionDeclineEvent;
import com.hospital.his.pharmacy.repository.PrescriptionDeclineEventRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class MedicationDispensationService {

    /**
     * Una receta puede utilizarse durante el día
     * de emisión y los siguientes siete días.
     */
    private static final long MAXIMUM_VALIDITY_DAYS =
            7L;

    private static final String PHARMACY_ROLE =
            "FARMACIA";

    private final PrescriptionRepository
            prescriptionRepository;

    private final PrescriptionItemRepository
            prescriptionItemRepository;

    private final PharmacyInventoryRepository
            pharmacyInventoryRepository;

    private final MedicationDispensationRepository
            medicationDispensationRepository;

    private final MedicationDispensationItemRepository
            medicationDispensationItemRepository;

    private final UserRepository userRepository;

    private final AuditService auditService;

    /**
     * Registra cada descuento de inventario producido
     * durante un despacho confirmado.
     */
    private final InventoryMovementService
            inventoryMovementService;

    /**
     * Código monetario utilizado en Farmacia.
     */
    private static final String CURRENCY =
            "GTQ";

    /**
     * Pasarela utilizada para pagos con tarjeta.
     *
     * Actualmente Spring inyectará la implementación
     * SimulatedPaymentGateway.
     */
    private final PaymentGateway paymentGateway;

    private final MedicationRepository medicationRepository;

    //Conserva los eventos en donde el paciente decidio adquirir los medicamentos.
    private final PrescriptionDeclineEventRepository prescriptionDeclineEventRepository;

    public MedicationDispensationService(
            PrescriptionRepository prescriptionRepository,
            PrescriptionItemRepository prescriptionItemRepository,
            PharmacyInventoryRepository pharmacyInventoryRepository,
            MedicationDispensationRepository
                    medicationDispensationRepository,
            MedicationDispensationItemRepository
                    medicationDispensationItemRepository,
            UserRepository userRepository,
            AuditService auditService, InventoryMovementService inventoryMovementService,
            PaymentGateway paymentGateway,
            MedicationRepository medicationRepository, PrescriptionDeclineEventRepository prescriptionDeclineEventRepository
    ) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.pharmacyInventoryRepository = pharmacyInventoryRepository;
        this.medicationDispensationRepository = medicationDispensationRepository;
        this.medicationDispensationItemRepository = medicationDispensationItemRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.inventoryMovementService = inventoryMovementService;
        this.paymentGateway = paymentGateway;
        this.medicationRepository = medicationRepository;
        this.prescriptionDeclineEventRepository = prescriptionDeclineEventRepository;
    }

    /**
     * Busca una receta activa para despacho.
     *
     * Criterios permitidos:
     *
     * PRESCRIPTION_NUMBER:
     * busca por el número visible de la receta.
     *
     * CONSULTATION_ID:
     * busca por el ID de la consulta médica.
     *
     * Aunque se encuentre la receta, la respuesta indica
     * si sigue vigente y si tiene cantidades pendientes.
     */
    @Transactional(readOnly = true)
    public List<PrescriptionDispensationSearchResponse> searchPrescriptions(String criteria, String value, String pharmacyUsername) {
        /*
         * Valida al usuario y su sucursal.
         */
        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        String normalizedCriteria =
                normalizeRequiredText(
                        criteria,
                        50,
                        "Debe seleccionar un criterio de búsqueda.",
                        "El criterio de búsqueda no es válido."
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        String normalizedValue =
                normalizeRequiredText(
                        value,
                        100,
                        "Debe ingresar un valor de búsqueda.",
                        "El valor de búsqueda no puede exceder "
                                + "los 100 caracteres."
                );

        Prescription prescription;

        switch (normalizedCriteria) {
            case "PRESCRIPTION_NUMBER":
            case "PRESCRIPTION":
            case "RECETA":
                prescription =
                        prescriptionRepository
                                .findByPrescriptionNumberIgnoreCaseAndActiveTrue(
                                        normalizedValue
                                )
                                .orElse(null);
                break;

            case "CONSULTATION_ID":
            case "CONSULTATION":
            case "CONSULTA":
                Long consultationId =
                        parsePositiveLong(
                                normalizedValue,
                                "El identificador de la consulta "
                                        + "debe ser un número válido."
                        );

                prescription =
                        prescriptionRepository
                                .findByMedicalConsultation_IdAndActiveTrue(
                                        consultationId
                                )
                                .orElse(null);
                break;

            default:
                throw new RuntimeException(
                        "El criterio de búsqueda no es válido. "
                                + "Use PRESCRIPTION_NUMBER "
                                + "o CONSULTATION_ID."
                );
        }

        /*
         * Una búsqueda sin coincidencias devuelve una lista
         * vacía. El frontend mostrará el mensaje correspondiente.
         */
        if (prescription == null) {
            return Collections.emptyList();
        }

        /*
         * Solo se presentan recetas correspondientes
         * a la sucursal del usuario de Farmacia.
         */
        validatePrescriptionBranch(
                prescription,
                pharmacyUser
        );

        return List.of(
                toSearchResponse(
                        prescription
                )
        );
    }

    /**
     * Obtiene el detalle de una receta y consulta
     * las existencias de cada medicamento en la
     * sucursal del usuario autenticado.
     *
     * Este método no modifica inventario.
     */
    @Transactional(readOnly = true)
    public PrescriptionDispensationDetailResponse getPrescriptionDetail(Long prescriptionId, String pharmacyUsername) {
        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        Prescription prescription =
                findActivePrescription(
                        prescriptionId
                );

        validatePrescriptionBranch(
                prescription,
                pharmacyUser
        );

        /*
         * Calcula la vigencia, pero no modifica
         * el estado almacenado.
         */
        long elapsedDays =
                calculateElapsedDays(
                        prescription
                );

        boolean valid =
                isPrescriptionValid(
                        prescription,
                        elapsedDays
                );

        List<PrescriptionItem> prescriptionItems =
                prescriptionItemRepository
                        .findByPrescription_IdAndActiveTrueOrderByIdAsc(
                                prescription.getId()
                        );

        if (prescriptionItems.isEmpty()) {
            throw new RuntimeException(
                    "La receta no contiene medicamentos activos."
            );
        }

        Long branchId =
                pharmacyUser
                        .getBranch()
                        .getId();

        List<PrescriptionDispensationItemResponse>
                itemResponses =
                prescriptionItems.stream()
                        .map(item ->
                                toDispensationItemResponse(
                                        item,
                                        branchId
                                )
                        )
                        .toList();

        int pendingMedications =
                countPendingMedications(
                        prescriptionItems
                );

        boolean canDispense =
                valid
                        && isDispensableStatus(
                        prescription.getStatus()
                )
                        && pendingMedications > 0;

        long remainingValidityDays =
                valid
                        ? Math.max(
                        MAXIMUM_VALIDITY_DAYS
                        - elapsedDays,
                        0
                )
                        : 0;

        String message =
                buildPrescriptionDetailMessage(
                        prescription,
                        valid,
                        pendingMedications
                );

        return PrescriptionDispensationDetailResponse
                .builder()
                .prescriptionId(
                        prescription.getId()
                )
                .prescriptionNumber(
                        prescription
                                .getPrescriptionNumber()
                )
                .medicalConsultationId(
                        prescription
                                .getMedicalConsultation()
                                .getId()
                )
                .patientName(
                        prescription
                                .getPatient()
                                .getFullName()
                )
                .patientDpi(
                        prescription
                                .getPatient()
                                .getDpi()
                )
                .doctorName(
                        prescription
                                .getDoctor()
                                .getFullName()
                )
                .prescriptionStatus(
                        prescription
                                .getStatus()
                                .name()
                )
                .issuedAt(
                        prescription
                                .getIssuedAt()
                                .toString()
                )
                .elapsedDays(
                        elapsedDays
                )
                .remainingValidityDays(
                        remainingValidityDays
                )
                .valid(valid)
                .pharmacyBranch(
                        pharmacyUser
                                .getBranch()
                                .getName()
                )
                .totalMedications(
                        prescriptionItems.size()
                )
                .pendingMedications(
                        pendingMedications
                )
                .canDispense(
                        canDispense
                )
                .items(
                        itemResponses
                )
                .message(message)
                .build();
    }

    /**
     * Registra que el paciente decidió no adquirir
     * los medicamentos durante esta visita.
     *
     * Esta operación:
     *
     * 1. Valida al usuario de Farmacia.
     * 2. Bloquea la receta durante el registro.
     * 3. Valida que la receta corresponda a la sucursal.
     * 4. Valida que la receta continúe vigente.
     * 5. Guarda el motivo y la persona que lo registró.
     * 6. Registra auditoría.
     *
     * Esta operación NO:
     *
     * - Descuenta inventario.
     * - Procesa pagos.
     * - Cambia cantidades despachadas.
     * - Cancela automáticamente la receta.
     *
     * La receta podrá ser atendida posteriormente mientras
     * continúe vigente y tenga cantidades pendientes.
     */
    @Transactional
    public PrescriptionDeclineResponse declinePrescription(Long prescriptionId, DeclinePrescriptionRequest request, String pharmacyUsername) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del registro son obligatorios."
            );
        }

        String reason =
                normalizeRequiredText(
                        request.getReason(),
                        500,
                        "Debe indicar el motivo por el cual "
                                + "el paciente no adquirirá "
                                + "los medicamentos.",
                        "El motivo no puede exceder "
                                + "los 500 caracteres."
                );

        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        /*
         * Bloquear la receta mientras se registra
         * el evento.
         */
        Prescription prescription =
                prescriptionRepository
                        .findForDispensation(
                                prescriptionId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Receta médica no encontrada."
                                )
                        );

        validatePrescriptionBranch(
                prescription,
                pharmacyUser
        );

        /*
         * La decisión solo se registra sobre recetas
         * que todavía podrían ser atendidas.
         */
        validatePrescriptionForDispensation(
                prescription
        );

        PrescriptionDeclineEvent event =
                PrescriptionDeclineEvent
                        .builder()
                        .prescription(
                                prescription
                        )
                        .branch(
                                pharmacyUser.getBranch()
                        )
                        .reason(reason)
                        .pharmacyUsername(
                                pharmacyUser.getUsername()
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        event =
                prescriptionDeclineEventRepository
                        .saveAndFlush(event);

        auditService.log(
                pharmacyUser.getUsername(),
                "PATIENT_DECLINED_MEDICATIONS",
                "PHARMACY",
                "El paciente decidió no adquirir "
                        + "los medicamentos de la receta "
                        + prescription
                        .getPrescriptionNumber()
                        + ". Motivo: "
                        + reason
                        + "."
        );

        return PrescriptionDeclineResponse
                .builder()
                .eventId(
                        event.getId()
                )
                .prescriptionId(
                        prescription.getId()
                )
                .prescriptionNumber(
                        prescription
                                .getPrescriptionNumber()
                )
                .patientName(
                        prescription
                                .getPatient()
                                .getFullName()
                )
                .branch(
                        pharmacyUser
                                .getBranch()
                                .getName()
                )
                .reason(
                        event.getReason()
                )
                .pharmacyUsername(
                        event.getPharmacyUsername()
                )
                .createdAt(
                        event.getCreatedAt()
                                .toString()
                )
                .prescriptionStatus(
                        prescription
                                .getStatus()
                                .name()
                )
                .message(
                        "Se registró que el paciente no "
                                + "adquirió los medicamentos. "
                                + "La receta permanece disponible "
                                + "mientras continúe vigente."
                )
                .build();
    }


    /**
     * Confirma el despacho y el cobro de medicamentos.
     *
     * Flujo:
     *
     * 1. Valida el request.
     * 2. Valida al usuario de Farmacia.
     * 3. Controla idempotencia.
     * 4. Bloquea la receta.
     * 5. Valida vigencia y estado.
     * 6. Valida los elementos seleccionados.
     * 7. Bloquea los inventarios.
     * 8. Calcula precios y total.
     * 9. Procesa el método de pago.
     * 10. Descuenta existencias.
     * 11. Actualiza cantidades de la receta.
     * 12. Actualiza el estado de la receta.
     * 13. Guarda el despacho.
     * 14. Registra auditoría.
     *
     * Si ocurre una excepción no controlada, la transacción
     * revierte el inventario, la receta y el despacho.
     */
    /**
     * Confirma el despacho y procesa el cobro.
     *
     * La operación se divide en dos fases:
     *
     * Fase 1:
     * 1. Validar receta, medicamentos e inventario.
     * 2. Bloquear las filas de inventario.
     * 3. Calcular precios y total.
     * 4. No modificar todavía inventario ni receta.
     *
     * Fase 2:
     * 1. Procesar el cobro.
     * 2. Si se aprueba, descontar inventario.
     * 3. Actualizar cantidades despachadas.
     * 4. Crear el detalle del despacho.
     * 5. Actualizar el estado de la receta.
     *
     * Si el pago es rechazado, únicamente se registra
     * el intento. No se modifica el inventario ni la receta.
     */
    @Transactional
    public MedicationDispensationResponse confirmDispensation(ConfirmDispensationRequest request, String pharmacyUsername) {
        /*
         * Validar la estructura general del request.
         */
        validateDispensationRequest(
                request
        );

        /*
         * Identificar al usuario autenticado.
         *
         * También valida:
         * - Cuenta activa.
         * - Rol FARMACIA.
         * - Rol activo.
         * - Sucursal activa.
         */
        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        /*
         * Idempotencia:
         * si la llave ya fue utilizada, se devuelve
         * exactamente el resultado anterior.
         */
        MedicationDispensation existingDispensation =
                medicationDispensationRepository
                        .findByIdempotencyKey(
                                request.getIdempotencyKey()
                        )
                        .orElse(null);

        if (existingDispensation != null) {
            /*
             * Una llave utilizada para una receta no puede
             * reutilizarse para otra receta diferente.
             */
            if (
                    !existingDispensation
                            .getPrescription()
                            .getId()
                            .equals(
                                    request
                                            .getPrescriptionId()
                            )
            ) {
                throw new RuntimeException(
                        "La llave de idempotencia ya fue utilizada "
                                + "para otra receta."
                );
            }

            return toDispensationResponse(
                    existingDispensation,
                    "La solicitud ya fue procesada anteriormente."
            );
        }

        /*
         * Obtener la receta con bloqueo de escritura.
         *
         * Esto evita que dos usuarios despachen
         * simultáneamente la misma receta.
         */
        Prescription prescription =
                prescriptionRepository
                        .findForDispensation(
                                request.getPrescriptionId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Receta médica no encontrada."
                                )
                        );

        /*
         * La receta debe pertenecer a la misma sucursal
         * del usuario de Farmacia.
         */
        validatePrescriptionBranch(
                prescription,
                pharmacyUser
        );

        /*
         * Validar:
         * - Vigencia máxima de siete días.
         * - Receta activa.
         * - Estado ACTIVA o PARCIALMENTE_DESPACHADA.
         */
        validatePrescriptionForDispensation(
                prescription
        );

        /*
         * Impedir enviar dos veces el mismo ítem
         * dentro de una sola operación.
         */
        validateNoDuplicateDispensationItems(
                request.getItems()
        );

        /*
         * Convertir y validar el método de pago.
         */
        PaymentMethod paymentMethod =
                parsePaymentMethod(
                        request.getPaymentMethod()
                );

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * Construir la cabecera del intento.
         *
         * Todavía no se guarda y todavía no se modifica
         * el inventario.
         */
        MedicationDispensation dispensation =
                MedicationDispensation.builder()
                        .dispensationNumber(
                                generateDispensationNumber()
                        )
                        .prescription(
                                prescription
                        )
                        .patient(
                                prescription.getPatient()
                        )
                        .branch(
                                pharmacyUser.getBranch()
                        )
                        .status(
                                DispensationStatus.PROCESANDO
                        )
                        .totalAmount(
                                BigDecimal.ZERO
                                        .setScale(2)
                        )
                        .currency(
                                CURRENCY
                        )
                        .paymentMethod(
                                paymentMethod
                        )
                        .paymentStatus(
                                PaymentStatus.PROCESANDO
                        )
                        .receivedAmount(null)
                        .changeAmount(null)
                        .cardLastFour(null)
                        .transactionNumber(null)
                        .gatewayReference(null)
                        .idempotencyKey(
                                request.getIdempotencyKey()
                        )
                        .pharmacyUsername(
                                pharmacyUser.getUsername()
                        )
                        .failureCode(null)
                        .failureMessage(null)
                        .createdAt(now)
                        .confirmedAt(null)
                        .items(
                                new ArrayList<>()
                        )
                        .build();

        /*
         * FASE 1:
         *
         * Validar y preparar todos los elementos sin
         * cambiar stock ni cantidades de la receta.
         */
        List<PreparedDispensationItem>
                preparedItems =
                prepareDispensationItemsWithoutMutation(
                        prescription,
                        request.getItems(),
                        pharmacyUser
                );

        /*
         * Calcular el total exclusivamente con precios
         * obtenidos desde el catálogo del backend.
         */
        BigDecimal totalAmount =
                calculatePreparedTotal(
                        preparedItems
                );

        dispensation.setTotalAmount(
                totalAmount
        );

        /*
         * Procesar el cobro después de conocer el total,
         * pero antes de descontar inventario.
         */
        PaymentProcessingResult paymentResult =
                processPayment(
                        paymentMethod,
                        totalAmount,
                        request
                );

        /*
         * Copiar el resultado del cobro a la cabecera.
         */
        applyPaymentResult(
                dispensation,
                paymentResult
        );

        /*
         * Si el pago fue rechazado o produjo un error:
         *
         * - Se guarda el intento.
         * - No se crean detalles.
         * - No se descuenta inventario.
         * - No se actualiza la receta.
         */
        if (!paymentResult.approved()) {
            dispensation.getItems().clear();

            try {
                dispensation =
                        medicationDispensationRepository
                                .saveAndFlush(
                                        dispensation
                                );

            } catch (
                    DataIntegrityViolationException exception
            ) {
                throw new RuntimeException(
                        "No fue posible registrar el intento de pago. "
                                + "La solicitud puede haber sido "
                                + "procesada anteriormente."
                );
            }

            auditService.log(
                    pharmacyUser.getUsername(),
                    paymentResult.paymentStatus()
                            == PaymentStatus.RECHAZADO
                            ? "MEDICATION_DISPENSATION_REJECTED"
                            : "MEDICATION_DISPENSATION_ERROR",
                    "PHARMACY",
                    "Despacho no confirmado para receta "
                            + prescription
                            .getPrescriptionNumber()
                            + ". Código: "
                            + paymentResult.failureCode()
                            + "."
            );

            return toDispensationResponse(
                    dispensation,
                    paymentResult.message()
            );
        }

        /*
         * FASE 2:
         *
         * El pago fue aprobado.
         *
         * Antes de descontar inventario, guardar la cabecera
         * para obtener el ID real del despacho.
         *
         * Este ID será utilizado como referenceId en
         * inventory_movements.
         *
         * La transacción continúa abierta. Si posteriormente
         * falla el inventario, el movimiento o la receta,
         * también se revierte esta inserción.
         */
        try {
            dispensation =
                    medicationDispensationRepository
                            .saveAndFlush(
                                    dispensation
                            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "No fue posible iniciar el despacho. "
                            + "La solicitud puede haber sido "
                            + "procesada anteriormente."
            );
        }

        /*
         * Aplicar los descuentos, actualizar la receta,
         * crear los detalles y registrar movimientos.
         */
        applyApprovedDispensationItems(
                dispensation,
                preparedItems,
                pharmacyUser
        );

        /*
         * Actualizar receta a:
         *
         * PARCIALMENTE_DESPACHADA
         * o
         * DESPACHADA
         */
        updatePrescriptionStatus(
                prescription
        );

        dispensation.setStatus(
                DispensationStatus.CONFIRMADO
        );

        dispensation.setConfirmedAt(
                LocalDateTime.now()
        );

        try {
            /*
             * Persistir la receta con sus cantidades
             * actualizadas.
             */
            prescriptionRepository
                    .saveAndFlush(
                            prescription
                    );

            /*
             * CascadeType.ALL guarda el despacho
             * y sus detalles.
             */
            dispensation =
                    medicationDispensationRepository
                            .saveAndFlush(
                                    dispensation
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "La receta o el inventario fueron actualizados "
                            + "por otra persona. Recargue la información "
                            + "e intente nuevamente."
            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "No fue posible confirmar el despacho. "
                            + "La solicitud puede haber sido "
                            + "procesada anteriormente."
            );
        }

        auditService.log(
                pharmacyUser.getUsername(),
                "CONFIRM_MEDICATION_DISPENSATION",
                "PHARMACY",
                "Despacho "
                        + dispensation
                        .getDispensationNumber()
                        + " confirmado para receta "
                        + prescription
                        .getPrescriptionNumber()
                        + ". Monto: Q"
                        + totalAmount
                        + ". Método: "
                        + paymentMethod.name()
                        + ". Medicamentos: "
                        + dispensation
                        .getItems()
                        .size()
                        + "."
        );

        return toDispensationResponse(
                dispensation,
                "Despacho de medicamentos registrado "
                        + "correctamente."
        );
    }

    /**
     * Recupera un comprobante validando que pertenezca
     * a la sucursal del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public MedicationDispensationResponse getReceipt(Long dispensationId, String pharmacyUsername) {
        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        MedicationDispensation dispensation =
                medicationDispensationRepository
                        .findById(dispensationId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Comprobante de despacho "
                                                + "no encontrado."
                                )
                        );

        if (
                !dispensation
                        .getBranch()
                        .getId()
                        .equals(
                                pharmacyUser
                                        .getBranch()
                                        .getId()
                        )
        ) {
            throw new RuntimeException(
                    "El comprobante no pertenece a "
                            + "la sucursal asignada."
            );
        }

        return toDispensationResponse(
                dispensation,
                "Comprobante encontrado."
        );
    }

    /**
     * Lista los despachos realizados por el usuario
     * autenticado.
     */
    @Transactional(readOnly = true)
    public List<MedicationDispensationResponse> getMyDispensations(String pharmacyUsername) {
        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        return medicationDispensationRepository
                .findByPharmacyUsernameOrderByCreatedAtDesc(
                        pharmacyUser.getUsername()
                )
                .stream()
                .map(dispensation ->
                        toDispensationResponse(
                                dispensation,
                                "Despacho registrado."
                        )
                )
                .toList();
    }

    /**
     * Valida al usuario autenticado de Farmacia.
     *
     * Las condiciones son:
     *
     * 1. Usuario existente.
     * 2. Cuenta activa.
     * 3. Rol activo.
     * 4. Rol FARMACIA.
     * 5. Sucursal activa asignada.
     */
    private User validatePharmacyUser(String pharmacyUsername) {
        if (
                pharmacyUsername == null ||
                        pharmacyUsername.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al usuario "
                            + "de farmacia autenticado."
            );
        }

        User pharmacyUser =
                userRepository
                        .findByUsername(
                                pharmacyUsername
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Usuario de farmacia no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        pharmacyUser.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del usuario de farmacia "
                            + "se encuentra inactiva."
            );
        }

        if (
                pharmacyUser.getRole() == null ||
                        !Boolean.TRUE.equals(
                                pharmacyUser
                                        .getRole()
                                        .getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El usuario no tiene un rol activo asignado."
            );
        }

        if (
                !PHARMACY_ROLE.equals(
                        normalizeRole(
                                pharmacyUser
                                        .getRole()
                                        .getName()
                        )
                )
        ) {
            throw new RuntimeException(
                    "Solamente el personal de farmacia "
                            + "puede realizar esta operación."
            );
        }

        if (
                pharmacyUser.getBranch() == null ||
                        !Boolean.TRUE.equals(
                                pharmacyUser
                                        .getBranch()
                                        .getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El usuario de farmacia no tiene "
                            + "una sucursal activa asignada."
            );
        }

        return pharmacyUser;
    }

    /**
     * Busca una receta activa por ID.
     */
    private Prescription findActivePrescription(Long prescriptionId) {
        if (prescriptionId == null) {
            throw new RuntimeException(
                    "Debe indicar la receta médica."
            );
        }

        return prescriptionRepository
                .findByIdAndActiveTrue(
                        prescriptionId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Receta médica no encontrada."
                        )
                );
    }

    /**
     * Impide que un usuario de Farmacia consulte
     * recetas de otra sucursal.
     */
    private void validatePrescriptionBranch(Prescription prescription, User pharmacyUser) {
        if (
                prescription.getAppointment() == null ||
                        prescription
                                .getAppointment()
                                .getBranch() == null
        ) {
            throw new RuntimeException(
                    "La receta no tiene una sucursal asociada."
            );
        }

        Long prescriptionBranchId =
                prescription
                        .getAppointment()
                        .getBranch()
                        .getId();

        Long pharmacyBranchId =
                pharmacyUser
                        .getBranch()
                        .getId();

        if (
                !pharmacyBranchId.equals(
                        prescriptionBranchId
                )
        ) {
            throw new RuntimeException(
                    "La receta no pertenece a la sucursal "
                            + "asignada al usuario de farmacia."
            );
        }
    }

    /**
     * Calcula los días transcurridos desde la emisión.
     *
     * El día de emisión se considera día cero.
     */
    private long calculateElapsedDays(Prescription prescription) {
        if (prescription.getIssuedAt() == null) {
            throw new RuntimeException(
                    "La receta no tiene una fecha "
                            + "de emisión válida."
            );
        }

        LocalDate issuedDate =
                prescription
                        .getIssuedAt()
                        .toLocalDate();

        LocalDate currentDate =
                LocalDate.now();

        long elapsedDays =
                ChronoUnit.DAYS.between(
                        issuedDate,
                        currentDate
                );

        /*
         * Una fecha futura representa información
         * inconsistente.
         */
        if (elapsedDays < 0) {
            throw new RuntimeException(
                    "La fecha de emisión de la receta "
                            + "no puede encontrarse en el futuro."
            );
        }

        return elapsedDays;
    }

    /**
     * Determina si la receta está dentro de sus
     * siete días y tiene un estado utilizable.
     */
    private boolean isPrescriptionValid(Prescription prescription, long elapsedDays) {
        return elapsedDays
                <= MAXIMUM_VALIDITY_DAYS
                && prescription.getStatus()
                != PrescriptionStatus.CANCELADA
                && prescription.getStatus()
                != PrescriptionStatus.DESPACHADA
                && Boolean.TRUE.equals(
                prescription.getActive()
        );
    }

    /**
     * Determina si el estado permite realizar
     * un despacho completo o parcial.
     */
    private boolean isDispensableStatus(
            PrescriptionStatus status
    ) {
        return status
                == PrescriptionStatus.ACTIVA
                ||
                status
                        == PrescriptionStatus
                        .PARCIALMENTE_DESPACHADA;
    }

    /**
     * Cuenta cuántos medicamentos todavía tienen
     * unidades pendientes.
     */
    private int countPendingMedications(
            List<PrescriptionItem> items
    ) {
        return (int) items.stream()
                .filter(item ->
                        calculatePendingQuantity(
                                item
                        ) > 0
                )
                .count();
    }

    /**
     * Calcula la cantidad que aún puede entregarse.
     */
    private int calculatePendingQuantity(
            PrescriptionItem item
    ) {
        int prescribedQuantity =
                item.getPrescribedQuantity() != null
                        ? item.getPrescribedQuantity()
                        : 0;

        int dispensedQuantity =
                item.getDispensedQuantity() != null
                        ? item.getDispensedQuantity()
                        : 0;

        return Math.max(
                prescribedQuantity
                        - dispensedQuantity,
                0
        );
    }

    /**
     * Construye la información de un medicamento
     * recetado junto con el inventario de la sucursal.
     */
    private PrescriptionDispensationItemResponse toDispensationItemResponse(PrescriptionItem prescriptionItem, Long branchId) {
        Medication medication =
                prescriptionItem
                        .getMedication();

        if (medication == null) {
            throw new RuntimeException(
                    "La receta contiene un medicamento inválido."
            );
        }

        int pendingQuantity =
                calculatePendingQuantity(
                        prescriptionItem
                );

        PharmacyInventory inventory =
                pharmacyInventoryRepository
                        .findByBranch_IdAndMedication_IdAndActiveTrue(
                                branchId,
                                medication.getId()
                        )
                        .orElse(null);

        int availableStock =
                inventory != null
                        ? inventory.getCurrentStock()
                        : 0;

        int minimumStock =
                inventory != null
                        ? inventory.getMinimumStock()
                        : 0;

        boolean available =
                inventory != null
                        && Boolean.TRUE.equals(
                        inventory.getActive()
                )
                        && availableStock > 0;

        boolean enoughForFullDispensation =
                available
                        && availableStock
                        >= pendingQuantity;

        boolean lowStock =
                inventory != null
                        && availableStock
                        <= minimumStock;

        return PrescriptionDispensationItemResponse
                .builder()
                .prescriptionItemId(
                        prescriptionItem.getId()
                )
                .originalMedicationId(
                        medication.getId()
                )
                .medicationCode(
                        medication.getCode()
                )
                .medicationName(
                        medication.getName()
                )
                .activeIngredient(
                        medication
                                .getActiveIngredient()
                )
                .concentration(
                        medication
                                .getConcentration()
                )
                .presentation(
                        medication
                                .getPresentation()
                )
                .dosage(
                        prescriptionItem
                                .getDosage()
                )
                .frequency(
                        prescriptionItem
                                .getFrequency()
                )
                .duration(
                        prescriptionItem
                                .getDuration()
                )
                .specialInstructions(
                        prescriptionItem
                                .getSpecialInstructions()
                )
                .prescribedQuantity(
                        prescriptionItem
                                .getPrescribedQuantity()
                )
                .alreadyDispensedQuantity(
                        prescriptionItem
                                .getDispensedQuantity()
                )
                .pendingQuantity(
                        pendingQuantity
                )
                .controlledMedication(
                        prescriptionItem
                                .getControlledMedication()
                )
                .currentUnitPrice(
                        medication.getUnitPrice()
                )
                .availableStock(
                        availableStock
                )
                .minimumStock(
                        minimumStock
                )
                .available(
                        available
                )
                .enoughForFullDispensation(
                        enoughForFullDispensation
                )
                .lowStock(
                        lowStock
                )
                .alternatives(
                        findAvailableAlternatives(
                                medication,
                                branchId
                        )
                )
                .message(
                        buildItemAvailabilityMessage(
                                medication,
                                inventory,
                                pendingQuantity
                        )
                )
                .build();
    }

    /**
     * Busca alternativas activas con el mismo
     * principio activo y con existencias en sucursal.
     *
     * El medicamento original se excluye.
     *
     * El sistema únicamente propone opciones técnicas.
     * No selecciona automáticamente una sustitución.
     */
    private List<AlternativeMedicationResponse> findAvailableAlternatives(Medication originalMedication, Long branchId) {
        String activeIngredient =
                originalMedication
                        .getActiveIngredient();

        if (
                activeIngredient == null ||
                        activeIngredient.isBlank()
        ) {
            return Collections.emptyList();
        }

        return pharmacyInventoryRepository
                .findByBranch_IdAndActiveTrueOrderByMedication_NameAsc(
                        branchId
                )
                .stream()
                .filter(inventory ->
                        inventory.getMedication() != null
                )
                .filter(inventory ->
                        !inventory
                                .getMedication()
                                .getId()
                                .equals(
                                        originalMedication
                                                .getId()
                                )
                )
                .filter(inventory ->
                        Boolean.TRUE.equals(
                                inventory
                                        .getMedication()
                                        .getActive()
                        )
                )
                .filter(inventory ->
                        inventory
                                .getMedication()
                                .getActiveIngredient()
                                != null
                )
                .filter(inventory ->
                        activeIngredient.equalsIgnoreCase(
                                inventory
                                        .getMedication()
                                        .getActiveIngredient()
                                        .trim()
                        )
                )
                .filter(inventory ->
                        inventory.getCurrentStock() > 0
                )
                .map(this::toAlternativeResponse)
                .toList();
    }

    /**
     * Convierte una existencia alternativa en DTO.
     */
    private AlternativeMedicationResponse toAlternativeResponse(PharmacyInventory inventory) {
        Medication medication =
                inventory.getMedication();

        return AlternativeMedicationResponse
                .builder()
                .medicationId(
                        medication.getId()
                )
                .code(
                        medication.getCode()
                )
                .name(
                        medication.getName()
                )
                .activeIngredient(
                        medication
                                .getActiveIngredient()
                )
                .concentration(
                        medication.getConcentration()
                )
                .presentation(
                        medication.getPresentation()
                )
                .unitPrice(
                        medication.getUnitPrice()
                )
                .availableStock(
                        inventory.getCurrentStock()
                )
                .controlled(
                        medication.getControlled()
                )
                .lowStock(
                        inventory.getCurrentStock()
                                <= inventory
                                .getMinimumStock()
                )
                .build();
    }

    /**
     * Construye el mensaje de disponibilidad
     * para un medicamento recetado.
     */
    private String buildItemAvailabilityMessage(Medication medication, PharmacyInventory inventory, int pendingQuantity) {
        if (pendingQuantity <= 0) {
            return "El medicamento ya fue despachado completamente.";
        }

        if (
                inventory == null ||
                        inventory.getCurrentStock() <= 0
        ) {
            return "El medicamento "
                    + medication.getName()
                    + " no tiene existencias en esta sucursal.";
        }

        if (
                inventory.getCurrentStock()
                        < pendingQuantity
        ) {
            return "Existencia parcial. Disponibles: "
                    + inventory.getCurrentStock()
                    + " unidades. Pendientes: "
                    + pendingQuantity
                    + " unidades.";
        }

        if (
                inventory.getCurrentStock()
                        <= inventory.getMinimumStock()
        ) {
            return "Medicamento disponible, pero se encuentra "
                    + "en nivel de stock mínimo.";
        }

        return "Medicamento disponible para despacho.";
    }

    /**
     * Construye la fila de resultado de búsqueda.
     */
    private PrescriptionDispensationSearchResponse toSearchResponse(Prescription prescription) {
        List<PrescriptionItem> items =
                prescriptionItemRepository
                        .findByPrescription_IdAndActiveTrueOrderByIdAsc(
                                prescription.getId()
                        );

        long elapsedDays =
                calculateElapsedDays(
                        prescription
                );

        boolean valid =
                isPrescriptionValid(
                        prescription,
                        elapsedDays
                );

        int pendingMedications =
                countPendingMedications(
                        items
                );

        boolean canDispense =
                valid
                        && isDispensableStatus(
                        prescription.getStatus()
                )
                        && pendingMedications > 0;

        String message =
                buildPrescriptionDetailMessage(
                        prescription,
                        valid,
                        pendingMedications
                );

        return PrescriptionDispensationSearchResponse
                .builder()
                .prescriptionId(
                        prescription.getId()
                )
                .prescriptionNumber(
                        prescription
                                .getPrescriptionNumber()
                )
                .medicalConsultationId(
                        prescription
                                .getMedicalConsultation()
                                .getId()
                )
                .patientName(
                        prescription
                                .getPatient()
                                .getFullName()
                )
                .patientDpi(
                        prescription
                                .getPatient()
                                .getDpi()
                )
                .doctorName(
                        prescription
                                .getDoctor()
                                .getFullName()
                )
                .branch(
                        prescription
                                .getAppointment()
                                .getBranch()
                                .getName()
                )
                .status(
                        prescription
                                .getStatus()
                                .name()
                )
                .issuedAt(
                        prescription
                                .getIssuedAt()
                                .toString()
                )
                .elapsedDays(
                        elapsedDays
                )
                .valid(valid)
                .totalMedications(
                        items.size()
                )
                .pendingMedications(
                        pendingMedications
                )
                .canDispense(
                        canDispense
                )
                .message(message)
                .build();
    }

    /**
     * Crea el mensaje funcional de la receta.
     */
    private String buildPrescriptionDetailMessage(
            Prescription prescription,
            boolean valid,
            int pendingMedications
    ) {
        if (!valid) {
            return "La receta no se encuentra vigente "
                    + "o no tiene un estado válido para despacho.";
        }

        if (
                prescription.getStatus()
                        == PrescriptionStatus.DESPACHADA
                        ||
                        pendingMedications == 0
        ) {
            return "La receta ya fue despachada completamente.";
        }

        if (
                prescription.getStatus()
                        == PrescriptionStatus
                        .PARCIALMENTE_DESPACHADA
        ) {
            return "La receta tiene medicamentos "
                    + "pendientes de despacho.";
        }

        return "Receta vigente y disponible para despacho.";
    }

    /**
     * Convierte un texto numérico a Long positivo.
     */
    private Long parsePositiveLong(
            String value,
            String errorMessage
    ) {
        try {
            Long result =
                    Long.valueOf(value);

            if (result <= 0) {
                throw new NumberFormatException();
            }

            return result;

        } catch (NumberFormatException exception) {
            throw new RuntimeException(
                    errorMessage
            );
        }
    }

    /**
     * Normaliza un texto obligatorio y limita
     * su longitud.
     */
    private String normalizeRequiredText(String value, int maximumLength, String requiredMessage, String maximumMessage) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    requiredMessage
            );
        }

        String cleanValue =
                value.trim();

        if (
                cleanValue.length()
                        > maximumLength
        ) {
            throw new RuntimeException(
                    maximumMessage
            );
        }

        return cleanValue;
    }

    /**
     * Normaliza nombres de roles y elimina tildes.
     *
     * Farmacia, FARMACIA y farmácia
     * se interpretan como FARMACIA.
     */
    private String normalizeRole(String roleName) {
        if (roleName == null) {
            return "";
        }

        return Normalizer
                .normalize(
                        roleName,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Valida y prepara los medicamentos sin modificar
     * inventario ni cantidades de la receta.
     *
     * Este método puede bloquear filas de inventario,
     * pero no cambia sus valores.
     *
     * El resultado se conserva temporalmente mediante
     * PreparedDispensationItem.
     */
    private List<PreparedDispensationItem>
    prepareDispensationItemsWithoutMutation(Prescription prescription, List<DispensationItemRequest> itemRequests, User pharmacyUser) {
        List<PreparedDispensationItem>
                preparedItems =
                new ArrayList<>();

        for (
                DispensationItemRequest itemRequest
                : itemRequests
        ) {
            /*
             * Verificar que el ítem pertenezca
             * realmente a la receta.
             */
            PrescriptionItem prescriptionItem =
                    prescriptionItemRepository
                            .findByIdAndPrescription_IdAndActiveTrue(
                                    itemRequest
                                            .getPrescriptionItemId(),
                                    prescription.getId()
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "El medicamento seleccionado "
                                                    + "no pertenece a la receta."
                                    )
                            );

            /*
             * Calcular cantidad pendiente.
             */
            int pendingQuantity =
                    calculatePendingQuantity(
                            prescriptionItem
                    );

            /*
             * La cantidad solicitada no puede ser cero,
             * negativa ni superior a la pendiente.
             */
            validateDispensedQuantity(
                    itemRequest.getDispensedQuantity(),
                    pendingQuantity,
                    prescriptionItem
            );

            Medication originalMedication =
                    prescriptionItem
                            .getMedication();

            if (originalMedication == null) {
                throw new RuntimeException(
                        "La receta contiene un medicamento inválido."
                );
            }

            /*
             * Determinar el medicamento que se entregará.
             *
             * Puede ser el original o uno alternativo.
             */
            Medication dispensedMedication =
                    resolveDispensedMedication(
                            originalMedication,
                            itemRequest
                    );

            /*
             * Validar autorización para medicamentos
             * controlados.
             */
            validateControlledMedication(
                    prescriptionItem,
                    dispensedMedication,
                    itemRequest
            );

            /*
             * Recuperar el inventario con bloqueo.
             *
             * El bloqueo evita que otra operación cambie
             * el stock antes de terminar la transacción.
             */
            PharmacyInventory inventory =
                    pharmacyInventoryRepository
                            .findForDispensation(
                                    pharmacyUser
                                            .getBranch()
                                            .getId(),
                                    dispensedMedication
                                            .getId()
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "El medicamento "
                                                    + dispensedMedication
                                                    .getName()
                                                    + " no tiene un inventario "
                                                    + "activo en esta sucursal."
                                    )
                            );

            int stockBefore =
                    inventory.getCurrentStock();

            int quantity =
                    itemRequest
                            .getDispensedQuantity();

            if (stockBefore < quantity) {
                throw new RuntimeException(
                        "Stock insuficiente para "
                                + dispensedMedication.getName()
                                + ". Disponible: "
                                + stockBefore
                                + " unidades. Solicitado: "
                                + quantity
                                + " unidades."
                );
            }

            /*
             * Precio obtenido exclusivamente del backend.
             */
            BigDecimal unitPrice =
                    normalizeMoney(
                            dispensedMedication
                                    .getUnitPrice()
                    );

            if (
                    unitPrice.compareTo(
                            BigDecimal.ZERO
                    ) <= 0
            ) {
                throw new RuntimeException(
                        "El medicamento "
                                + dispensedMedication
                                .getName()
                                + " no tiene un precio válido."
                );
            }

            BigDecimal subtotal =
                    unitPrice
                            .multiply(
                                    BigDecimal.valueOf(
                                            quantity
                                    )
                            )
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            int stockAfter =
                    stockBefore - quantity;

            boolean substituted =
                    Boolean.TRUE.equals(
                            itemRequest
                                    .getSubstituted()
                    );

            DispensationItemStatus itemStatus =
                    determineItemStatus(
                            substituted,
                            quantity,
                            pendingQuantity
                    );

            String substitutionReason =
                    substituted
                            ? normalizeRequiredText(
                            itemRequest
                                    .getSubstitutionReason(),
                            1000,
                            "Debe justificar la sustitución "
                            + "del medicamento.",
                            "La justificación de sustitución "
                            + "no puede exceder los "
                            + "1000 caracteres."
                    )
                            : null;

            String controlAuthorization =
                    normalizeOptionalText(
                            itemRequest
                                    .getControlAuthorization(),
                            200,
                            "La autorización no puede exceder "
                                    + "los 200 caracteres."
                    );

            /*
             * Guardar el resultado temporal.
             *
             * Todavía no se modifica ninguna entidad.
             */
            preparedItems.add(
                    new PreparedDispensationItem(
                            prescriptionItem,
                            originalMedication,
                            dispensedMedication,
                            inventory,
                            pendingQuantity,
                            quantity,
                            unitPrice,
                            subtotal,
                            stockBefore,
                            stockAfter,
                            substituted,
                            substitutionReason,
                            controlAuthorization,
                            itemStatus
                    )
            );
        }

        if (preparedItems.isEmpty()) {
            throw new RuntimeException(
                    "Debe seleccionar al menos un medicamento "
                            + "para el despacho."
            );
        }

        return preparedItems;
    }

    /**
     * Calcula el total a partir de los elementos
     * previamente validados.
     *
     * El frontend no envía ni controla este valor.
     */
    private BigDecimal calculatePreparedTotal(List<PreparedDispensationItem> preparedItems) {
        BigDecimal total =
                preparedItems.stream()
                        .map(
                                PreparedDispensationItem
                                        ::subtotal
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        if (
                total.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {
            throw new RuntimeException(
                    "El total del despacho debe ser "
                            + "mayor que cero."
            );
        }

        return total;
    }

    /**
     * Aplica definitivamente los cambios después
     * de que el cobro fue aprobado.
     *
     * Para cada medicamento:
     *
     * 1. Descuenta inventario.
     * 2. Actualiza quién modificó el stock.
     * 3. Aumenta la cantidad despachada en la receta.
     * 4. Crea el detalle histórico del despacho.
     *
     * Este método no debe ejecutarse antes de aprobar
     * el pago.
     */
    private void applyApprovedDispensationItems(MedicationDispensation dispensation, List<PreparedDispensationItem> preparedItems, User pharmacyUser) {
        LocalDateTime now =
                LocalDateTime.now();

        for (
                PreparedDispensationItem prepared
                : preparedItems
        ) {
            PharmacyInventory inventory =
                    prepared.inventory();

            PrescriptionItem prescriptionItem =
                    prepared.prescriptionItem();

            /*
             * Validación defensiva.
             *
             * Aunque la fila se encuentra bloqueada,
             * se verifica nuevamente el stock.
             */
            if (
                    inventory.getCurrentStock()
                            < prepared.dispensedQuantity()
            ) {
                throw new RuntimeException(
                        "El inventario de "
                                + prepared
                                .dispensedMedication()
                                .getName()
                                + " cambió durante la operación. "
                                + "Recargue la receta e intente "
                                + "nuevamente."
                );
            }

            /*
             * Descontar inventario.
             */
            inventory.setCurrentStock(
                    prepared.stockAfter()
            );

            inventory.setUpdatedAt(now);

            inventory.setUpdatedBy(
                    pharmacyUser.getUsername()
            );

            /*
             * Actualizar la cantidad acumulada entregada.
             */
            int previousDispensed =
                    prescriptionItem
                            .getDispensedQuantity() != null
                            ? prescriptionItem
                            .getDispensedQuantity()
                            : 0;

            int newDispensedQuantity =
                    previousDispensed
                            + prepared
                            .dispensedQuantity();

            if (
                    newDispensedQuantity >
                            prescriptionItem
                                    .getPrescribedQuantity()
            ) {
                throw new RuntimeException(
                        "La cantidad acumulada entregada para "
                                + prepared
                                .originalMedication()
                                .getName()
                                + " excede la cantidad prescrita."
                );
            }

            prescriptionItem.setDispensedQuantity(
                    newDispensedQuantity
            );

            /*
             * Construir el detalle histórico.
             */
            MedicationDispensationItem
                    dispensationItem =
                    MedicationDispensationItem.builder()
                            .dispensation(
                                    dispensation
                            )
                            .prescriptionItem(
                                    prescriptionItem
                            )
                            .originalMedication(
                                    prepared
                                            .originalMedication()
                            )
                            .dispensedMedication(
                                    prepared
                                            .dispensedMedication()
                            )
                            .prescribedQuantity(
                                    prescriptionItem
                                            .getPrescribedQuantity()
                            )
                            .pendingQuantityBefore(
                                    prepared
                                            .pendingQuantityBefore()
                            )
                            .dispensedQuantity(
                                    prepared
                                            .dispensedQuantity()
                            )
                            .unitPrice(
                                    prepared.unitPrice()
                            )
                            .subtotal(
                                    prepared.subtotal()
                            )
                            .stockBefore(
                                    prepared.stockBefore()
                            )
                            .stockAfter(
                                    prepared.stockAfter()
                            )
                            .substituted(
                                    prepared.substituted()
                            )
                            .substitutionReason(
                                    prepared
                                            .substitutionReason()
                            )
                            .controlledMedication(
                                    Boolean.TRUE.equals(
                                            prescriptionItem
                                                    .getControlledMedication()
                                    )
                                            ||
                                            Boolean.TRUE.equals(
                                                    prepared
                                                            .dispensedMedication()
                                                            .getControlled()
                                            )
                            )
                            .controlAuthorization(
                                    prepared
                                            .controlAuthorization()
                            )
                            .status(
                                    prepared.itemStatus()
                            )
                            .build();

            dispensation
                    .getItems()
                    .add(
                            dispensationItem
                    );
        }

        /*
         * Forzar sincronización de inventarios e ítems.
         *
         * Si existe conflicto de versión, la excepción
         * provoca rollback de toda la operación.
         */
        try {
            /*
             * Guardar primero los inventarios actualizados.
             *
             * Así los movimientos se registran únicamente
             * después de comprobar que el descuento pudo
             * persistirse correctamente.
             */
            pharmacyInventoryRepository
                    .saveAllAndFlush(
                            preparedItems.stream()
                                    .map(
                                            PreparedDispensationItem
                                                    ::inventory
                                    )
                                    .toList()
                    );

            /*
             * Guardar las cantidades acumuladas
             * despachadas de la receta.
             */
            prescriptionItemRepository
                    .saveAllAndFlush(
                            preparedItems.stream()
                                    .map(
                                            PreparedDispensationItem
                                                    ::prescriptionItem
                                    )
                                    .toList()
                    );

            if (dispensation.getId() == null) {
                throw new RuntimeException(
                        "No fue posible identificar el despacho "
                                + "para registrar los movimientos."
                );
            }

            /*
             * Registrar un movimiento DESPACHO por cada
             * medicamento entregado.
             *
             * Cada movimiento conserva:
             *
             * - Stock anterior.
             * - Cantidad entregada.
             * - Stock resultante.
             * - Número de despacho.
             * - Número de receta.
             * - ID del despacho.
             * - Usuario de farmacia.
             */
            for (
                    PreparedDispensationItem prepared
                    : preparedItems
            ) {
                String movementReason =
                        "Despacho "
                                + dispensation
                                .getDispensationNumber()
                                + " asociado con la receta "
                                + dispensation
                                .getPrescription()
                                .getPrescriptionNumber()
                                + ".";

                inventoryMovementService
                        .registerMovement(
                                prepared.inventory(),
                                InventoryMovementType.DESPACHO,
                                prepared.stockBefore(),
                                prepared.dispensedQuantity(),
                                prepared.stockAfter(),
                                movementReason,
                                "DISPENSATION",
                                dispensation.getId(),
                                pharmacyUser.getUsername()
                        );
            }

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El inventario o la receta fueron "
                            + "actualizados por otra persona. "
                            + "Recargue la información e "
                            + "intente nuevamente."
            );
        }
    }

    /**
     * Determina cuál medicamento será entregado.
     *
     * Sin sustitución:
     * devuelve el medicamento original.
     *
     * Con sustitución:
     * valida medicamento alternativo, diferencia respecto
     * al original y mismo principio activo.
     */
    private Medication resolveDispensedMedication(Medication originalMedication, DispensationItemRequest itemRequest) {
        boolean substituted =
                Boolean.TRUE.equals(
                        itemRequest.getSubstituted()
                );

        if (!substituted) {
            if (
                    itemRequest.getAlternativeMedicationId()
                            != null
            ) {
                throw new RuntimeException(
                        "No debe enviar un medicamento alternativo "
                                + "si no se realizará una sustitución."
                );
            }

            return originalMedication;
        }

        if (
                itemRequest.getAlternativeMedicationId()
                        == null
        ) {
            throw new RuntimeException(
                    "Debe seleccionar el medicamento alternativo."
            );
        }

        normalizeRequiredText(
                itemRequest.getSubstitutionReason(),
                1000,
                "Debe justificar la sustitución "
                        + "del medicamento.",
                "La justificación de sustitución no puede "
                        + "exceder los 1000 caracteres."
        );

        if (
                originalMedication
                        .getId()
                        .equals(
                                itemRequest
                                        .getAlternativeMedicationId()
                        )
        ) {
            throw new RuntimeException(
                    "El medicamento alternativo debe ser "
                            + "diferente al medicamento original."
            );
        }

        Medication alternative =
                medicationRepository
                        .findByIdAndActiveTrue(
                                itemRequest
                                        .getAlternativeMedicationId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "El medicamento alternativo "
                                                + "no existe o se encuentra "
                                                + "inactivo."
                                )
                        );

        String originalIngredient =
                originalMedication
                        .getActiveIngredient();

        String alternativeIngredient =
                alternative
                        .getActiveIngredient();

        if (
                originalIngredient == null ||
                        alternativeIngredient == null ||
                        !originalIngredient
                                .trim()
                                .equalsIgnoreCase(
                                        alternativeIngredient
                                                .trim()
                                )
        ) {
            throw new RuntimeException(
                    "El medicamento alternativo debe tener "
                            + "el mismo principio activo que "
                            + "el medicamento original."
            );
        }

        return alternative;
    }

    /**
     * Exige autorización adicional si el medicamento
     * original o el alternativo está clasificado
     * como controlado.
     */
    private void validateControlledMedication(PrescriptionItem prescriptionItem, Medication dispensedMedication, DispensationItemRequest request) {
        boolean controlled =
                Boolean.TRUE.equals(
                        prescriptionItem
                                .getControlledMedication()
                )
                        ||
                        Boolean.TRUE.equals(
                                dispensedMedication
                                        .getControlled()
                        );

        if (!controlled) {
            return;
        }

        normalizeRequiredText(
                request.getControlAuthorization(),
                200,
                "Debe registrar la autorización requerida "
                        + "para despachar el medicamento controlado.",
                "La autorización no puede exceder "
                        + "los 200 caracteres."
        );
    }

    /**
     * Valida los datos generales recibidos.
     */
    private void validateDispensationRequest(ConfirmDispensationRequest request) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del despacho son obligatorios."
            );
        }

        if (request.getPrescriptionId() == null) {
            throw new RuntimeException(
                    "Debe indicar la receta médica."
            );
        }

        if (request.getIdempotencyKey() == null) {
            throw new RuntimeException(
                    "La llave de idempotencia es obligatoria."
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
                request.getItems() == null ||
                        request.getItems().isEmpty()
        ) {
            throw new RuntimeException(
                    "Debe seleccionar al menos un medicamento "
                            + "para el despacho."
            );
        }

        if (request.getItems().size() > 30) {
            throw new RuntimeException(
                    "El despacho no puede incluir más "
                            + "de 30 medicamentos."
            );
        }
    }

    /**
     * Valida nuevamente vigencia, estado y cantidades
     * antes de modificar inventario.
     */
    private void validatePrescriptionForDispensation(Prescription prescription) {
        long elapsedDays =
                calculateElapsedDays(
                        prescription
                );

        if (elapsedDays > MAXIMUM_VALIDITY_DAYS) {
            throw new RuntimeException(
                    "Receta vencida. La receta fue emitida hace "
                            + elapsedDays
                            + " días y ya no es válida "
                            + "para despacho."
            );
        }

        if (
                !isDispensableStatus(
                        prescription.getStatus()
                )
        ) {
            throw new RuntimeException(
                    "La receta no se encuentra en un estado "
                            + "válido para despacho."
            );
        }

        if (
                !Boolean.TRUE.equals(
                        prescription.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La receta se encuentra inactiva."
            );
        }
    }

    /**
     * Impide incluir dos veces el mismo ítem
     * de la receta en un despacho.
     */
    private void validateNoDuplicateDispensationItems(List<DispensationItemRequest> items) {
        Set<Long> itemIds =
                new HashSet<>();

        for (DispensationItemRequest item : items) {
            if (
                    item == null ||
                            item.getPrescriptionItemId() == null
            ) {
                throw new RuntimeException(
                        "El despacho contiene un medicamento inválido."
                );
            }

            if (
                    !itemIds.add(
                            item.getPrescriptionItemId()
                    )
            ) {
                throw new RuntimeException(
                        "No debe incluir el mismo medicamento "
                                + "más de una vez en el despacho."
                );
            }
        }
    }

    /**
     * Valida la cantidad solicitada frente a la
     * cantidad pendiente de la receta.
     */
    private void validateDispensedQuantity(Integer quantity, int pendingQuantity, PrescriptionItem item) {
        if (
                quantity == null ||
                        quantity <= 0
        ) {
            throw new RuntimeException(
                    "La cantidad a despachar debe ser "
                            + "mayor que cero."
            );
        }

        if (quantity > 10000) {
            throw new RuntimeException(
                    "La cantidad a despachar no puede "
                            + "exceder 10000 unidades."
            );
        }

        if (pendingQuantity <= 0) {
            throw new RuntimeException(
                    "El medicamento "
                            + item.getMedication().getName()
                            + " ya fue despachado completamente."
            );
        }

        if (quantity > pendingQuantity) {
            throw new RuntimeException(
                    "La cantidad a despachar para "
                            + item.getMedication().getName()
                            + " no puede exceder las "
                            + pendingQuantity
                            + " unidades pendientes."
            );
        }
    }

    /**
     * Procesa el cobro según el método seleccionado.
     */
    private PaymentProcessingResult processPayment(
            PaymentMethod paymentMethod,
            BigDecimal total,
            ConfirmDispensationRequest request
    ) {
        if (
                paymentMethod ==
                        PaymentMethod.EFECTIVO
        ) {
            return processCashPayment(
                    total,
                    request.getReceivedAmount()
            );
        }

        return processCardPayment(
                total,
                request
        );
    }

    /**
     * Valida efectivo y calcula el cambio.
     */
    private PaymentProcessingResult processCashPayment(
            BigDecimal total,
            BigDecimal receivedAmount
    ) {
        if (receivedAmount == null) {
            throw new RuntimeException(
                    "Debe ingresar el monto recibido."
            );
        }

        BigDecimal normalizedReceived =
                normalizeMoney(
                        receivedAmount
                );

        if (
                normalizedReceived.compareTo(
                        total
                ) < 0
        ) {
            throw new RuntimeException(
                    "El monto recibido (Q"
                            + normalizedReceived
                            + ") es menor al total a cobrar (Q"
                            + total
                            + ")."
            );
        }

        BigDecimal change =
                normalizedReceived
                        .subtract(total)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return new PaymentProcessingResult(
                true,
                PaymentStatus.APROBADO,
                normalizedReceived,
                change,
                null,
                generateTransactionNumber(),
                null,
                null,
                null,
                "Pago en efectivo aprobado."
        );
    }

    /**
     * Procesa un pago con tarjeta mediante la pasarela.
     */
    private PaymentProcessingResult processCardPayment(BigDecimal total, ConfirmDispensationRequest request) {
        if (
                request.getCardLastFour() == null ||
                        !request.getCardLastFour()
                                .matches("^\\d{4}$")
        ) {
            throw new RuntimeException(
                    "Debe ingresar exactamente los últimos "
                            + "4 dígitos de la tarjeta."
            );
        }

        if (
                request.getPaymentToken() == null ||
                        request.getPaymentToken().isBlank()
        ) {
            throw new RuntimeException(
                    "El token de pago es obligatorio "
                            + "para pagos con tarjeta."
            );
        }

        GatewayPaymentResult gatewayResult;

        try {
            gatewayResult =
                    paymentGateway.processPayment(
                            request.getPaymentToken(),
                            total,
                            CURRENCY
                    );

        } catch (RuntimeException exception) {
            return new PaymentProcessingResult(
                    false,
                    PaymentStatus.ERROR,
                    null,
                    null,
                    request.getCardLastFour(),
                    null,
                    null,
                    "COMMUNICATION_ERROR",
                    "No fue posible comunicarse con la pasarela.",
                    "Error de comunicación con la pasarela "
                            + "de pago."
            );
        }

        if (
                !Boolean.TRUE.equals(
                        gatewayResult.getApproved()
                )
        ) {
            PaymentStatus status =
                    isTechnicalGatewayError(
                            gatewayResult
                    )
                            ? PaymentStatus.ERROR
                            : PaymentStatus.RECHAZADO;

            return new PaymentProcessingResult(
                    false,
                    status,
                    null,
                    null,
                    request.getCardLastFour(),
                    null,
                    gatewayResult
                            .getGatewayReference(),
                    gatewayResult
                            .getFailureCode(),
                    gatewayResult
                            .getFailureMessage(),
                    gatewayResult
                            .getFailureMessage()
            );
        }

        return new PaymentProcessingResult(
                true,
                PaymentStatus.APROBADO,
                null,
                null,
                request.getCardLastFour(),
                gatewayResult
                        .getTransactionNumber(),
                gatewayResult
                        .getGatewayReference(),
                null,
                null,
                "Pago con tarjeta aprobado."
        );
    }

    /**
     * Copia el resultado del cobro al despacho.
     */
    private void applyPaymentResult(MedicationDispensation dispensation, PaymentProcessingResult result) {
        dispensation.setPaymentStatus(
                result.paymentStatus()
        );

        dispensation.setReceivedAmount(
                result.receivedAmount()
        );

        dispensation.setChangeAmount(
                result.changeAmount()
        );

        dispensation.setCardLastFour(
                result.cardLastFour()
        );

        dispensation.setTransactionNumber(
                result.transactionNumber()
        );

        dispensation.setGatewayReference(
                result.gatewayReference()
        );

        dispensation.setFailureCode(
                result.failureCode()
        );

        dispensation.setFailureMessage(
                result.failureMessage()
        );

        if (!result.approved()) {
            dispensation.setStatus(
                    result.paymentStatus()
                            == PaymentStatus.RECHAZADO
                            ? DispensationStatus.CANCELADO
                            : DispensationStatus.ERROR
            );
        }
    }

    /**
     * Actualiza la receta según las cantidades pendientes.
     */
    private void updatePrescriptionStatus(Prescription prescription) {
        List<PrescriptionItem> items =
                prescriptionItemRepository
                        .findByPrescription_IdAndActiveTrueOrderByIdAsc(
                                prescription.getId()
                        );

        boolean allDispensed =
                !items.isEmpty()
                        && items.stream()
                        .allMatch(item ->
                                calculatePendingQuantity(
                                        item
                                ) == 0
                        );

        if (allDispensed) {
            prescription.setStatus(
                    PrescriptionStatus.DESPACHADA
            );

            prescription.setFullyDispensedAt(
                    LocalDateTime.now()
            );

        } else {
            prescription.setStatus(
                    PrescriptionStatus
                            .PARCIALMENTE_DESPACHADA
            );

            prescription.setFullyDispensedAt(null);
        }

        prescription.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    private PaymentMethod parsePaymentMethod(String value) {
        final PaymentMethod paymentMethod;

        try {
            paymentMethod =
                    PaymentMethod.valueOf(
                            value.trim()
                                    .toUpperCase(
                                            Locale.ROOT
                                    )
                    );

        } catch (Exception exception) {
            throw new RuntimeException(
                    "El método de pago no es válido. "
                            + "Use EFECTIVO, VISA, MASTERCARD "
                            + "o DEBITO."
            );
        }

        if (
                paymentMethod ==
                        PaymentMethod.EN_LINEA
        ) {
            throw new RuntimeException(
                    "El método EN_LINEA no está permitido "
                            + "para el despacho en Farmacia."
            );
        }

        return paymentMethod;
    }

    private DispensationItemStatus determineItemStatus(boolean substituted, int quantity, int pendingQuantity) {
        if (substituted) {
            return DispensationItemStatus.SUSTITUIDO;
        }

        return quantity == pendingQuantity
                ? DispensationItemStatus.COMPLETO
                : DispensationItemStatus.PARCIAL;
    }

    private boolean isTechnicalGatewayError(GatewayPaymentResult result) {
        return "PROCESSING_ERROR".equalsIgnoreCase(
                result.getFailureCode()
        )
                ||
                "COMMUNICATION_ERROR".equalsIgnoreCase(
                        result.getFailureCode()
                );
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO
                    .setScale(2);
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private String generateDispensationNumber() {
        return "DSP-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    }

    private String generateTransactionNumber() {
        return "PHA-TXN-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String value, int maximumLength, String maximumMessage) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        String cleanValue =
                value.trim();

        if (
                cleanValue.length() >
                        maximumLength
        ) {
            throw new RuntimeException(
                    maximumMessage
            );
        }

        return cleanValue;
    }

    /**
     * Resultado temporal de la validación de un medicamento.
     *
     * Este record no es una entidad y no crea una tabla.
     *
     * Su propósito es separar:
     *
     * - Validación y cálculo.
     * - Modificaciones definitivas del inventario.
     */
    private record PreparedDispensationItem(
            PrescriptionItem prescriptionItem,
            Medication originalMedication,
            Medication dispensedMedication,
            PharmacyInventory inventory,
            int pendingQuantityBefore,
            int dispensedQuantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            int stockBefore,
            int stockAfter,
            boolean substituted,
            String substitutionReason,
            String controlAuthorization,
            DispensationItemStatus itemStatus
    ) {
    }

    /**
     * Resultado interno del procesamiento del cobro.
     *
     * No se almacena como entidad. Únicamente transporta
     * datos dentro del servicio.
     */
    private record PaymentProcessingResult(
            boolean approved,
            PaymentStatus paymentStatus,
            BigDecimal receivedAmount,
            BigDecimal changeAmount,
            String cardLastFour,
            String transactionNumber,
            String gatewayReference,
            String failureCode,
            String failureMessage,
            String message
    ) {
    }

    /**
     * Convierte el despacho en un comprobante seguro.
     */
    private MedicationDispensationResponse toDispensationResponse(MedicationDispensation dispensation, String message) {
        List<MedicationDispensationItem> items =
                medicationDispensationItemRepository
                        .findByDispensation_IdOrderByIdAsc(
                                dispensation.getId()
                        );

        List<String> stockAlerts =
                items.stream()
                        .filter(item ->
                                item.getStockAfter()
                                        <= findMinimumStock(
                                        dispensation,
                                        item
                                )
                        )
                        .map(item ->
                                "ALERTA: El medicamento "
                                        + item.getDispensedMedication()
                                        .getName()
                                        + " ha alcanzado el nivel "
                                        + "de stock mínimo ("
                                        + item.getStockAfter()
                                        + " unidades restantes). "
                                        + "Se recomienda generar "
                                        + "una orden de reabastecimiento."
                        )
                        .toList();

        return MedicationDispensationResponse
                .builder()
                .dispensationId(
                        dispensation.getId()
                )
                .dispensationNumber(
                        dispensation
                                .getDispensationNumber()
                )
                .prescriptionId(
                        dispensation
                                .getPrescription()
                                .getId()
                )
                .prescriptionNumber(
                        dispensation
                                .getPrescription()
                                .getPrescriptionNumber()
                )
                .patientName(
                        dispensation
                                .getPatient()
                                .getFullName()
                )
                .patientDpi(
                        dispensation
                                .getPatient()
                                .getDpi()
                )
                .doctorName(
                        dispensation
                                .getPrescription()
                                .getDoctor()
                                .getFullName()
                )
                .branch(
                        dispensation
                                .getBranch()
                                .getName()
                )
                .dispensationStatus(
                        dispensation
                                .getStatus()
                                .name()
                )
                .prescriptionStatus(
                        dispensation
                                .getPrescription()
                                .getStatus()
                                .name()
                )
                .totalAmount(
                        dispensation
                                .getTotalAmount()
                )
                .currency(
                        dispensation.getCurrency()
                )
                .paymentMethod(
                        dispensation
                                .getPaymentMethod()
                                .name()
                )
                .paymentStatus(
                        dispensation
                                .getPaymentStatus()
                                .name()
                )
                .receivedAmount(
                        dispensation
                                .getReceivedAmount()
                )
                .changeAmount(
                        dispensation
                                .getChangeAmount()
                )
                .cardLastFour(
                        dispensation
                                .getCardLastFour()
                )
                .transactionNumber(
                        dispensation
                                .getTransactionNumber()
                )
                .pharmacyUsername(
                        dispensation
                                .getPharmacyUsername()
                )
                .confirmedAt(
                        dispensation
                                .getConfirmedAt() != null
                                ? dispensation
                                .getConfirmedAt()
                                .toString()
                                : null
                )
                .totalItems(
                        items.size()
                )
                .items(
                        items.stream()
                                .map(this::toDispensationItemResponse)
                                .toList()
                )
                .stockAlerts(
                        stockAlerts
                )
                .message(message)
                .build();
    }

    private int findMinimumStock(MedicationDispensation dispensation, MedicationDispensationItem item) {
        return pharmacyInventoryRepository
                .findByBranch_IdAndMedication_IdAndActiveTrue(
                        dispensation.getBranch().getId(),
                        item.getDispensedMedication().getId()
                )
                .map(
                        PharmacyInventory::getMinimumStock
                )
                .orElse(0);
    }

    private MedicationDispensationItemResponse toDispensationItemResponse(MedicationDispensationItem item) {
        int minimumStock =
                findMinimumStock(
                        item.getDispensation(),
                        item
                );

        boolean lowStock =
                item.getStockAfter()
                        <= minimumStock;

        return MedicationDispensationItemResponse
                .builder()
                .dispensationItemId(
                        item.getId()
                )
                .prescriptionItemId(
                        item.getPrescriptionItem()
                                .getId()
                )
                .originalMedicationId(
                        item.getOriginalMedication()
                                .getId()
                )
                .originalMedicationName(
                        item.getOriginalMedication()
                                .getName()
                )
                .dispensedMedicationId(
                        item.getDispensedMedication()
                                .getId()
                )
                .dispensedMedicationName(
                        item.getDispensedMedication()
                                .getName()
                )
                .prescribedQuantity(
                        item.getPrescribedQuantity()
                )
                .pendingQuantityBefore(
                        item.getPendingQuantityBefore()
                )
                .dispensedQuantity(
                        item.getDispensedQuantity()
                )
                .unitPrice(
                        item.getUnitPrice()
                )
                .subtotal(
                        item.getSubtotal()
                )
                .stockBefore(
                        item.getStockBefore()
                )
                .stockAfter(
                        item.getStockAfter()
                )
                .substituted(
                        item.getSubstituted()
                )
                .substitutionReason(
                        item.getSubstitutionReason()
                )
                .controlledMedication(
                        item.getControlledMedication()
                )
                .status(
                        item.getStatus().name()
                )
                .lowStockAfterDispensation(
                        lowStock
                )
                .stockAlert(
                        lowStock
                                ? "El medicamento alcanzó "
                                  + "el nivel de stock mínimo."
                                : null
                )
                .build();
    }
}