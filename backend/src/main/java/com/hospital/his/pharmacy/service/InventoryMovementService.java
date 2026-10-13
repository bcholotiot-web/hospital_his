package com.hospital.his.pharmacy.service;

import com.hospital.his.pharmacy.dto.InventoryMonthlySummaryItemResponse;
import com.hospital.his.pharmacy.dto.InventoryMonthlySummaryResponse;
import com.hospital.his.pharmacy.dto.InventoryMovementPageResponse;
import com.hospital.his.pharmacy.dto.InventoryMovementResponse;
import com.hospital.his.pharmacy.entity.InventoryMovement;
import com.hospital.his.pharmacy.entity.InventoryMovementType;
import com.hospital.his.pharmacy.entity.PharmacyInventory;
import com.hospital.his.pharmacy.repository.InventoryMovementRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;

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

    public InventoryMovementService(
            InventoryMovementRepository
                    inventoryMovementRepository,
            UserRepository userRepository
    ) {
        this.inventoryMovementRepository =
                inventoryMovementRepository;

        this.userRepository =
                userRepository;
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
                        .createdBy(
                                username
                        )
                        .createdAt(
                                LocalDateTime.now()
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
    @Transactional(readOnly = true)
    public InventoryMovementPageResponse
    getMyBranchMovements(
            String search,
            String movementType,
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

        if (user.getBranch() == null) {
            throw new RuntimeException(
                    "El usuario no tiene una sucursal asignada."
            );
        }

        int safePage =
                page != null ? page : 0;

        int safeSize =
                size != null ? size : 20;

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
                search != null
                        ? search.trim()
                        : "";

        if (cleanSearch.length() > 200) {
            throw new RuntimeException(
                    "El criterio de búsqueda no puede exceder "
                            + "los 200 caracteres."
            );
        }

        Specification<InventoryMovement>
                specification =
                buildSpecification(
                        user.getBranch().getId(),
                        cleanSearch,
                        parsedType,
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

        return toPageResponse(result);
    }

    /**
     * Genera el resumen mensual de los movimientos
     * de la sucursal del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public InventoryMonthlySummaryResponse
    getMonthlySummary(
            Integer year,
            Integer month,
            String username
    ) {
        User user =
                validatePharmacyOrAdministrator(
                        username
                );

        if (user.getBranch() == null) {
            throw new RuntimeException(
                    "El usuario no tiene una sucursal asignada."
            );
        }

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
                                user.getBranch().getId(),
                                start,
                                end
                        );

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
     * Crea los filtros dinámicos.
     */
    private Specification<InventoryMovement>
    buildSpecification(
            Long branchId,
            String search,
            InventoryMovementType movementType,
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
                                                root.get("reason")
                                        ),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("createdBy")
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
                .reason(
                        movement.getReason()
                )
                .referenceType(
                        movement.getReferenceType()
                )
                .referenceId(
                        movement.getReferenceId()
                )
                .createdBy(
                        movement.getCreatedBy()
                )
                .createdAt(
                        movement.getCreatedAt()
                                .toString()
                )
                .build();
    }

    private int getSignedQuantity(
            InventoryMovement movement
    ) {
        return switch (
                movement.getMovementType()
                ) {
            case SALIDA,
                 AJUSTE_NEGATIVO,
                 DESPACHO ->
                    -movement.getQuantity();

            default ->
                    movement.getQuantity();
        };
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
                     REACTIVACION ->
                        entries +=
                                movement.getQuantity();

                case SALIDA ->
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
}