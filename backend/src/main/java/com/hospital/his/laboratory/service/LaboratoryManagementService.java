package com.hospital.his.laboratory.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.laboratory.dto.LaboratoryOrderItemResponse;
import com.hospital.his.laboratory.dto.LaboratoryOrderResponse;
import com.hospital.his.laboratory.dto.LaboratoryOrderSummaryResponse;
import com.hospital.his.laboratory.dto.SaveLaboratoryResultRequest;
import com.hospital.his.laboratory.entity.LaboratoryOrder;
import com.hospital.his.laboratory.entity.LaboratoryOrderItem;
import com.hospital.his.laboratory.entity.LaboratoryOrderItemStatus;
import com.hospital.his.laboratory.entity.LaboratoryOrderStatus;
import com.hospital.his.laboratory.entity.LaboratoryTest;
import com.hospital.his.laboratory.repository.LaboratoryOrderItemRepository;
import com.hospital.his.laboratory.repository.LaboratoryOrderRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class LaboratoryManagementService {

    private static final String CURRENCY = "GTQ";

    private final LaboratoryOrderRepository
            laboratoryOrderRepository;

    private final LaboratoryOrderItemRepository
            laboratoryOrderItemRepository;

    private final UserRepository
            userRepository;

    private final AuditService
            auditService;

    public LaboratoryManagementService(
            LaboratoryOrderRepository laboratoryOrderRepository,
            LaboratoryOrderItemRepository laboratoryOrderItemRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.laboratoryOrderRepository =
                laboratoryOrderRepository;

        this.laboratoryOrderItemRepository =
                laboratoryOrderItemRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /**
     * Lista las órdenes de la sucursal asignada
     * al personal de laboratorio.
     *
     * Los filtros son opcionales:
     * status, patient y doctor.
     */
    @Transactional(readOnly = true)
    public List<LaboratoryOrderSummaryResponse>
    getLaboratoryOrders(
            String status,
            String patient,
            String doctor,
            String laboratoryUsername
    ) {
        User laboratoryUser =
                validateLaboratoryUser(
                        laboratoryUsername
                );

        if (laboratoryUser.getBranch() == null) {
            throw new RuntimeException(
                    "El usuario de laboratorio no tiene una sucursal asignada."
            );
        }

        LaboratoryOrderStatus parsedStatus =
                parseOptionalStatus(
                        status
                );

        List<LaboratoryOrder> orders;

        if (parsedStatus != null) {
            orders =
                    laboratoryOrderRepository
                            .findByAppointment_Branch_IdAndStatusAndActiveTrueOrderByCreatedAtDesc(
                                    laboratoryUser
                                            .getBranch()
                                            .getId(),
                                    parsedStatus
                            );

        } else {
            orders =
                    laboratoryOrderRepository
                            .findByAppointment_Branch_IdAndActiveTrueOrderByCreatedAtDesc(
                                    laboratoryUser
                                            .getBranch()
                                            .getId()
                            );
        }

        String cleanPatient =
                normalizeFilter(patient);

        String cleanDoctor =
                normalizeFilter(doctor);

        return orders.stream()
                .filter(order ->
                        cleanPatient == null
                                || containsIgnoreCase(
                                order.getPatient()
                                        .getFullName(),
                                cleanPatient
                        )
                )
                .filter(order ->
                        cleanDoctor == null
                                || containsIgnoreCase(
                                order.getDoctor()
                                        .getFullName(),
                                cleanDoctor
                        )
                )
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Busca una orden por ID y valida que pertenezca
     * a la sucursal del personal autenticado.
     */
    @Transactional(readOnly = true)
    public LaboratoryOrderResponse getLaboratoryOrder(
            Long orderId,
            String laboratoryUsername
    ) {
        User laboratoryUser =
                validateLaboratoryUser(
                        laboratoryUsername
                );

        LaboratoryOrder order =
                findLaboratoryOrder(
                        orderId
                );

        validateSameBranch(
                order,
                laboratoryUser
        );

        return toOrderResponse(
                order,
                "Orden de laboratorio encontrada."
        );
    }

    /**
     * Guarda o actualiza el resultado de un examen.
     * No publica el resultado.
     */
    @Transactional
    public LaboratoryOrderItemResponse saveResult(
            Long orderId,
            Long itemId,
            SaveLaboratoryResultRequest request,
            String laboratoryUsername
    ) {
        User laboratoryUser =
                validateLaboratoryUser(
                        laboratoryUsername
                );

        LaboratoryOrder order =
                findLaboratoryOrder(
                        orderId
                );

        validateSameBranch(
                order,
                laboratoryUser
        );

        validateOrderForResultEntry(
                order
        );

        validateResultRequest(
                request
        );

        LaboratoryOrderItem item =
                laboratoryOrderItemRepository
                        .findByIdAndLaboratoryOrder_Id(
                                itemId,
                                orderId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "El examen indicado no pertenece a la orden."
                                )
                        );

        if (Boolean.TRUE.equals(
                item.getPublished()
        )) {
            throw new RuntimeException(
                    "El resultado ya fue publicado y no puede modificarse."
            );
        }

        LocalDateTime resultDate =
                request.getResultDate() != null
                        ? request.getResultDate()
                        : LocalDateTime.now();

        if (resultDate.isAfter(
                LocalDateTime.now()
                        .plusMinutes(1)
        )) {
            throw new RuntimeException(
                    "La fecha del resultado no puede encontrarse en el futuro."
            );
        }

        item.setResultValue(
                request.getResultValue()
                        .trim()
        );

        item.setResultUnit(
                normalizeOptionalText(
                        request.getUnit(),
                        50,
                        "La unidad no puede exceder los 50 caracteres."
                )
        );

        item.setResultDate(
                resultDate
        );

        item.setOutOfRange(
                Boolean.TRUE.equals(
                        request.getOutOfRange()
                )
        );

        item.setResultNotes(
                normalizeOptionalText(
                        request.getNotes(),
                        1000,
                        "Las notas del resultado no pueden exceder los 1000 caracteres."
                )
        );

        item.setResultSavedAt(
                LocalDateTime.now()
        );

        item.setResultSavedBy(
                laboratoryUsername
        );

        item.setStatus(
                LaboratoryOrderItemStatus
                        .RESULTADO_GUARDADO
        );

        item.setPublished(false);
        item.setPublishedAt(null);
        item.setPublishedBy(null);

        order.setUpdatedAt(
                LocalDateTime.now()
        );

        try {
            item =
                    laboratoryOrderItemRepository
                            .saveAndFlush(item);

            laboratoryOrderRepository
                    .saveAndFlush(order);

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "El resultado fue actualizado por otro usuario. "
                            + "Actualice la orden e intente nuevamente."
            );
        }

        auditService.log(
                laboratoryUsername,
                "SAVE_LABORATORY_RESULT",
                "LABORATORY",
                "Resultado guardado para examen "
                        + item.getLaboratoryTest()
                        .getCode()
                        + ", orden "
                        + order.getOrderNumber()
                        + ". Fuera de rango: "
                        + item.getOutOfRange()
                        + "."
        );

        return toItemResponse(
                item
        );
    }

    /**
     * Publica de forma individual un resultado
     * previamente guardado.
     */
    @Transactional
    public LaboratoryOrderResponse publishResult(
            Long orderId,
            Long itemId,
            String laboratoryUsername
    ) {
        User laboratoryUser =
                validateLaboratoryUser(
                        laboratoryUsername
                );

        LaboratoryOrder order =
                findLaboratoryOrder(
                        orderId
                );

        validateSameBranch(
                order,
                laboratoryUser
        );

        validateOrderForResultEntry(
                order
        );

        LaboratoryOrderItem item =
                laboratoryOrderItemRepository
                        .findByIdAndLaboratoryOrder_Id(
                                itemId,
                                orderId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "El examen indicado no pertenece a la orden."
                                )
                        );

        if (Boolean.TRUE.equals(
                item.getPublished()
        )) {
            return toOrderResponse(
                    order,
                    "El resultado ya se encuentra publicado."
            );
        }

        if (item.getStatus()
                != LaboratoryOrderItemStatus
                .RESULTADO_GUARDADO) {

            throw new RuntimeException(
                    "Debe guardar el resultado antes de publicarlo."
            );
        }

        if (item.getResultValue() == null
                || item.getResultValue()
                .isBlank()) {

            throw new RuntimeException(
                    "No es posible publicar un resultado vacío."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        item.setStatus(
                LaboratoryOrderItemStatus.PUBLICADO
        );

        item.setPublished(true);
        item.setPublishedAt(now);
        item.setPublishedBy(
                laboratoryUsername
        );

        try {
            laboratoryOrderItemRepository
                    .saveAndFlush(item);

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "El resultado fue actualizado por otro usuario. "
                            + "Actualice la orden."
            );
        }

        boolean pendingResults =
                laboratoryOrderItemRepository
                        .existsByLaboratoryOrder_IdAndPublishedFalse(
                                orderId
                        );

        String message;

        if (!pendingResults) {
            order.setStatus(
                    LaboratoryOrderStatus.COMPLETADA
            );

            order.setCompletedAt(now);
            order.setUpdatedAt(now);

            laboratoryOrderRepository
                    .saveAndFlush(order);

            message =
                    "Resultado publicado exitosamente. "
                            + "Todos los resultados fueron publicados "
                            + "y la orden quedó completada.";

        } else {
            order.setUpdatedAt(now);

            laboratoryOrderRepository
                    .saveAndFlush(order);

            message =
                    "Resultado publicado exitosamente.";
        }

        auditService.log(
                laboratoryUsername,
                "PUBLISH_LABORATORY_RESULT",
                "LABORATORY",
                "Resultado publicado para examen "
                        + item.getLaboratoryTest()
                        .getCode()
                        + ", orden "
                        + order.getOrderNumber()
                        + ". Estado de orden: "
                        + order.getStatus()
                        .name()
                        + "."
        );

        return toOrderResponse(
                order,
                message
        );
    }

    private User validateLaboratoryUser(
            String username
    ) {
        if (username == null
                || username.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al personal de laboratorio autenticado."
            );
        }

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontró al personal de laboratorio autenticado."
                                )
                        );

        if (!Boolean.TRUE.equals(
                user.getActive()
        )) {
            throw new RuntimeException(
                    "La cuenta del personal de laboratorio se encuentra inactiva."
            );
        }

        String roleName =
                user.getRole() != null
                        ? user.getRole()
                        .getName()
                        : null;

        if (!isLaboratoryRole(roleName)) {
            throw new RuntimeException(
                    "Solamente el personal de laboratorio puede realizar esta operación."
            );
        }

        return user;
    }

    private boolean isLaboratoryRole(
            String roleName
    ) {
        if (roleName == null) {
            return false;
        }

        String normalizedRole =
                roleName.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        return normalizedRole.equals(
                "LABORATORIO"
        );
    }

    private LaboratoryOrder findLaboratoryOrder(
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

    private void validateSameBranch(
            LaboratoryOrder order,
            User laboratoryUser
    ) {
        if (laboratoryUser.getBranch() == null) {
            throw new RuntimeException(
                    "El usuario de laboratorio no tiene una sucursal asignada."
            );
        }

        if (order.getAppointment() == null
                || order.getAppointment()
                .getBranch() == null) {

            throw new RuntimeException(
                    "La orden no tiene una sucursal asociada."
            );
        }

        if (!order.getAppointment()
                .getBranch()
                .getId()
                .equals(
                        laboratoryUser
                                .getBranch()
                                .getId()
                )) {

            throw new RuntimeException(
                    "La orden no pertenece a la sucursal del personal autenticado."
            );
        }
    }

    private void validateOrderForResultEntry(
            LaboratoryOrder order
    ) {
        if (!Boolean.TRUE.equals(
                order.getActive()
        )) {
            throw new RuntimeException(
                    "La orden no se encuentra activa."
            );
        }

        if (Boolean.TRUE.equals(
                order.getExternalOrder()
        )) {
            throw new RuntimeException(
                    "La orden está marcada como externa. "
                            + "Los resultados serán presentados directamente al médico."
            );
        }

        if (order.getStatus()
                == LaboratoryOrderStatus
                .PENDIENTE_DE_PAGO) {

            throw new RuntimeException(
                    "La orden se encuentra pendiente de pago. "
                            + "La toma de muestras y el registro de resultados "
                            + "no están habilitados."
            );
        }

        if (order.getStatus()
                == LaboratoryOrderStatus
                .COMPLETADA) {

            throw new RuntimeException(
                    "La orden ya se encuentra completada."
            );
        }

        if (order.getStatus()
                == LaboratoryOrderStatus
                .CANCELADA) {

            throw new RuntimeException(
                    "La orden se encuentra cancelada."
            );
        }

        if (order.getStatus()
                != LaboratoryOrderStatus
                .EN_PROCESO) {

            throw new RuntimeException(
                    "La orden no se encuentra en un estado válido "
                            + "para registrar resultados."
            );
        }
    }

    private void validateResultRequest(
            SaveLaboratoryResultRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del resultado son obligatorios."
            );
        }

        if (request.getResultValue() == null
                || request.getResultValue()
                .isBlank()) {

            throw new RuntimeException(
                    "El valor del resultado es obligatorio."
            );
        }

        if (request.getResultValue()
                .trim()
                .length() > 500) {

            throw new RuntimeException(
                    "El valor del resultado no puede exceder los 500 caracteres."
            );
        }

        if (request.getUnit() != null
                && request.getUnit()
                .trim()
                .length() > 50) {

            throw new RuntimeException(
                    "La unidad no puede exceder los 50 caracteres."
            );
        }

        if (request.getNotes() != null
                && request.getNotes()
                .trim()
                .length() > 1000) {

            throw new RuntimeException(
                    "Las notas del resultado no pueden exceder los 1000 caracteres."
            );
        }
    }

    private LaboratoryOrderStatus parseOptionalStatus(
            String status
    ) {
        if (status == null
                || status.isBlank()
                || status.equalsIgnoreCase("TODOS")) {

            return null;
        }

        try {
            return LaboratoryOrderStatus.valueOf(
                    status.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException exception) {
            throw new RuntimeException(
                    "El estado de orden no es válido. "
                            + "Use PENDIENTE_DE_PAGO, EN_PROCESO, "
                            + "COMPLETADA o CANCELADA."
            );
        }
    }

    private String normalizeFilter(
            String value
    ) {
        if (value == null
                || value.isBlank()) {

            return null;
        }

        String cleanValue =
                value.trim();

        if (cleanValue.length() > 100) {
            throw new RuntimeException(
                    "Los filtros no pueden exceder los 100 caracteres."
            );
        }

        return cleanValue;
    }

    private boolean containsIgnoreCase(
            String original,
            String searchValue
    ) {
        if (original == null) {
            return false;
        }

        return original
                .toLowerCase(Locale.ROOT)
                .contains(
                        searchValue
                                .toLowerCase(
                                        Locale.ROOT
                                )
                );
    }

    private String normalizeOptionalText(
            String value,
            int maximumLength,
            String errorMessage
    ) {
        if (value == null
                || value.isBlank()) {

            return null;
        }

        String cleanValue =
                value.trim();

        if (cleanValue.length()
                > maximumLength) {

            throw new RuntimeException(
                    errorMessage
            );
        }

        return cleanValue;
    }

    private LaboratoryOrderSummaryResponse
    toSummaryResponse(
            LaboratoryOrder order
    ) {
        int totalTests =
                order.getItems().size();

        int publishedTests =
                (int) order.getItems()
                        .stream()
                        .filter(item ->
                                Boolean.TRUE.equals(
                                        item.getPublished()
                                )
                        )
                        .count();

        return LaboratoryOrderSummaryResponse.builder()
                .orderId(order.getId())
                .orderNumber(
                        order.getOrderNumber()
                )
                .patientName(
                        order.getPatient()
                                .getFullName()
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
                .status(
                        order.getStatus()
                                .name()
                )
                .totalAmount(
                        order.getTotalAmount()
                )
                .externalOrder(
                        order.getExternalOrder()
                )
                .totalTests(totalTests)
                .publishedTests(
                        publishedTests
                )
                .createdAt(
                        order.getCreatedAt()
                                .toString()
                )
                .build();
    }

    private LaboratoryOrderResponse toOrderResponse(
            LaboratoryOrder order,
            String message
    ) {
        List<LaboratoryOrderItem> currentItems =
                laboratoryOrderItemRepository
                        .findByLaboratoryOrder_IdOrderByIdAsc(
                                order.getId()
                        );

        List<LaboratoryOrderItemResponse> items =
                currentItems.stream()
                        .map(this::toItemResponse)
                        .toList();

        int totalTests =
                items.size();

        int publishedTests =
                (int) items.stream()
                        .filter(item ->
                                Boolean.TRUE.equals(
                                        item.getPublished()
                                )
                        )
                        .count();

        return LaboratoryOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(
                        order.getOrderNumber()
                )
                .appointmentId(
                        order.getAppointment()
                                .getId()
                )
                .consultationId(
                        order.getMedicalConsultation()
                                .getId()
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
                .status(
                        order.getStatus()
                                .name()
                )
                .totalAmount(
                        order.getTotalAmount()
                )
                .currency(CURRENCY)
                .externalOrder(
                        order.getExternalOrder()
                )
                .notes(order.getNotes())
                .createdAt(
                        order.getCreatedAt()
                                .toString()
                )
                .paidAt(
                        order.getPaidAt() != null
                                ? order.getPaidAt()
                                .toString()
                                : null
                )
                .completedAt(
                        order.getCompletedAt() != null
                                ? order.getCompletedAt()
                                .toString()
                                : null
                )
                .totalTests(totalTests)
                .publishedTests(
                        publishedTests
                )
                .allResultsPublished(
                        totalTests > 0
                                && totalTests
                                == publishedTests
                )
                .items(items)
                .message(message)
                .build();
    }

    private LaboratoryOrderItemResponse toItemResponse(
            LaboratoryOrderItem item
    ) {
        LaboratoryTest test =
                item.getLaboratoryTest();

        boolean orderInProcess =
                item.getLaboratoryOrder()
                        .getStatus()
                        == LaboratoryOrderStatus
                        .EN_PROCESO;

        boolean published =
                Boolean.TRUE.equals(
                        item.getPublished()
                );

        return LaboratoryOrderItemResponse.builder()
                .itemId(item.getId())
                .laboratoryTestId(
                        test.getId()
                )
                .testCode(test.getCode())
                .testName(test.getName())
                .referenceRange(
                        test.getReferenceRange()
                )
                .unitPrice(
                        item.getUnitPrice()
                )
                .status(
                        item.getStatus()
                                .name()
                )
                .resultValue(
                        item.getResultValue()
                )
                .resultUnit(
                        item.getResultUnit()
                )
                .resultDate(
                        item.getResultDate() != null
                                ? item.getResultDate()
                                .toString()
                                : null
                )
                .outOfRange(
                        item.getOutOfRange()
                )
                .resultNotes(
                        item.getResultNotes()
                )
                .published(published)
                .resultSavedAt(
                        item.getResultSavedAt() != null
                                ? item.getResultSavedAt()
                                .toString()
                                : null
                )
                .publishedAt(
                        item.getPublishedAt() != null
                                ? item.getPublishedAt()
                                .toString()
                                : null
                )
                .canSaveResult(
                        orderInProcess
                                && !published
                )
                .canPublishResult(
                        orderInProcess
                                && !published
                                && item.getStatus()
                                == LaboratoryOrderItemStatus
                                .RESULTADO_GUARDADO
                )
                .build();
    }
}