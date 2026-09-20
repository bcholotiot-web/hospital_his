package com.hospital.his.pharmacy.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.medications.entity.Medication;
import com.hospital.his.medications.repository.MedicationRepository;
import com.hospital.his.pharmacy.dto.CreatePharmacyInventoryRequest;
import com.hospital.his.pharmacy.dto.PharmacyInventoryResponse;
import com.hospital.his.pharmacy.dto.UpdatePharmacyInventoryRequest;
import com.hospital.his.pharmacy.entity.PharmacyInventory;
import com.hospital.his.pharmacy.repository.PharmacyInventoryRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class PharmacyInventoryService {

    private static final String PHARMACY_ROLE =
            "FARMACIA";

    private final PharmacyInventoryRepository
            pharmacyInventoryRepository;

    private final MedicationRepository
            medicationRepository;

    private final UserRepository
            userRepository;

    private final AuditService
            auditService;

    public PharmacyInventoryService(
            PharmacyInventoryRepository
                    pharmacyInventoryRepository,
            MedicationRepository
                    medicationRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.pharmacyInventoryRepository =
                pharmacyInventoryRepository;

        this.medicationRepository =
                medicationRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /**
     * Lista el inventario activo de la sucursal
     * asignada al usuario de Farmacia.
     *
     * La sucursal se obtiene desde el usuario autenticado,
     * no desde un parámetro enviado por el frontend.
     */
    @Transactional(readOnly = true)
    public List<PharmacyInventoryResponse>
    getMyBranchInventory(
            String pharmacyUsername
    ) {
        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        return pharmacyInventoryRepository
                .findByBranch_IdAndActiveTrueOrderByMedication_NameAsc(
                        pharmacyUser
                                .getBranch()
                                .getId()
                )
                .stream()
                .map(inventory ->
                        toResponse(
                                inventory,
                                buildInventoryMessage(
                                        inventory
                                )
                        )
                )
                .toList();
    }

    /**
     * Obtiene un inventario específico.
     *
     * El inventario debe pertenecer a la misma sucursal
     * del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public PharmacyInventoryResponse
    getInventoryById(
            Long inventoryId,
            String pharmacyUsername
    ) {
        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        PharmacyInventory inventory =
                findActiveInventoryForUser(
                        inventoryId,
                        pharmacyUser
                );

        return toResponse(
                inventory,
                buildInventoryMessage(
                        inventory
                )
        );
    }

    /**
     * Lista medicamentos cuyo stock actual es igual
     * o inferior al stock mínimo configurado.
     *
     * La comparación se realiza registro por registro,
     * porque cada medicamento puede tener un stock
     * mínimo diferente.
     */
    @Transactional(readOnly = true)
    public List<PharmacyInventoryResponse>
    getLowStockAlerts(
            String pharmacyUsername
    ) {
        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        return pharmacyInventoryRepository
                .findByBranch_IdAndActiveTrueOrderByMedication_NameAsc(
                        pharmacyUser
                                .getBranch()
                                .getId()
                )
                .stream()
                .filter(this::hasLowStock)
                .map(inventory ->
                        toResponse(
                                inventory,
                                buildLowStockMessage(
                                        inventory
                                )
                        )
                )
                .toList();
    }

    /**
     * Agrega un medicamento al inventario de la sucursal.
     *
     * Si ya existe un registro inactivo para la misma
     * combinación, el registro será reactivado en lugar
     * de crear una fila duplicada.
     */
    @Transactional
    public PharmacyInventoryResponse
    createInventory(
            CreatePharmacyInventoryRequest request,
            String pharmacyUsername
    ) {
        validateCreateRequest(request);

        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        Medication medication =
                medicationRepository
                        .findByIdAndActiveTrue(
                                request
                                        .getMedicationId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "El medicamento no existe "
                                                + "o se encuentra inactivo."
                                )
                        );

        Long branchId =
                pharmacyUser
                        .getBranch()
                        .getId();

        PharmacyInventory existingInventory =
                pharmacyInventoryRepository
                        .findByBranch_IdAndMedication_Id(
                                branchId,
                                medication.getId()
                        )
                        .orElse(null);

        LocalDateTime now =
                LocalDateTime.now();

        PharmacyInventory inventory;

        if (existingInventory != null) {
            /*
             * Si ya está activo, no se permite duplicar.
             */
            if (
                    Boolean.TRUE.equals(
                            existingInventory
                                    .getActive()
                    )
            ) {
                throw new RuntimeException(
                        "El medicamento ya se encuentra registrado "
                                + "en el inventario de esta sucursal."
                );
            }

            /*
             * Reactivar el registro previamente desactivado.
             */
            existingInventory.setCurrentStock(
                    request.getCurrentStock()
            );

            existingInventory.setMinimumStock(
                    request.getMinimumStock()
            );

            existingInventory.setActive(true);
            existingInventory.setUpdatedAt(now);

            existingInventory.setUpdatedBy(
                    pharmacyUser.getUsername()
            );

            inventory =
                    existingInventory;

        } else {
            /*
             * Crear un inventario nuevo.
             */
            inventory =
                    PharmacyInventory.builder()
                            .branch(
                                    pharmacyUser
                                            .getBranch()
                            )
                            .medication(
                                    medication
                            )
                            .currentStock(
                                    request
                                            .getCurrentStock()
                            )
                            .minimumStock(
                                    request
                                            .getMinimumStock()
                            )
                            .active(true)
                            .updatedAt(now)
                            .updatedBy(
                                    pharmacyUser
                                            .getUsername()
                            )
                            .build();
        }

        try {
            inventory =
                    pharmacyInventoryRepository
                            .saveAndFlush(
                                    inventory
                            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "El medicamento ya se encuentra registrado "
                            + "en el inventario de esta sucursal."
            );
        }

        auditService.log(
                pharmacyUser.getUsername(),
                "CREATE_PHARMACY_INVENTORY",
                "PHARMACY",
                "Medicamento "
                        + medication.getCode()
                        + " registrado en el inventario de "
                        + pharmacyUser
                        .getBranch()
                        .getName()
                        + ". Stock inicial: "
                        + inventory.getCurrentStock()
                        + ". Stock mínimo: "
                        + inventory.getMinimumStock()
                        + "."
        );

        return toResponse(
                inventory,
                "Medicamento agregado al inventario correctamente."
        );
    }

    /**
     * Actualiza el stock actual y el stock mínimo.
     *
     * La versión enviada por el cliente debe coincidir
     * con la versión actual del registro.
     */
    @Transactional
    public PharmacyInventoryResponse
    updateInventory(
            Long inventoryId,
            UpdatePharmacyInventoryRequest request,
            String pharmacyUsername
    ) {
        validateUpdateRequest(request);

        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        PharmacyInventory inventory =
                findActiveInventoryForUser(
                        inventoryId,
                        pharmacyUser
                );

        /*
         * Verificación explícita de versión.
         *
         * Hibernate también verificará @Version al guardar,
         * pero esta validación ofrece un mensaje claro.
         */
        if (
                inventory.getVersion() == null ||
                        !inventory.getVersion()
                                .equals(
                                        request.getVersion()
                                )
        ) {
            throw new RuntimeException(
                    "El inventario fue actualizado por otra persona. "
                            + "Recargue la información e intente nuevamente."
            );
        }

        Integer previousStock =
                inventory.getCurrentStock();

        Integer previousMinimum =
                inventory.getMinimumStock();

        inventory.setCurrentStock(
                request.getCurrentStock()
        );

        inventory.setMinimumStock(
                request.getMinimumStock()
        );

        inventory.setUpdatedAt(
                LocalDateTime.now()
        );

        inventory.setUpdatedBy(
                pharmacyUser.getUsername()
        );

        try {
            inventory =
                    pharmacyInventoryRepository
                            .saveAndFlush(
                                    inventory
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El inventario fue actualizado por otra persona. "
                            + "Recargue la información e intente nuevamente."
            );
        }

        auditService.log(
                pharmacyUser.getUsername(),
                "UPDATE_PHARMACY_INVENTORY",
                "PHARMACY",
                "Inventario actualizado para medicamento "
                        + inventory
                        .getMedication()
                        .getCode()
                        + ". Stock anterior: "
                        + previousStock
                        + ". Stock actual: "
                        + inventory
                        .getCurrentStock()
                        + ". Mínimo anterior: "
                        + previousMinimum
                        + ". Mínimo actual: "
                        + inventory
                        .getMinimumStock()
                        + "."
        );

        return toResponse(
                inventory,
                hasLowStock(inventory)
                        ? buildLowStockMessage(
                        inventory
                )
                        : "Inventario actualizado correctamente."
        );
    }

    /**
     * Activa o desactiva lógicamente un registro
     * de inventario.
     *
     * La fila continúa almacenada en PostgreSQL.
     */
    @Transactional
    public PharmacyInventoryResponse
    changeInventoryStatus(
            Long inventoryId,
            Boolean active,
            String pharmacyUsername
    ) {
        if (active == null) {
            throw new RuntimeException(
                    "Debe indicar el estado del inventario."
            );
        }

        User pharmacyUser =
                validatePharmacyUser(
                        pharmacyUsername
                );

        if (inventoryId == null) {
            throw new RuntimeException(
                    "Debe indicar el registro de inventario."
            );
        }

        PharmacyInventory inventory =
                pharmacyInventoryRepository
                        .findByIdAndBranch_Id(
                                inventoryId,
                                pharmacyUser
                                        .getBranch()
                                        .getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Inventario no encontrado "
                                                + "en la sucursal asignada."
                                )
                        );

        inventory.setActive(active);
        inventory.setUpdatedAt(
                LocalDateTime.now()
        );

        inventory.setUpdatedBy(
                pharmacyUser.getUsername()
        );

        inventory =
                pharmacyInventoryRepository
                        .saveAndFlush(inventory);

        auditService.log(
                pharmacyUser.getUsername(),
                active
                        ? "ACTIVATE_PHARMACY_INVENTORY"
                        : "DEACTIVATE_PHARMACY_INVENTORY",
                "PHARMACY",
                "Inventario del medicamento "
                        + inventory
                        .getMedication()
                        .getCode()
                        + " actualizado a estado "
                        + (
                        active
                                ? "ACTIVO."
                                : "INACTIVO."
                )
        );

        return toResponse(
                inventory,
                active
                        ? "Inventario activado correctamente."
                        : "Inventario desactivado correctamente."
        );
    }

    /**
     * Valida que el usuario autenticado pertenezca
     * al módulo de Farmacia.
     *
     * También exige:
     * - Cuenta activa.
     * - Rol activo.
     * - Rol FARMACIA.
     * - Sucursal activa asignada.
     */
    private User validatePharmacyUser(
            String username
    ) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al usuario "
                            + "de farmacia autenticado."
            );
        }

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Usuario de farmacia "
                                                + "no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        user.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del usuario de farmacia "
                            + "se encuentra inactiva."
            );
        }

        if (
                user.getRole() == null ||
                        !Boolean.TRUE.equals(
                                user.getRole()
                                        .getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El usuario no tiene un rol activo asignado."
            );
        }

        String roleName =
                user.getRole()
                        .getName();

        if (
                roleName == null ||
                        !PHARMACY_ROLE.equals(
                                normalizeRole(roleName)
                        )
        ) {
            throw new RuntimeException(
                    "Solamente el personal de farmacia "
                            + "puede realizar esta operación."
            );
        }

        if (
                user.getBranch() == null ||
                        !Boolean.TRUE.equals(
                                user.getBranch()
                                        .getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El usuario de farmacia no tiene "
                            + "una sucursal activa asignada."
            );
        }

        return user;
    }

    /**
     * Normaliza el nombre del rol.
     *
     * FARMACIA y Farmacia se convierten en FARMACIA.
     */
    private String normalizeRole(
            String roleName
    ) {
        return roleName
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Busca un inventario activo restringido
     * a la sucursal del usuario autenticado.
     */
    private PharmacyInventory
    findActiveInventoryForUser(
            Long inventoryId,
            User pharmacyUser
    ) {
        if (inventoryId == null) {
            throw new RuntimeException(
                    "Debe indicar el registro de inventario."
            );
        }

        return pharmacyInventoryRepository
                .findByIdAndBranch_IdAndActiveTrue(
                        inventoryId,
                        pharmacyUser
                                .getBranch()
                                .getId()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Inventario activo no encontrado "
                                        + "en la sucursal asignada."
                        )
                );
    }

    /**
     * Valida el request de creación.
     *
     * Estas validaciones se repiten aunque también
     * existan anotaciones Jakarta Validation.
     */
    private void validateCreateRequest(
            CreatePharmacyInventoryRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del inventario son obligatorios."
            );
        }

        if (
                request.getMedicationId() == null
        ) {
            throw new RuntimeException(
                    "Debe seleccionar un medicamento."
            );
        }

        validateStockValues(
                request.getCurrentStock(),
                request.getMinimumStock()
        );
    }

    /**
     * Valida el request de actualización.
     */
    private void validateUpdateRequest(
            UpdatePharmacyInventoryRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del inventario son obligatorios."
            );
        }

        validateStockValues(
                request.getCurrentStock(),
                request.getMinimumStock()
        );

        if (request.getVersion() == null) {
            throw new RuntimeException(
                    "Debe indicar la versión del inventario."
            );
        }
    }

    /**
     * Garantiza que las existencias nunca tengan
     * valores negativos.
     */
    private void validateStockValues(
            Integer currentStock,
            Integer minimumStock
    ) {
        if (currentStock == null) {
            throw new RuntimeException(
                    "Debe indicar el stock actual."
            );
        }

        if (minimumStock == null) {
            throw new RuntimeException(
                    "Debe indicar el stock mínimo."
            );
        }

        if (currentStock < 0) {
            throw new RuntimeException(
                    "El stock actual no puede ser negativo."
            );
        }

        if (minimumStock < 0) {
            throw new RuntimeException(
                    "El stock mínimo no puede ser negativo."
            );
        }
    }

    /**
     * Determina si el medicamento alcanzó
     * su nivel mínimo.
     */
    private boolean hasLowStock(
            PharmacyInventory inventory
    ) {
        return inventory.getCurrentStock()
                <= inventory.getMinimumStock();
    }

    /**
     * Construye la alerta de stock mínimo.
     */
    private String buildLowStockMessage(
            PharmacyInventory inventory
    ) {
        return "ALERTA: El medicamento "
                + inventory
                .getMedication()
                .getName()
                + " ha alcanzado el nivel de stock mínimo ("
                + inventory.getCurrentStock()
                + " unidades restantes). Se recomienda "
                + "generar una orden de reabastecimiento.";
    }

    /**
     * Construye un mensaje informativo según
     * la existencia actual.
     */
    private String buildInventoryMessage(
            PharmacyInventory inventory
    ) {
        if (inventory.getCurrentStock() == 0) {
            return "Medicamento sin existencias.";
        }

        if (hasLowStock(inventory)) {
            return buildLowStockMessage(
                    inventory
            );
        }

        return "Inventario disponible.";
    }

    /**
     * Convierte la entidad en un DTO seguro.
     *
     * Las relaciones JPA no se envían directamente
     * al frontend.
     */
    private PharmacyInventoryResponse toResponse(
            PharmacyInventory inventory,
            String message
    ) {
        return PharmacyInventoryResponse
                .builder()
                .inventoryId(
                        inventory.getId()
                )
                .branchId(
                        inventory.getBranch()
                                .getId()
                )
                .branchName(
                        inventory.getBranch()
                                .getName()
                )
                .medicationId(
                        inventory.getMedication()
                                .getId()
                )
                .medicationCode(
                        inventory.getMedication()
                                .getCode()
                )
                .medicationName(
                        inventory.getMedication()
                                .getName()
                )
                .activeIngredient(
                        inventory.getMedication()
                                .getActiveIngredient()
                )
                .concentration(
                        inventory.getMedication()
                                .getConcentration()
                )
                .presentation(
                        inventory.getMedication()
                                .getPresentation()
                )
                .unitPrice(
                        inventory.getMedication()
                                .getUnitPrice()
                )
                .controlled(
                        inventory.getMedication()
                                .getControlled()
                )
                .currentStock(
                        inventory.getCurrentStock()
                )
                .minimumStock(
                        inventory.getMinimumStock()
                )
                .outOfStock(
                        inventory.getCurrentStock() == 0
                )
                .available(
                        inventory.getCurrentStock() > 0
                )
                .lowStock(
                        hasLowStock(inventory)
                )
                .active(
                        inventory.getActive()
                )
                .updatedAt(
                        inventory.getUpdatedAt() != null
                                ? inventory
                                .getUpdatedAt()
                                .toString()
                                : null
                )
                .updatedBy(
                        inventory.getUpdatedBy()
                )
                .version(
                        inventory.getVersion()
                )
                .message(message)
                .build();
    }
}