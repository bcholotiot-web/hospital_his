package com.hospital.his.catalogs.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.dto.BranchSpecialtyOptionResponse;
import com.hospital.his.catalogs.dto.BranchSpecialtyPageResponse;
import com.hospital.his.catalogs.dto.BranchSpecialtyResponse;
import com.hospital.his.catalogs.dto.CreateBranchSpecialtyRequest;
import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.entity.BranchSpecialty;
import com.hospital.his.catalogs.entity.Specialty;
import com.hospital.his.catalogs.repository.BranchRepository;
import com.hospital.his.catalogs.repository.BranchSpecialtyRepository;
import com.hospital.his.catalogs.repository.SpecialtyRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BranchSpecialtyService {

    private static final String ADMIN_ROLE =
            "ADMIN";

    private static final int MAXIMUM_PAGE_SIZE =
            100;

    private final BranchSpecialtyRepository
            branchSpecialtyRepository;

    private final BranchRepository
            branchRepository;

    private final SpecialtyRepository
            specialtyRepository;

    private final UserRepository
            userRepository;

    private final AuditService
            auditService;

    public BranchSpecialtyService(
            BranchSpecialtyRepository
                    branchSpecialtyRepository,
            BranchRepository branchRepository,
            SpecialtyRepository specialtyRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.branchSpecialtyRepository =
                branchSpecialtyRepository;

        this.branchRepository =
                branchRepository;

        this.specialtyRepository =
                specialtyRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /**
     * Lista asignaciones con paginación y filtro
     * opcional por ID.
     *
     * El backend limita el tamaño máximo a 100
     * registros para evitar consultas excesivas.
     */
    @Transactional(readOnly = true)
    public BranchSpecialtyPageResponse
    getAssignments(
            Long assignmentId,
            Integer page,
            Integer size,
            String adminUsername
    ) {
        validateAdministrator(
                adminUsername
        );

        int safePage =
                page != null
                        ? page
                        : 0;

        int safeSize =
                size != null
                        ? size
                        : 20;

        if (safePage < 0) {
            throw new RuntimeException(
                    "El número de página no puede ser negativo."
            );
        }

        if (
                safeSize <= 0 ||
                        safeSize > MAXIMUM_PAGE_SIZE
        ) {
            throw new RuntimeException(
                    "El tamaño de página debe estar entre "
                            + "1 y 100 registros."
            );
        }

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "id"
                        )
                );

        Page<BranchSpecialty> result;

        if (assignmentId != null) {
            if (assignmentId <= 0) {
                throw new RuntimeException(
                        "El ID de asignación debe ser "
                                + "mayor que cero."
                );
            }

            result =
                    branchSpecialtyRepository
                            .findById(
                                    assignmentId,
                                    pageable
                            );

        } else {
            result =
                    branchSpecialtyRepository
                            .findAllByOrderByIdDesc(
                                    pageable
                            );
        }

        return toPageResponse(
                result
        );
    }

    /**
     * Crea una relación sede-especialidad.
     *
     * Casos:
     *
     * 1. No existe:
     *    crea un registro.
     *
     * 2. Existe activa:
     *    rechaza la duplicidad.
     *
     * 3. Existe inactiva:
     *    reactiva el registro existente.
     */
    @Transactional
    public BranchSpecialtyResponse
    createAssignment(
            CreateBranchSpecialtyRequest request,
            String adminUsername
    ) {
        User administrator =
                validateAdministrator(
                        adminUsername
                );

        validateCreateRequest(
                request
        );

        Branch branch =
                branchRepository
                        .findByIdAndActiveTrue(
                                request.getBranchId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "La sede no existe "
                                                + "o se encuentra inactiva."
                                )
                        );

        Specialty specialty =
                specialtyRepository
                        .findByIdAndActiveTrue(
                                request.getSpecialtyId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "La especialidad no existe "
                                                + "o se encuentra inactiva."
                                )
                        );

        BranchSpecialty existing =
                branchSpecialtyRepository
                        .findByBranch_IdAndSpecialty_Id(
                                branch.getId(),
                                specialty.getId()
                        )
                        .orElse(null);

        LocalDateTime now =
                LocalDateTime.now();

        if (existing != null) {
            if (
                    Boolean.TRUE.equals(
                            existing.getActive()
                    )
            ) {
                throw new RuntimeException(
                        "Esta combinación de sede y especialidad "
                                + "ya existe en el sistema."
                );
            }

            /*
             * Reactivar una asignación anterior
             * conservando createdAt y createdBy.
             */
            existing.setActive(true);
            existing.setUpdatedAt(now);
            existing.setUpdatedBy(
                    administrator.getUsername()
            );

            try {
                existing =
                        branchSpecialtyRepository
                                .saveAndFlush(
                                        existing
                                );

            } catch (
                    OptimisticLockingFailureException exception
            ) {
                throw new RuntimeException(
                        "La asignación fue modificada por otro "
                                + "usuario. Actualice el listado "
                                + "e intente nuevamente."
                );
            }

            auditService.log(
                    administrator.getUsername(),
                    "REACTIVATE_BRANCH_SPECIALTY",
                    "CATALOGS",
                    "Asignación reactivada. Sede: "
                            + branch.getName()
                            + ". Especialidad: "
                            + specialty.getName()
                            + "."
            );

            return toResponse(
                    existing,
                    "Especialidad reasignada a la sede "
                            + "correctamente."
            );
        }

        BranchSpecialty assignment =
                BranchSpecialty.builder()
                        .branch(branch)
                        .specialty(specialty)
                        .active(true)
                        .createdAt(now)
                        .createdBy(
                                administrator.getUsername()
                        )
                        .updatedAt(now)
                        .updatedBy(
                                administrator.getUsername()
                        )
                        .build();

        try {
            assignment =
                    branchSpecialtyRepository
                            .saveAndFlush(
                                    assignment
                            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "Esta combinación de sede y especialidad "
                            + "ya existe en el sistema."
            );
        }

        auditService.log(
                administrator.getUsername(),
                "CREATE_BRANCH_SPECIALTY",
                "CATALOGS",
                "Asignación creada. Sede: "
                        + branch.getName()
                        + ". Especialidad: "
                        + specialty.getName()
                        + "."
        );

        return toResponse(
                assignment,
                "Especialidad asignada a la sede "
                        + "correctamente."
        );
    }

    /**
     * Desactiva lógicamente una asignación.
     *
     * La fila no se elimina de PostgreSQL para
     * conservar el historial.
     */
    @Transactional
    public BranchSpecialtyResponse
    deactivateAssignment(
            Long assignmentId,
            String adminUsername
    ) {
        User administrator =
                validateAdministrator(
                        adminUsername
                );

        if (assignmentId == null) {
            throw new RuntimeException(
                    "Debe indicar la asignación."
            );
        }

        BranchSpecialty assignment =
                branchSpecialtyRepository
                        .findById(assignmentId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Asignación de sede y "
                                                + "especialidad no encontrada."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        assignment.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La asignación ya se encuentra inactiva."
            );
        }

        assignment.setActive(false);
        assignment.setUpdatedAt(
                LocalDateTime.now()
        );
        assignment.setUpdatedBy(
                administrator.getUsername()
        );

        try {
            assignment =
                    branchSpecialtyRepository
                            .saveAndFlush(
                                    assignment
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "La asignación fue modificada por otro "
                            + "usuario. Actualice el listado "
                            + "e intente nuevamente."
            );
        }

        auditService.log(
                administrator.getUsername(),
                "DEACTIVATE_BRANCH_SPECIALTY",
                "CATALOGS",
                "Asignación desactivada. Sede: "
                        + assignment
                        .getBranch()
                        .getName()
                        + ". Especialidad: "
                        + assignment
                        .getSpecialty()
                        .getName()
                        + "."
        );

        return toResponse(
                assignment,
                "Asignación eliminada correctamente."
        );
    }

    /**
     * Devuelve sedes activas para el formulario
     * administrativo.
     */
    @Transactional(readOnly = true)
    public List<BranchSpecialtyOptionResponse>
    getActiveBranchOptions(
            String adminUsername
    ) {
        validateAdministrator(
                adminUsername
        );

        return branchRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(branch ->
                        BranchSpecialtyOptionResponse
                                .builder()
                                .id(branch.getId())
                                .name(branch.getName())
                                .description(
                                        branch.getAddress()
                                )
                                .build()
                )
                .toList();
    }

    /**
     * Devuelve especialidades activas para
     * el formulario administrativo.
     */
    @Transactional(readOnly = true)
    public List<BranchSpecialtyOptionResponse>
    getActiveSpecialtyOptions(
            String adminUsername
    ) {
        validateAdministrator(
                adminUsername
        );

        return specialtyRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(specialty ->
                        BranchSpecialtyOptionResponse
                                .builder()
                                .id(
                                        specialty.getId()
                                )
                                .name(
                                        specialty.getName()
                                )
                                .description(
                                        specialty
                                                .getDescription()
                                )
                                .build()
                )
                .toList();
    }

    /**
     * Devuelve únicamente las especialidades
     * disponibles en una sede.
     *
     * Este método será utilizado por CU-03.
     */
    @Transactional(readOnly = true)
    public List<BranchSpecialtyOptionResponse>
    getActiveSpecialtiesByBranch(
            Long branchId
    ) {
        if (branchId == null) {
            throw new RuntimeException(
                    "Debe indicar la sede."
            );
        }

        Branch branch =
                branchRepository
                        .findByIdAndActiveTrue(
                                branchId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "La sede no existe "
                                                + "o se encuentra inactiva."
                                )
                        );

        return branchSpecialtyRepository
                .findByBranch_IdAndActiveTrueAndBranch_ActiveTrueAndSpecialty_ActiveTrueOrderBySpecialty_NameAsc(
                        branch.getId()
                )
                .stream()
                .map(assignment ->
                        BranchSpecialtyOptionResponse
                                .builder()
                                .id(
                                        assignment
                                                .getSpecialty()
                                                .getId()
                                )
                                .name(
                                        assignment
                                                .getSpecialty()
                                                .getName()
                                )
                                .description(
                                        assignment
                                                .getSpecialty()
                                                .getDescription()
                                )
                                .build()
                )
                .toList();
    }

    /**
     * Valida que el usuario autenticado sea
     * un administrador activo.
     */
    private User validateAdministrator(
            String username
    ) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al "
                            + "administrador autenticado."
            );
        }

        User administrator =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Administrador no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        administrator.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del administrador "
                            + "se encuentra inactiva."
            );
        }

        if (
                administrator.getRole() == null ||
                        !Boolean.TRUE.equals(
                                administrator
                                        .getRole()
                                        .getActive()
                        ) ||
                        !ADMIN_ROLE.equals(
                                normalizeRole(
                                        administrator
                                                .getRole()
                                                .getName()
                                )
                        )
        ) {
            throw new RuntimeException(
                    "Solamente un administrador puede "
                            + "realizar esta operación."
            );
        }

        return administrator;
    }

    /**
     * Valida los campos obligatorios del request.
     */
    private void validateCreateRequest(
            CreateBranchSpecialtyRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la asignación "
                            + "son obligatorios."
            );
        }

        if (request.getBranchId() == null) {
            throw new RuntimeException(
                    "Debe seleccionar una sede."
            );
        }

        if (request.getSpecialtyId() == null) {
            throw new RuntimeException(
                    "Debe seleccionar una especialidad."
            );
        }
    }

    /**
     * Convierte un Page de Spring en un contrato
     * controlado para el frontend.
     */
    private BranchSpecialtyPageResponse
    toPageResponse(
            Page<BranchSpecialty> page
    ) {
        return BranchSpecialtyPageResponse
                .builder()
                .content(
                        page.getContent()
                                .stream()
                                .map(assignment ->
                                        toResponse(
                                                assignment,
                                                null
                                        )
                                )
                                .toList()
                )
                .page(
                        page.getNumber()
                )
                .size(
                        page.getSize()
                )
                .totalElements(
                        page.getTotalElements()
                )
                .totalPages(
                        page.getTotalPages()
                )
                .first(
                        page.isFirst()
                )
                .last(
                        page.isLast()
                )
                .empty(
                        page.isEmpty()
                )
                .build();
    }

    /**
     * Convierte la entidad en una respuesta segura.
     */
    private BranchSpecialtyResponse toResponse(
            BranchSpecialty assignment,
            String message
    ) {
        return BranchSpecialtyResponse
                .builder()
                .id(
                        assignment.getId()
                )
                .branchId(
                        assignment
                                .getBranch()
                                .getId()
                )
                .branchName(
                        assignment
                                .getBranch()
                                .getName()
                )
                .branchAddress(
                        assignment
                                .getBranch()
                                .getAddress()
                )
                .specialtyId(
                        assignment
                                .getSpecialty()
                                .getId()
                )
                .specialtyName(
                        assignment
                                .getSpecialty()
                                .getName()
                )
                .specialtyDescription(
                        assignment
                                .getSpecialty()
                                .getDescription()
                )
                .active(
                        assignment.getActive()
                )
                .createdAt(
                        assignment
                                .getCreatedAt()
                                .toString()
                )
                .createdBy(
                        assignment.getCreatedBy()
                )
                .updatedAt(
                        assignment
                                .getUpdatedAt()
                                .toString()
                )
                .updatedBy(
                        assignment.getUpdatedBy()
                )
                .version(
                        assignment.getVersion()
                )
                .message(message)
                .build();
    }

    /**
     * Normaliza nombres de rol.
     *
     * Administrador se reconoce como ADMINISTRADOR,
     * mientras que el rol del sistema esperado
     * para seguridad es ADMIN.
     */
    private String normalizeRole(
            String value
    ) {
        if (value == null) {
            return "";
        }

        String normalized =
                Normalizer
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

        /*
         * El sistema actual puede almacenar
         * ADMIN o Administrador.
         */
        return "ADMINISTRADOR".equals(normalized)
                ? "ADMIN"
                : normalized;
    }
}