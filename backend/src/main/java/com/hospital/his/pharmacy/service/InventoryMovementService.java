package com.hospital.his.pharmacy.service;

import com.hospital.his.pharmacy.dto.InventoryMonthlySummaryItemResponse;
import com.hospital.his.pharmacy.dto.InventoryMonthlySummaryResponse;
import com.hospital.his.pharmacy.dto.InventoryMovementPageResponse;
import com.hospital.his.pharmacy.dto.InventoryMovementResponse;
import com.hospital.his.pharmacy.entity.InventoryMovement;
import com.hospital.his.pharmacy.entity.InventoryMovementType;
import com.hospital.his.pharmacy.entity.PharmacyInventory;
import com.hospital.his.pharmacy.repository.InventoryMovementRepository;
import com.hospital.his.pharmacy.repository.PharmacyInventoryRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.pharmacy.dto.CreateInventoryMovementRequest;
import com.hospital.his.pharmacy.dto.InventoryMovementDetailResponse;
import com.hospital.his.pharmacy.dto.InventoryMovementPreviewResponse;

import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class InventoryMovementService {

    private static final int MAXIMUM_PAGE_SIZE =
            100;

    private final InventoryMovementRepository
            inventoryMovementRepository;

    private final UserRepository
            userRepository;

    private final PharmacyInventoryRepository pharmacyInventoryRepository;


    private final AuditService auditService;


    public InventoryMovementService(
            InventoryMovementRepository
                    inventoryMovementRepository,
            UserRepository userRepository,
            PharmacyInventoryRepository
                    pharmacyInventoryRepository,


            AuditService auditService
    ) {
        this.inventoryMovementRepository =
                inventoryMovementRepository;

        this.pharmacyInventoryRepository =
                pharmacyInventoryRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /**
     * Registra un movimiento persistente.
     *
     * Este método debe ejecutarse dentro de la misma
     * transacción que modifica el inventario.
     */
    @Transactional
    public InventoryMovement registerMovement(
            PharmacyInventory inventory,
            InventoryMovementType movementType,
            Integer previousStock,
            Integer quantity,
            Integer resultingStock,
            String reason,
            String referenceType,
            Long referenceId,
            String username
    ) {
        validateMovementData(
                inventory,
                movementType,
                previousStock,
                quantity,
                resultingStock,
                reason,
                username
        );

        LocalDateTime now =
                LocalDateTime.now();

        InventoryMovement movement =
                InventoryMovement.builder()
                        .inventory(inventory)
                        .branch(
                                inventory.getBranch()
                        )
                        .medication(
                                inventory.getMedication()
                        )
                        .movementType(
                                movementType
                        )
                        .previousStock(
                                previousStock
                        )
                        .quantity(
                                quantity
                        )
                        .resultingStock(
                                resultingStock
                        )
                        .reason(
                                reason.trim()
                        )
                        .referenceType(
                                normalizeOptionalText(
                                        referenceType,
                                        50
                                )
                        )
                        .referenceId(
                                referenceId
                        )
                        /*
                         * Los movimientos automáticos existentes
                         * no tienen referencia textual manual.
                         */
                        .referenceNumber(null)
                        /*
                         * Los costos solamente se informarán
                         * desde el registro manual de COMPRA.
                         */
                        .unitCost(null)
                        .totalCost(null)
                        .createdBy(
                                username
                        )
                        .createdAt(
                                now
                        )
                        .active(true)
                        .updatedAt(
                                now
                        )
                        .updatedBy(
                                username
                        )
                        .build();

        return inventoryMovementRepository
                .saveAndFlush(movement);
    }

    /**
     * Lista movimientos de la sucursal asociada
     * con el usuario autenticado.
     *
     * Los filtros son opcionales.
     */
    /**
     * Lista movimientos aplicando el alcance
     * de sucursal del usuario autenticado.
     *
     * FARMACIA:
     * utiliza su sucursal asignada.
     *
     * ADMIN:
     * debe seleccionar una sucursal.
     */
    @Transactional(readOnly = true) public InventoryMovementPageResponse getMyBranchMovements(
            Long requestedBranchId,
            String search,
            String movementType,
            String referenceNumber,
            String createdBy,
            Boolean active,
            LocalDate startDate,
            LocalDate endDate,
            Integer page,
            Integer size,
            String username
    ) {
        User user =
                validatePharmacyOrAdministrator(
                        username
                );

        Long branchId =
                resolveBranchId(
                        user,
                        requestedBranchId
                );

        int safePage =
                page != null
                        ? page
                        : 0;

        int safeSize =
                size != null
                        ? size
                        : 20;

        validatePagination(
                safePage,
                safeSize
        );

        InventoryMovementType parsedType =
                parseOptionalMovementType(
                        movementType
                );

        LocalDateTime startDateTime =
                startDate != null
                        ? startDate.atStartOfDay()
                        : null;

        LocalDateTime endDateTime =
                endDate != null
                        ? endDate
                        .plusDays(1)
                        .atStartOfDay()
                        : null;

        if (
                startDate != null &&
                        endDate != null &&
                        endDate.isBefore(startDate)
        ) {
            throw new RuntimeException(
                    "La fecha final no puede ser anterior "
                            + "a la fecha inicial."
            );
        }

        String cleanSearch =
                normalizeFilterText(
                        search,
                        200,
                        "El criterio de búsqueda"
                );

        String cleanReferenceNumber =
                normalizeFilterText(
                        referenceNumber,
                        100,
                        "La referencia"
                );

        String cleanCreatedBy =
                normalizeFilterText(
                        createdBy,
                        100,
                        "El usuario"
                );

        Specification<InventoryMovement>
                specification =
                buildSpecification(
                        branchId,
                        cleanSearch,
                        parsedType,
                        cleanReferenceNumber,
                        cleanCreatedBy,
                        active,
                        startDateTime,
                        endDateTime
                );

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Page<InventoryMovement> result =
                inventoryMovementRepository
                        .findAll(
                                specification,
                                pageable
                        );

        return toPageResponse(
                result
        );
    }

    /**
     * Genera el resumen mensual de los movimientos
     * de la sucursal del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public InventoryMonthlySummaryResponse
    getMonthlySummary(
            Long requestedBranchId,
            Integer year,
            Integer month,
            String username
    ) {
        User user =
                validatePharmacyOrAdministrator(
                        username
                );

        Long branchId =
                resolveBranchId(
                        user,
                        requestedBranchId
                );

        validateYearAndMonth(
                year,
                month
        );

        YearMonth period =
                YearMonth.of(
                        year,
                        month
                );

        LocalDateTime start =
                period.atDay(1)
                        .atStartOfDay();

        LocalDateTime end =
                period.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        List<InventoryMovement> movements =
                inventoryMovementRepository
                        .findByBranch_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
                                branchId,
                                start,
                                end
                        )
                        .stream()
                        .filter(
                                movement ->
                                        Boolean.TRUE.equals(
                                                movement.getActive()
                                        )
                        )
                        .toList();

        Map<Long, MonthlyAccumulator>
                accumulators =
                new LinkedHashMap<>();

        for (
                InventoryMovement movement
                : movements
        ) {
            Long medicationId =
                    movement
                            .getMedication()
                            .getId();

            MonthlyAccumulator accumulator =
                    accumulators.computeIfAbsent(
                            medicationId,
                            ignored ->
                                    new MonthlyAccumulator(
                                            movement
                                    )
                    );

            accumulator.add(
                    movement
            );
        }

        List<InventoryMonthlySummaryItemResponse>
                items =
                accumulators
                        .values()
                        .stream()
                        .map(
                                MonthlyAccumulator
                                        ::toResponse
                        )
                        .sorted(
                                Comparator.comparing(
                                        InventoryMonthlySummaryItemResponse
                                                ::getMedicationName,
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        int totalEntries =
                items.stream()
                        .mapToInt(
                                InventoryMonthlySummaryItemResponse
                                        ::getTotalEntries
                        )
                        .sum();

        int totalOutputs =
                items.stream()
                        .mapToInt(item ->
                                item.getTotalManualOutputs()
                                        + item.getTotalNegativeAdjustments()
                        )
                        .sum();

        int totalDispensed =
                items.stream()
                        .mapToInt(
                                InventoryMonthlySummaryItemResponse
                                        ::getTotalDispensed
                        )
                        .sum();

        return InventoryMonthlySummaryResponse
                .builder()
                .branchId(
                        user.getBranch().getId()
                )
                .branchName(
                        user.getBranch().getName()
                )
                .year(year)
                .month(month)
                .periodStart(
                        start.toString()
                )
                .periodEnd(
                        end.minusNanos(1)
                                .toString()
                )
                .totalMovements(
                        movements.size()
                )
                .totalEntries(
                        totalEntries
                )
                .totalOutputs(
                        totalOutputs
                )
                .totalDispensed(
                        totalDispensed
                )
                .medications(items)
                .build();
    }

    /**
     * Calcula el efecto de un movimiento sin modificar
     * el inventario ni crear una fila de bitácora.
     */
    @Transactional(readOnly = true)
    public InventoryMovementPreviewResponse
    previewManualMovement(
            Long requestedBranchId,
            Long medicationId,
            String movementTypeValue,
            Integer quantity,
            BigDecimal unitCost,
            String username
    ) {
        User user =
                findActiveUser(
                        username
                );

        Long branchId =
                resolveBranchId(
                        user,
                        requestedBranchId
                );

        InventoryMovementType movementType =
                parseManualMovementType(
                        movementTypeValue
                );

        validatePositiveQuantity(
                quantity
        );

        validateUnitCost(
                movementType,
                unitCost
        );

        PharmacyInventory inventory =
                pharmacyInventoryRepository
                        .findByBranch_IdAndMedication_Id(
                                branchId,
                                medicationId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "El medicamento no tiene un registro de inventario en la sucursal seleccionada."
                                )
                        );

        validateActiveInventory(
                inventory
        );

        int currentStock =
                inventory.getCurrentStock();

        int signedQuantity =
                movementType.isOutput()
                        ? -quantity
                        : quantity;

        int projectedStock =
                currentStock +
                        signedQuantity;

        if (projectedStock < 0) {
            throw new RuntimeException(
                    "Stock insuficiente. Disponible: "
                            + currentStock
                            + ", solicitado: "
                            + quantity
                            + "."
            );
        }

        BigDecimal normalizedUnitCost =
                movementType.requiresUnitCost()
                        ? normalizeMoney(
                        unitCost
                )
                        : null;

        BigDecimal totalCost =
                normalizedUnitCost != null
                        ? normalizedUnitCost
                        .multiply(
                                BigDecimal.valueOf(
                                        quantity
                                )
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                        : null;

        return InventoryMovementPreviewResponse
                .builder()
                .inventoryId(
                        inventory.getId()
                )
                .branchId(
                        inventory
                                .getBranch()
                                .getId()
                )
                .branchName(
                        inventory
                                .getBranch()
                                .getName()
                )
                .medicationId(
                        inventory
                                .getMedication()
                                .getId()
                )
                .medicationCode(
                        inventory
                                .getMedication()
                                .getCode()
                )
                .medicationName(
                        inventory
                                .getMedication()
                                .getName()
                )
                .medicationUnit(
                        inventory
                                .getMedication()
                                .getUnit()
                )
                .movementType(
                        movementType.name()
                )
                .operationType(
                        movementType.isEntry()
                                ? "ENTRY"
                                : "OUTPUT"
                )
                .currentStock(
                        currentStock
                )
                .minimumStock(
                        inventory.getMinimumStock()
                )
                .quantity(
                        quantity
                )
                .signedQuantity(
                        signedQuantity
                )
                .projectedStock(
                        projectedStock
                )
                .lowStockWarning(
                        projectedStock <=
                                inventory.getMinimumStock()
                )
                .outOfStockWarning(
                        projectedStock == 0
                )
                .unitCost(
                        normalizedUnitCost
                )
                .totalCost(
                        totalCost
                )
                .inventoryVersion(
                        inventory.getVersion()
                )
                .message(
                        "Vista previa calculada correctamente."
                )
                .build();
    }

    /**
     * Registra un movimiento manual y actualiza
     * el inventario dentro de la misma transacción.
     */
    @Transactional
    public InventoryMovementDetailResponse
    createManualMovement(
            CreateInventoryMovementRequest request,
            String username
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del movimiento son obligatorios."
            );
        }

        User user =
                findActiveUser(
                        username
                );

        Long branchId =
                resolveBranchId(
                        user,
                        request.getBranchId()
                );

        InventoryMovementType movementType =
                parseManualMovementType(
                        request.getMovementType()
                );

        validateRoleMovementType(
                user,
                movementType
        );

        validatePositiveQuantity(
                request.getQuantity()
        );

        validateUnitCost(
                movementType,
                request.getUnitCost()
        );

        String normalizedReason =
                validateAndNormalizeReason(
                        movementType,
                        request.getReason()
                );

        String referenceNumber =
                normalizeText(
                        request.getReferenceNumber(),
                        100
                );

        PharmacyInventory inventory =
                pharmacyInventoryRepository
                        .findForUpdateByBranchIdAndMedicationId(
                                branchId,
                                request.getMedicationId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "El medicamento no tiene inventario en la sucursal seleccionada."
                                )
                        );

        validateActiveInventory(
                inventory
        );

        if (
                request.getInventoryVersion() == null ||
                        !request.getInventoryVersion()
                                .equals(
                                        inventory.getVersion()
                                )
        ) {
            throw new RuntimeException(
                    "El inventario fue modificado por otro usuario. Actualice la vista previa antes de continuar."
            );
        }

        int previousStock =
                inventory.getCurrentStock();

        int quantity =
                request.getQuantity();

        int signedQuantity =
                movementType.isOutput()
                        ? -quantity
                        : quantity;

        int resultingStock =
                previousStock +
                        signedQuantity;

        if (resultingStock < 0) {
            throw new RuntimeException(
                    "Stock insuficiente. Disponible: "
                            + previousStock
                            + ", solicitado: "
                            + quantity
                            + "."
            );
        }

        BigDecimal unitCost =
                movementType.requiresUnitCost()
                        ? normalizeMoney(
                        request.getUnitCost()
                )
                        : null;

        BigDecimal totalCost =
                unitCost != null
                        ? unitCost
                        .multiply(
                                BigDecimal.valueOf(
                                        quantity
                                )
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                        : null;

        LocalDateTime now =
                LocalDateTime.now();

        inventory.setCurrentStock(
                resultingStock
        );

        inventory.setUpdatedAt(
                now
        );

        inventory.setUpdatedBy(
                username
        );

        try {
            pharmacyInventoryRepository
                    .saveAndFlush(
                            inventory
                    );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El inventario fue modificado por otro usuario. Recargue la información."
            );
        }

        InventoryMovement movement =
                InventoryMovement
                        .builder()
                        .inventory(
                                inventory
                        )
                        .branch(
                                inventory.getBranch()
                        )
                        .medication(
                                inventory.getMedication()
                        )
                        .movementType(
                                movementType
                        )
                        .previousStock(
                                previousStock
                        )
                        .quantity(
                                quantity
                        )
                        .resultingStock(
                                resultingStock
                        )
                        .unitCost(
                                unitCost
                        )
                        .totalCost(
                                totalCost
                        )
                        .reason(
                                normalizedReason
                        )
                        .referenceType(
                                resolveReferenceType(
                                        movementType
                                )
                        )
                        .referenceId(null)
                        .referenceNumber(
                                referenceNumber
                        )
                        .createdBy(
                                username
                        )
                        .createdAt(
                                now
                        )
                        .active(true)
                        .updatedAt(
                                now
                        )
                        .updatedBy(
                                username
                        )
                        .build();

        movement =
                inventoryMovementRepository
                        .saveAndFlush(
                                movement
                        );

        auditService.log(
                username,
                "CREATE_INVENTORY_MOVEMENT",
                "INVENTORY",
                "Movimiento "
                        + movementType.name()
                        + " registrado para "
                        + inventory
                        .getMedication()
                        .getCode()
                        + ". Cantidad: "
                        + quantity
                        + ". Stock anterior: "
                        + previousStock
                        + ". Stock resultante: "
                        + resultingStock
                        + "."
        );

        return toDetailResponse(
                movement
        );
    }

    /**
     * Obtiene el detalle de un movimiento respetando
     * el alcance de sucursal del usuario.
     */
    @Transactional(readOnly = true)
    public InventoryMovementDetailResponse
    getMovementDetail(
            Long movementId,
            String username
    ) {
        if (movementId == null) {
            throw new RuntimeException(
                    "Debe indicar el movimiento."
            );
        }

        User user =
                findActiveUser(
                        username
                );

        InventoryMovement movement =
                inventoryMovementRepository
                        .findDetailById(
                                movementId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Movimiento de inventario no encontrado."
                                )
                        );

        if (
                !isAdmin(user) &&
                        !movement
                                .getBranch()
                                .getId()
                                .equals(
                                        user.getBranch().getId()
                                )
        ) {
            throw new RuntimeException(
                    "No tiene acceso al movimiento solicitado."
            );
        }

        return toDetailResponse(
                movement
        );
    }

    /**
     * Activa o desactiva lógicamente un movimiento.
     *
     * Esta operación no altera el inventario y no
     * vuelve a aplicar ni revierte la cantidad.
     *
     * Solamente modifica el estado administrativo
     * del registro histórico.
     */
    @Transactional
    public InventoryMovementDetailResponse
    changeMovementStatus(
            Long movementId,
            Boolean active,
            Long version,
            String username
    ) {
        if (movementId == null) {
            throw new RuntimeException(
                    "Debe indicar el movimiento."
            );
        }

        if (active == null) {
            throw new RuntimeException(
                    "Debe indicar el nuevo estado del movimiento."
            );
        }

        if (version == null) {
            throw new RuntimeException(
                    "La versión del movimiento es obligatoria."
            );
        }

        User user =
                findActiveUser(
                        username
                );

        if (!isAdmin(user)) {
            throw new RuntimeException(
                    "Solamente Administración puede cambiar "
                            + "el estado de los movimientos."
            );
        }

        InventoryMovement movement =
                inventoryMovementRepository
                        .findDetailById(
                                movementId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Movimiento de inventario no encontrado."
                                )
                        );

        if (
                movement.getVersion() == null ||
                        !movement.getVersion()
                                .equals(version)
        ) {
            throw new RuntimeException(
                    "El movimiento fue modificado por otro usuario. "
                            + "Actualice la información antes de continuar."
            );
        }

        if (
                Boolean.TRUE.equals(
                        movement.getActive()
                ) ==
                        Boolean.TRUE.equals(active)
        ) {
            throw new RuntimeException(
                    active
                            ? "El movimiento ya se encuentra activo."
                            : "El movimiento ya se encuentra inactivo."
            );
        }

        movement.setActive(
                active
        );

        movement.setUpdatedAt(
                LocalDateTime.now()
        );

        movement.setUpdatedBy(
                username
        );

        movement =
                inventoryMovementRepository
                        .saveAndFlush(
                                movement
                        );

        auditService.log(
                username,
                active
                        ? "REACTIVATE_INVENTORY_MOVEMENT"
                        : "DEACTIVATE_INVENTORY_MOVEMENT",
                "INVENTORY",
                (
                        active
                                ? "Movimiento reactivado. "
                                : "Movimiento desactivado. "
                )
                        + "ID: "
                        + movement.getId()
                        + ". Tipo: "
                        + movement
                        .getMovementType()
                        .name()
                        + ". Medicamento: "
                        + movement
                        .getMedication()
                        .getCode()
                        + ". Esta operación no modificó el stock."
        );

        return toDetailResponse(
                movement
        );
    }

    /**
     * Crea los filtros dinámicos.
     */
    /**
     * Construye filtros dinámicos de bitácora.
     */
    private Specification<InventoryMovement>
    buildSpecification(
            Long branchId,
            String search,
            InventoryMovementType movementType,
            String referenceNumber,
            String createdBy,
            Boolean active,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return (
                root,
                query,
                criteriaBuilder
        ) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            predicates.add(
                    criteriaBuilder.equal(
                            root.get("branch")
                                    .get("id"),
                            branchId
                    )
            );

            if (
                    search != null &&
                            !search.isBlank()
            ) {
                String pattern =
                        "%"
                                + search.toLowerCase(
                                Locale.ROOT
                        )
                                + "%";

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get(
                                                        "medication"
                                                ).get("code")
                                        ),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get(
                                                        "medication"
                                                ).get("name")
                                        ),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get(
                                                        "medication"
                                                ).get(
                                                        "activeIngredient"
                                                )
                                        ),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("reason")
                                        ),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("createdBy")
                                        ),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get(
                                                        "referenceNumber"
                                                )
                                        ),
                                        pattern
                                )
                        )
                );
            }

            if (movementType != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("movementType"),
                                movementType
                        )
                );
            }

            if (
                    referenceNumber != null &&
                            !referenceNumber.isBlank()
            ) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get(
                                                "referenceNumber"
                                        )
                                ),
                                "%"
                                        + referenceNumber
                                        .toLowerCase(
                                                Locale.ROOT
                                        )
                                        + "%"
                        )
                );
            }

            if (
                    createdBy != null &&
                            !createdBy.isBlank()
            ) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("createdBy")
                                ),
                                "%"
                                        + createdBy
                                        .toLowerCase(
                                                Locale.ROOT
                                        )
                                        + "%"
                        )
                );
            }

            if (active != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("active"),
                                active
                        )
                );
            }

            if (startDate != null) {
                predicates.add(
                        criteriaBuilder
                                .greaterThanOrEqualTo(
                                        root.get("createdAt"),
                                        startDate
                                )
                );
            }

            if (endDate != null) {
                predicates.add(
                        criteriaBuilder.lessThan(
                                root.get("createdAt"),
                                endDate
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }

    private User validatePharmacyOrAdministrator(
            String username
    ) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al usuario autenticado."
            );
        }

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Usuario autenticado no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        user.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del usuario se encuentra inactiva."
            );
        }

        if (
                user.getRole() == null ||
                        !Boolean.TRUE.equals(
                                user.getRole().getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El usuario no tiene un rol activo."
            );
        }

        String roleName =
                normalizeRole(
                        user.getRole().getName()
                );

        if (
                !"FARMACIA".equals(roleName) &&
                        !"ADMIN".equals(roleName)
        ) {
            throw new RuntimeException(
                    "Solamente Farmacia o Administración "
                            + "pueden consultar movimientos."
            );
        }

        return user;
    }

    private void validateMovementData(
            PharmacyInventory inventory,
            InventoryMovementType movementType,
            Integer previousStock,
            Integer quantity,
            Integer resultingStock,
            String reason,
            String username
    ) {
        if (inventory == null) {
            throw new RuntimeException(
                    "El inventario es obligatorio."
            );
        }

        if (
                inventory.getBranch() == null ||
                        inventory.getMedication() == null
        ) {
            throw new RuntimeException(
                    "El inventario no tiene sede "
                            + "o medicamento asociado."
            );
        }

        if (movementType == null) {
            throw new RuntimeException(
                    "El tipo de movimiento es obligatorio."
            );
        }

        if (
                previousStock == null ||
                        previousStock < 0
        ) {
            throw new RuntimeException(
                    "El stock anterior no puede ser negativo."
            );
        }

        if (
                quantity == null ||
                        quantity < 0
        ) {
            throw new RuntimeException(
                    "La cantidad del movimiento no puede ser negativa."
            );
        }

        if (
                resultingStock == null ||
                        resultingStock < 0
        ) {
            throw new RuntimeException(
                    "El stock resultante no puede ser negativo."
            );
        }

        if (
                reason == null ||
                        reason.isBlank()
        ) {
            throw new RuntimeException(
                    "El motivo del movimiento es obligatorio."
            );
        }

        if (reason.trim().length() > 500) {
            throw new RuntimeException(
                    "El motivo no puede exceder los 500 caracteres."
            );
        }

        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "El usuario responsable es obligatorio."
            );
        }
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new RuntimeException(
                    "El número de página no puede ser negativo."
            );
        }

        if (
                size <= 0 ||
                        size > MAXIMUM_PAGE_SIZE
        ) {
            throw new RuntimeException(
                    "El tamaño de página debe estar entre "
                            + "1 y 100 registros."
            );
        }
    }

    private void validateYearAndMonth(
            Integer year,
            Integer month
    ) {
        if (
                year == null ||
                        year < 2000 ||
                        year > 2100
        ) {
            throw new RuntimeException(
                    "El año debe estar entre 2000 y 2100."
            );
        }

        if (
                month == null ||
                        month < 1 ||
                        month > 12
        ) {
            throw new RuntimeException(
                    "El mes debe estar entre 1 y 12."
            );
        }
    }

    private InventoryMovementType
    parseOptionalMovementType(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        try {
            return InventoryMovementType
                    .valueOf(
                            value.trim()
                                    .toUpperCase(
                                            Locale.ROOT
                                    )
                    );

        } catch (
                IllegalArgumentException exception
        ) {
            throw new RuntimeException(
                    "El tipo de movimiento no es válido."
            );
        }
    }

    private String normalizeOptionalText(
            String value,
            int maximumLength
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        String cleanValue =
                value.trim();

        if (
                cleanValue.length()
                        > maximumLength
        ) {
            throw new RuntimeException(
                    "El valor no puede exceder los "
                            + maximumLength
                            + " caracteres."
            );
        }

        return cleanValue;
    }

    /**
     * Normaliza un filtro opcional y valida
     * su longitud máxima.
     */
    private String normalizeFilterText(
            String value,
            int maximumLength,
            String fieldName
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return "";
        }

        String normalized =
                value.trim();

        if (
                normalized.length() >
                        maximumLength
        ) {
            throw new RuntimeException(
                    fieldName
                            + " no puede exceder los "
                            + maximumLength
                            + " caracteres."
            );
        }

        return normalized;
    }


    private String normalizeRole(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private InventoryMovementPageResponse
    toPageResponse(
            Page<InventoryMovement> page
    ) {
        return InventoryMovementPageResponse
                .builder()
                .content(
                        page.getContent()
                                .stream()
                                .map(this::toResponse)
                                .toList()
                )
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(
                        page.getTotalElements()
                )
                .totalPages(
                        page.getTotalPages()
                )
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    private InventoryMovementResponse toResponse(
            InventoryMovement movement
    ) {
        return InventoryMovementResponse
                .builder()
                .movementId(
                        movement.getId()
                )
                .inventoryId(
                        movement.getInventory()
                                .getId()
                )
                .branchId(
                        movement.getBranch()
                                .getId()
                )
                .branchName(
                        movement.getBranch()
                                .getName()
                )
                .medicationId(
                        movement.getMedication()
                                .getId()
                )
                .medicationCode(
                        movement.getMedication()
                                .getCode()
                )
                .medicationName(
                        movement.getMedication()
                                .getName()
                )
                .medicationUnit(
                        movement.getMedication()
                                .getUnit()
                )
                .movementType(
                        movement.getMovementType()
                                .name()
                )
                .previousStock(
                        movement.getPreviousStock()
                )
                .quantity(
                        movement.getQuantity()
                )
                .signedQuantity(
                        getSignedQuantity(
                                movement
                        )
                )
                .resultingStock(
                        movement.getResultingStock()
                )
                .unitCost(
                        movement.getUnitCost()
                )
                .totalCost(
                        movement.getTotalCost()
                )
                .reason(
                        movement.getReason()
                )
                .referenceType(
                        movement.getReferenceType()
                )
                .referenceId(
                        movement.getReferenceId()
                )
                .referenceNumber(
                        movement.getReferenceNumber()
                )
                .createdBy(
                        movement.getCreatedBy()
                )
                .createdAt(
                        movement.getCreatedAt()
                                .toString()
                )
                .active(
                        movement.getActive()
                )
                .updatedAt(
                        movement.getUpdatedAt() != null
                                ? movement
                                .getUpdatedAt()
                                .toString()
                                : null
                )
                .updatedBy(
                        movement.getUpdatedBy()
                )
                .version(
                        movement.getVersion()
                )
                .build();
    }

    /**
     * Devuelve la cantidad con signo según
     * la dirección del movimiento.
     */
    private int getSignedQuantity(
            InventoryMovement movement
    ) {
        if (
                movement == null ||
                        movement.getMovementType() == null
        ) {
            return 0;
        }

        if (
                movement
                        .getMovementType()
                        .isOutput()
        ) {
            return -movement.getQuantity();
        }

        return movement.getQuantity();
    }

    /**
     * Acumulador utilizado para el reporte mensual.
     */
    private static class MonthlyAccumulator {

        private final Long medicationId;
        private final String medicationCode;
        private final String medicationName;
        private final String medicationUnit;

        private Integer initialStock;
        private Integer finalStock;

        private int entries;
        private int manualOutputs;
        private int dispensed;
        private int positiveAdjustments;
        private int negativeAdjustments;
        private int count;

        MonthlyAccumulator(
                InventoryMovement firstMovement
        ) {
            this.medicationId =
                    firstMovement
                            .getMedication()
                            .getId();

            this.medicationCode =
                    firstMovement
                            .getMedication()
                            .getCode();

            this.medicationName =
                    firstMovement
                            .getMedication()
                            .getName();

            this.medicationUnit =
                    firstMovement
                            .getMedication()
                            .getUnit();

            this.initialStock =
                    firstMovement
                            .getPreviousStock();

            this.finalStock =
                    firstMovement
                            .getResultingStock();
        }

        void add(
                InventoryMovement movement
        ) {
            if (count == 0) {
                initialStock =
                        movement
                                .getPreviousStock();
            }

            switch (
                    movement.getMovementType()
            ) {
                case STOCK_INICIAL,
                     ENTRADA,
                     COMPRA,
                     DEVOLUCION,
                     REACTIVACION ->
                        entries +=
                                movement.getQuantity();

                case SALIDA,
                     VENTA,
                     RECLAMO ->
                        manualOutputs +=
                                movement.getQuantity();

                case AJUSTE_POSITIVO ->
                        positiveAdjustments +=
                                movement.getQuantity();

                case AJUSTE_NEGATIVO ->
                        negativeAdjustments +=
                                movement.getQuantity();

                case DESPACHO ->
                        dispensed +=
                                movement.getQuantity();
            }

            finalStock =
                    movement.getResultingStock();

            count++;
        }

        InventoryMonthlySummaryItemResponse
        toResponse() {
            return InventoryMonthlySummaryItemResponse
                    .builder()
                    .medicationId(
                            medicationId
                    )
                    .medicationCode(
                            medicationCode
                    )
                    .medicationName(
                            medicationName
                    )
                    .medicationUnit(
                            medicationUnit
                    )
                    .initialStock(
                            initialStock
                    )
                    .totalEntries(
                            entries
                    )
                    .totalManualOutputs(
                            manualOutputs
                    )
                    .totalDispensed(
                            dispensed
                    )
                    .totalPositiveAdjustments(
                            positiveAdjustments
                    )
                    .totalNegativeAdjustments(
                            negativeAdjustments
                    )
                    .finalStock(
                            finalStock
                    )
                    .movementCount(
                            count
                    )
                    .build();
        }
    }

    private User findActiveUser(
            String username
    ) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al usuario autenticado."
            );
        }

        User user =
                userRepository
                        .findByUsername(
                                username
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Usuario autenticado no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        user.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del usuario está inactiva."
            );
        }

        if (
                user.getRole() == null ||
                        !Boolean.TRUE.equals(
                                user.getRole().getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El usuario no tiene un rol activo."
            );
        }

        return user;
    }

    private Long resolveBranchId(
            User user,
            Long requestedBranchId
    ) {
        if (isAdmin(user)) {
            if (requestedBranchId == null) {
                throw new RuntimeException(
                        "Debe seleccionar una sucursal."
                );
            }

            return requestedBranchId;
        }

        if (!isPharmacy(user)) {
            throw new RuntimeException(
                    "El usuario no está autorizado para registrar movimientos de inventario."
            );
        }

        if (
                user.getBranch() == null ||
                        user.getBranch().getId() == null
        ) {
            throw new RuntimeException(
                    "El usuario de Farmacia no tiene una sucursal asignada."
            );
        }

        return user
                .getBranch()
                .getId();
    }

    private InventoryMovementType
    parseManualMovementType(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    "Debe seleccionar el tipo de movimiento."
            );
        }

        InventoryMovementType movementType;

        try {
            movementType =
                    InventoryMovementType
                            .valueOf(
                                    value.trim()
                                            .toUpperCase(
                                                    Locale.ROOT
                                            )
                            );

        } catch (
                IllegalArgumentException exception
        ) {
            throw new RuntimeException(
                    "El tipo de movimiento no es válido."
            );
        }

        if (!movementType.isManual()) {
            throw new RuntimeException(
                    "El tipo "
                            + movementType.name()
                            + " no puede registrarse manualmente."
            );
        }

        return movementType;
    }

    private void validateRoleMovementType(
            User user,
            InventoryMovementType movementType
    ) {
        if (
                isAdmin(user) &&
                        movementType !=
                                InventoryMovementType
                                        .AJUSTE_POSITIVO &&
                        movementType !=
                                InventoryMovementType
                                        .AJUSTE_NEGATIVO
        ) {
            throw new RuntimeException(
                    "El rol ADMIN solamente puede registrar ajustes positivos o negativos."
            );
        }
    }

    private void validatePositiveQuantity(
            Integer quantity
    ) {
        if (
                quantity == null ||
                        quantity <= 0
        ) {
            throw new RuntimeException(
                    "La cantidad debe ser un número entero positivo."
            );
        }
    }

    /**
     * Valida que el registro de inventario exista,
     * esté activo y tenga valores de stock válidos.
     *
     * Los movimientos manuales no pueden realizarse
     * sobre un inventario inactivo.
     */
    private void validateActiveInventory(
            PharmacyInventory inventory
    ) {
        if (inventory == null) {
            throw new RuntimeException(
                    "El registro de inventario no existe."
            );
        }

        if (
                !Boolean.TRUE.equals(
                        inventory.getActive()
                )
        ) {
            throw new RuntimeException(
                    "El registro de inventario se encuentra inactivo."
            );
        }

        if (inventory.getCurrentStock() == null) {
            throw new RuntimeException(
                    "El inventario no tiene un stock actual válido."
            );
        }

        if (inventory.getCurrentStock() < 0) {
            throw new RuntimeException(
                    "El stock actual del inventario no puede ser negativo."
            );
        }

        if (inventory.getMinimumStock() == null) {
            throw new RuntimeException(
                    "El inventario no tiene un stock mínimo configurado."
            );
        }

        if (inventory.getMinimumStock() < 0) {
            throw new RuntimeException(
                    "El stock mínimo del inventario no puede ser negativo."
            );
        }

        if (inventory.getBranch() == null) {
            throw new RuntimeException(
                    "El inventario no tiene una sucursal asociada."
            );
        }

        if (
                inventory.getBranch().getActive() != null &&
                        !Boolean.TRUE.equals(
                                inventory.getBranch().getActive()
                        )
        ) {
            throw new RuntimeException(
                    "La sucursal asociada con el inventario se encuentra inactiva."
            );
        }

        if (inventory.getMedication() == null) {
            throw new RuntimeException(
                    "El inventario no tiene un medicamento asociado."
            );
        }

        if (
                inventory.getMedication().getActive() != null &&
                        !Boolean.TRUE.equals(
                                inventory.getMedication().getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El medicamento asociado con el inventario se encuentra inactivo."
            );
        }
    }

    private void validateUnitCost(
            InventoryMovementType movementType,
            BigDecimal unitCost
    ) {
        if (
                movementType.requiresUnitCost() &&
                        unitCost == null
        ) {
            throw new RuntimeException(
                    "El costo unitario es obligatorio para compras."
            );
        }

        if (unitCost == null) {
            return;
        }

        if (
                unitCost.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {
            throw new RuntimeException(
                    "El costo unitario debe ser mayor a 0."
            );
        }

        if (
                unitCost.scale() > 2
        ) {
            throw new RuntimeException(
                    "El costo unitario debe tener máximo 2 decimales."
            );
        }
    }

    private String validateAndNormalizeReason(
            InventoryMovementType movementType,
            String reason
    ) {
        String normalizedReason =
                reason == null
                        ? ""
                        : reason.trim();

        if (
                movementType ==
                        InventoryMovementType.VENTA
        ) {
            return normalizedReason.isBlank()
                    ? "Venta de medicamento."
                    : normalizedReason;
        }

        if (
                movementType.requiresReason()
        ) {
            if (normalizedReason.isBlank()) {
                throw new RuntimeException(
                        "El motivo es obligatorio para el tipo de movimiento seleccionado."
                );
            }

            if (
                    normalizedReason.length() <
                            10
            ) {
                throw new RuntimeException(
                        "El motivo debe contener al menos 10 caracteres."
                );
            }
        }

        if (
                normalizedReason.length() >
                        500
        ) {
            throw new RuntimeException(
                    "El motivo no puede exceder los 500 caracteres."
            );
        }

        if (normalizedReason.isBlank()) {
            return defaultReason(
                    movementType
            );
        }

        return normalizedReason;
    }

    private String defaultReason(
            InventoryMovementType movementType
    ) {
        return switch (movementType) {
            case COMPRA ->
                    "Compra de medicamento.";

            case DEVOLUCION ->
                    "Devolución de medicamento.";

            case VENTA ->
                    "Venta de medicamento.";

            case RECLAMO ->
                    "Salida por reclamo.";

            case AJUSTE_POSITIVO ->
                    "Ajuste positivo de inventario.";

            case AJUSTE_NEGATIVO ->
                    "Ajuste negativo de inventario.";

            default ->
                    "Movimiento de inventario.";
        };
    }

    private String resolveReferenceType(
            InventoryMovementType movementType
    ) {
        return switch (movementType) {
            case COMPRA ->
                    "PURCHASE_INVOICE";

            case DEVOLUCION ->
                    "RETURN";

            case VENTA ->
                    "SALE";

            case RECLAMO ->
                    "CLAIM";

            case AJUSTE_POSITIVO,
                 AJUSTE_NEGATIVO ->
                    "MANUAL_ADJUSTMENT";

            default ->
                    "INVENTORY_MOVEMENT";
        };
    }

    private BigDecimal normalizeMoney(
            BigDecimal value
    ) {
        if (value == null) {
            return null;
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private String normalizeText(
            String value,
            int maximumLength
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        String normalized =
                value.trim();

        if (
                normalized.length() >
                        maximumLength
        ) {
            throw new RuntimeException(
                    "El texto no puede exceder los "
                            + maximumLength
                            + " caracteres."
            );
        }

        return normalized;
    }

    private boolean isAdmin(
            User user
    ) {
        return hasRole(
                user,
                "ADMIN"
        );
    }

    private boolean isPharmacy(
            User user
    ) {
        return hasRole(
                user,
                "FARMACIA"
        );
    }

    private boolean hasRole(
            User user,
            String expectedRole
    ) {
        return (
                user != null &&
                        user.getRole() != null &&
                        user.getRole().getName() != null &&
                        expectedRole.equalsIgnoreCase(
                                user.getRole()
                                        .getName()
                                        .trim()
                        )
        );
    }

    private InventoryMovementDetailResponse
    toDetailResponse(
            InventoryMovement movement
    ) {
        return InventoryMovementDetailResponse
                .builder()
                .movementId(
                        movement.getId()
                )
                .inventoryId(
                        movement
                                .getInventory()
                                .getId()
                )
                .branchId(
                        movement
                                .getBranch()
                                .getId()
                )
                .branchName(
                        movement
                                .getBranch()
                                .getName()
                )
                .medicationId(
                        movement
                                .getMedication()
                                .getId()
                )
                .medicationCode(
                        movement
                                .getMedication()
                                .getCode()
                )
                .medicationName(
                        movement
                                .getMedication()
                                .getName()
                )
                .activeIngredient(
                        movement
                                .getMedication()
                                .getActiveIngredient()
                )
                .concentration(
                        movement
                                .getMedication()
                                .getConcentration()
                )
                .presentation(
                        movement
                                .getMedication()
                                .getPresentation()
                )
                .medicationUnit(
                        movement
                                .getMedication()
                                .getUnit()
                )
                .movementType(
                        movement
                                .getMovementType()
                                .name()
                )
                .operationType(
                        movement
                                .getMovementType()
                                .isEntry()
                                ? "ENTRY"
                                : "OUTPUT"
                )
                .previousStock(
                        movement.getPreviousStock()
                )
                .quantity(
                        movement.getQuantity()
                )
                .signedQuantity(
                        getSignedQuantity(
                                movement
                        )
                )
                .resultingStock(
                        movement.getResultingStock()
                )
                .unitCost(
                        movement.getUnitCost()
                )
                .totalCost(
                        movement.getTotalCost()
                )
                .reason(
                        movement.getReason()
                )
                .referenceType(
                        movement.getReferenceType()
                )
                .referenceId(
                        movement.getReferenceId()
                )
                .referenceNumber(
                        movement.getReferenceNumber()
                )
                .active(
                        movement.getActive()
                )
                .createdBy(
                        movement.getCreatedBy()
                )
                .createdAt(
                        movement.getCreatedAt() != null
                                ? movement
                                .getCreatedAt()
                                .toString()
                                : null
                )
                .updatedBy(
                        movement.getUpdatedBy()
                )
                .updatedAt(
                        movement.getUpdatedAt() != null
                                ? movement
                                .getUpdatedAt()
                                .toString()
                                : null
                )
                .version(
                        movement.getVersion()
                )
                .build();
    }
}