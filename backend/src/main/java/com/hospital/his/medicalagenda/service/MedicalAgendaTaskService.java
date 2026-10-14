package com.hospital.his.medicalagenda.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.medicalagenda.dto
        .CreateMedicalAgendaTaskRequest;
import com.hospital.his.medicalagenda.dto
        .MedicalAgendaTaskResponse;
import com.hospital.his.medicalagenda.dto
        .UpdateMedicalAgendaTaskRequest;
import com.hospital.his.medicalagenda.entity
        .MedicalAgendaTask;
import com.hospital.his.medicalagenda.entity
        .MedicalAgendaTaskPriority;
import com.hospital.his.medicalagenda.repository
        .MedicalAgendaTaskRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.dao
        .OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation
        .Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Gestiona las tareas y recordatorios
 * del médico autenticado.
 */
@Service
public class MedicalAgendaTaskService {

    private final MedicalAgendaTaskRepository
            taskRepository;

    private final UserRepository
            userRepository;

    private final AuditService
            auditService;

    public MedicalAgendaTaskService(
            MedicalAgendaTaskRepository
                    taskRepository,

            UserRepository
                    userRepository,

            AuditService
                    auditService
    ) {
        this.taskRepository =
                taskRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /**
     * Lista tareas activas del día seleccionado.
     *
     * Filtros válidos:
     *
     * PENDING
     * COMPLETED
     * ALL
     */
    @Transactional(readOnly = true)
    public List<MedicalAgendaTaskResponse>
    getMyTasks(
            LocalDate selectedDate,
            String status,
            String username
    ) {
        User doctor =
                validateDoctor(
                        username
                );

        if (selectedDate == null) {
            throw new RuntimeException(
                    "Debe seleccionar una fecha."
            );
        }

        String normalizedStatus =
                normalizeStatusFilter(
                        status
                );

        LocalDateTime rangeStart =
                selectedDate.atStartOfDay();

        LocalDateTime rangeEnd =
                selectedDate
                        .plusDays(1)
                        .atStartOfDay();

        List<MedicalAgendaTask> tasks =
                switch (
                        normalizedStatus
                        ) {
                    case "PENDING" ->
                            taskRepository
                                    .findPendingTasksWithinRange(
                                            doctor.getId(),
                                            rangeStart,
                                            rangeEnd
                                    );

                    case "COMPLETED" ->
                            taskRepository
                                    .findCompletedTasksWithinRange(
                                            doctor.getId(),
                                            rangeStart,
                                            rangeEnd
                                    );

                    case "ALL" ->
                            taskRepository
                                    .findActiveTasksWithinRange(
                                            doctor.getId(),
                                            rangeStart,
                                            rangeEnd
                                    );

                    default ->
                            throw new RuntimeException(
                                    "El filtro de tareas no es válido."
                            );
                };

        return tasks
                .stream()
                .map(
                        task ->
                                toResponse(
                                        task,
                                        null
                                )
                )
                .toList();
    }

    /**
     * Obtiene una tarea propia por ID.
     */
    @Transactional(readOnly = true)
    public MedicalAgendaTaskResponse
    getMyTaskById(
            Long taskId,
            String username
    ) {
        if (taskId == null) {
            throw new RuntimeException(
                    "Debe indicar la tarea."
            );
        }

        User doctor =
                validateDoctor(
                        username
                );

        MedicalAgendaTask task =
                taskRepository
                        .findByIdAndDoctor_Id(
                                taskId,
                                doctor.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Tarea de agenda no encontrada."
                                        )
                        );

        return toResponse(
                task,
                null
        );
    }

    /**
     * Crea una tarea nueva.
     */
    @Transactional
    public MedicalAgendaTaskResponse
    createTask(
            CreateMedicalAgendaTaskRequest request,
            String username
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la tarea son obligatorios."
            );
        }

        User doctor =
                validateDoctor(
                        username
                );

        String cleanTitle =
                normalizeRequiredText(
                        request.getTitle(),
                        150,
                        "El título de la tarea"
                );

        String cleanDescription =
                normalizeOptionalText(
                        request.getDescription(),
                        1000,
                        "La descripción"
                );

        MedicalAgendaTaskPriority priority =
                parsePriority(
                        request.getPriority()
                );

        LocalDateTime dueDateTime =
                validateDueDateTime(
                        request.getDueDateTime()
                );

        LocalDateTime now =
                LocalDateTime.now();

        MedicalAgendaTask task =
                MedicalAgendaTask
                        .builder()
                        .doctor(
                                doctor
                        )
                        .title(
                                cleanTitle
                        )
                        .description(
                                cleanDescription
                        )
                        .priority(
                                priority
                        )
                        .dueDateTime(
                                dueDateTime
                        )
                        .completed(false)
                        .completedAt(null)
                        .active(true)
                        .createdAt(
                                now
                        )
                        .createdBy(
                                username
                        )
                        .updatedAt(
                                now
                        )
                        .updatedBy(
                                username
                        )
                        .build();

        task =
                taskRepository
                        .saveAndFlush(
                                task
                        );

        auditService.log(
                username,
                "CREATE_MEDICAL_AGENDA_TASK",
                "MEDICAL_AGENDA",
                "Tarea de agenda creada. ID: "
                        + task.getId()
                        + ". Título: "
                        + task.getTitle()
                        + ". Fecha límite: "
                        + task.getDueDateTime()
                        + ". Prioridad: "
                        + task
                        .getPriority()
                        .name()
                        + "."
        );

        return toResponse(
                task,
                "Tarea creada exitosamente."
        );
    }

    /**
     * Actualiza los datos editables de una
     * tarea activa propia.
     *
     * No modifica completed.
     */
    @Transactional
    public MedicalAgendaTaskResponse
    updateTask(
            Long taskId,
            UpdateMedicalAgendaTaskRequest request,
            String username
    ) {
        if (taskId == null) {
            throw new RuntimeException(
                    "Debe indicar la tarea."
            );
        }

        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la tarea son obligatorios."
            );
        }

        User doctor =
                validateDoctor(
                        username
                );

        MedicalAgendaTask task =
                taskRepository
                        .findByIdAndDoctor_IdAndActiveTrue(
                                taskId,
                                doctor.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Tarea activa no encontrada."
                                        )
                        );

        validateVersion(
                task,
                request.getVersion()
        );

        task.setTitle(
                normalizeRequiredText(
                        request.getTitle(),
                        150,
                        "El título de la tarea"
                )
        );

        task.setDescription(
                normalizeOptionalText(
                        request.getDescription(),
                        1000,
                        "La descripción"
                )
        );

        task.setPriority(
                parsePriority(
                        request.getPriority()
                )
        );

        task.setDueDateTime(
                validateDueDateTime(
                        request.getDueDateTime()
                )
        );

        task.setUpdatedAt(
                LocalDateTime.now()
        );

        task.setUpdatedBy(
                username
        );

        task =
                saveWithOptimisticControl(
                        task
                );

        auditService.log(
                username,
                "UPDATE_MEDICAL_AGENDA_TASK",
                "MEDICAL_AGENDA",
                "Tarea de agenda actualizada. ID: "
                        + task.getId()
                        + ". Título: "
                        + task.getTitle()
                        + "."
        );

        return toResponse(
                task,
                "Tarea actualizada exitosamente."
        );
    }

    /**
     * Completa o reabre una tarea.
     */
    @Transactional
    public MedicalAgendaTaskResponse
    changeTaskCompletion(
            Long taskId,
            Boolean completed,
            Long version,
            String username
    ) {
        if (taskId == null) {
            throw new RuntimeException(
                    "Debe indicar la tarea."
            );
        }

        if (completed == null) {
            throw new RuntimeException(
                    "Debe indicar el estado de la tarea."
            );
        }

        User doctor =
                validateDoctor(
                        username
                );

        MedicalAgendaTask task =
                taskRepository
                        .findByIdAndDoctor_IdAndActiveTrue(
                                taskId,
                                doctor.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Tarea activa no encontrada."
                                        )
                        );

        validateVersion(
                task,
                version
        );

        if (
                Boolean.TRUE.equals(
                        task.getCompleted()
                ) ==
                        Boolean.TRUE.equals(
                                completed
                        )
        ) {
            throw new RuntimeException(
                    completed
                            ? "La tarea ya se encuentra completada."
                            : "La tarea ya se encuentra pendiente."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        task.setCompleted(
                completed
        );

        task.setCompletedAt(
                Boolean.TRUE.equals(
                        completed
                )
                        ? now
                        : null
        );

        task.setUpdatedAt(
                now
        );

        task.setUpdatedBy(
                username
        );

        task =
                saveWithOptimisticControl(
                        task
                );

        auditService.log(
                username,
                completed
                        ? "COMPLETE_MEDICAL_AGENDA_TASK"
                        : "REOPEN_MEDICAL_AGENDA_TASK",
                "MEDICAL_AGENDA",
                (
                        completed
                                ? "Tarea completada. "
                                : "Tarea reabierta. "
                )
                        + "ID: "
                        + task.getId()
                        + ". Título: "
                        + task.getTitle()
                        + "."
        );

        return toResponse(
                task,
                completed
                        ? "Tarea completada exitosamente."
                        : "Tarea reabierta exitosamente."
        );
    }

    /**
     * Activa o desactiva lógicamente
     * una tarea propia.
     *
     * Desactivar equivale a eliminar en
     * la interfaz.
     */
    @Transactional
    public MedicalAgendaTaskResponse
    changeTaskStatus(
            Long taskId,
            Boolean active,
            Long version,
            String username
    ) {
        if (taskId == null) {
            throw new RuntimeException(
                    "Debe indicar la tarea."
            );
        }

        if (active == null) {
            throw new RuntimeException(
                    "Debe indicar el nuevo estado de la tarea."
            );
        }

        User doctor =
                validateDoctor(
                        username
                );

        MedicalAgendaTask task =
                taskRepository
                        .findByIdAndDoctor_Id(
                                taskId,
                                doctor.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Tarea de agenda no encontrada."
                                        )
                        );

        validateVersion(
                task,
                version
        );

        if (
                Boolean.TRUE.equals(
                        task.getActive()
                ) ==
                        Boolean.TRUE.equals(
                                active
                        )
        ) {
            throw new RuntimeException(
                    active
                            ? "La tarea ya se encuentra activa."
                            : "La tarea ya se encuentra eliminada."
            );
        }

        task.setActive(
                active
        );

        task.setUpdatedAt(
                LocalDateTime.now()
        );

        task.setUpdatedBy(
                username
        );

        task =
                saveWithOptimisticControl(
                        task
                );

        auditService.log(
                username,
                active
                        ? "REACTIVATE_MEDICAL_AGENDA_TASK"
                        : "DELETE_MEDICAL_AGENDA_TASK",
                "MEDICAL_AGENDA",
                (
                        active
                                ? "Tarea reactivada. "
                                : "Tarea eliminada. "
                )
                        + "ID: "
                        + task.getId()
                        + ". Título: "
                        + task.getTitle()
                        + "."
        );

        return toResponse(
                task,
                active
                        ? "Tarea reactivada exitosamente."
                        : "Tarea eliminada exitosamente."
        );
    }

    /**
     * Guarda utilizando el control optimista
     * de Hibernate.
     */
    private MedicalAgendaTask
    saveWithOptimisticControl(
            MedicalAgendaTask task
    ) {
        try {
            return taskRepository
                    .saveAndFlush(
                            task
                    );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "La tarea fue modificada por otro usuario. "
                            + "Actualice la información antes de continuar."
            );
        }
    }

    /**
     * Valida que la sesión corresponda
     * con un médico activo.
     */
    private User validateDoctor(
            String username
    ) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al médico autenticado."
            );
        }

        User doctor =
                userRepository
                        .findByUsername(
                                username
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Usuario autenticado no encontrado."
                                        )
                        );

        if (
                !Boolean.TRUE.equals(
                        doctor.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del médico se encuentra inactiva."
            );
        }

        if (
                doctor.getRole() == null ||
                        !Boolean.TRUE.equals(
                                doctor
                                        .getRole()
                                        .getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El médico no tiene un rol activo."
            );
        }

        String roleName =
                doctor
                        .getRole()
                        .getName();

        if (
                roleName == null ||
                        !"MEDICO".equalsIgnoreCase(
                                roleName.trim()
                        )
        ) {
            throw new RuntimeException(
                    "Solamente un médico puede administrar estas tareas."
            );
        }

        return doctor;
    }

    private String normalizeStatusFilter(
            String status
    ) {
        if (
                status == null ||
                        status.isBlank()
        ) {
            return "PENDING";
        }

        String normalized =
                status
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                !normalized.equals(
                        "PENDING"
                ) &&
                        !normalized.equals(
                                "COMPLETED"
                        ) &&
                        !normalized.equals(
                                "ALL"
                        )
        ) {
            throw new RuntimeException(
                    "El filtro de tareas no es válido. "
                            + "Use PENDING, COMPLETED o ALL."
            );
        }

        return normalized;
    }

    private MedicalAgendaTaskPriority
    parsePriority(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    "La prioridad es obligatoria."
            );
        }

        try {
            return MedicalAgendaTaskPriority
                    .valueOf(
                            value
                                    .trim()
                                    .toUpperCase(
                                            Locale.ROOT
                                    )
                    );

        } catch (
                IllegalArgumentException exception
        ) {
            throw new RuntimeException(
                    "La prioridad no es válida. "
                            + "Use BAJA, NORMAL o ALTA."
            );
        }
    }

    private LocalDateTime validateDueDateTime(
            LocalDateTime dueDateTime
    ) {
        if (dueDateTime == null) {
            throw new RuntimeException(
                    "La fecha límite es obligatoria."
            );
        }

        return dueDateTime;
    }

    private void validateVersion(
            MedicalAgendaTask task,
            Long requestVersion
    ) {
        if (requestVersion == null) {
            throw new RuntimeException(
                    "La versión de la tarea es obligatoria."
            );
        }

        if (
                task.getVersion() == null ||
                        !task.getVersion()
                                .equals(
                                        requestVersion
                                )
        ) {
            throw new RuntimeException(
                    "La tarea fue modificada por otro usuario. "
                            + "Actualice la información antes de continuar."
            );
        }
    }

    private String normalizeRequiredText(
            String value,
            int maximumLength,
            String fieldName
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    fieldName
                            + " es obligatorio."
            );
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

    private String normalizeOptionalText(
            String value,
            int maximumLength,
            String fieldName
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
                    fieldName
                            + " no puede exceder los "
                            + maximumLength
                            + " caracteres."
            );
        }

        return normalized;
    }

    private MedicalAgendaTaskResponse
    toResponse(
            MedicalAgendaTask task,
            String message
    ) {
        LocalDateTime now =
                LocalDateTime.now();

        boolean overdue =
                Boolean.TRUE.equals(
                        task.getActive()
                ) &&
                        !Boolean.TRUE.equals(
                                task.getCompleted()
                        ) &&
                        task.getDueDateTime() != null &&
                        task.getDueDateTime()
                                .isBefore(
                                        now
                                );

        return MedicalAgendaTaskResponse
                .builder()
                .taskId(
                        task.getId()
                )
                .doctorId(
                        task
                                .getDoctor()
                                .getId()
                )
                .doctorName(
                        task
                                .getDoctor()
                                .getFullName()
                )
                .title(
                        task.getTitle()
                )
                .description(
                        task.getDescription()
                )
                .priority(
                        task
                                .getPriority()
                                .name()
                )
                .priorityLabel(
                        getPriorityLabel(
                                task.getPriority()
                        )
                )
                .priorityColor(
                        getPriorityColor(
                                task.getPriority()
                        )
                )
                .dueDateTime(
                        task.getDueDateTime() != null
                                ? task
                                .getDueDateTime()
                                .toString()
                                : null
                )
                .completed(
                        task.getCompleted()
                )
                .completedAt(
                        task.getCompletedAt() != null
                                ? task
                                .getCompletedAt()
                                .toString()
                                : null
                )
                .overdue(
                        overdue
                )
                .active(
                        task.getActive()
                )
                .createdAt(
                        task.getCreatedAt() != null
                                ? task
                                .getCreatedAt()
                                .toString()
                                : null
                )
                .createdBy(
                        task.getCreatedBy()
                )
                .updatedAt(
                        task.getUpdatedAt() != null
                                ? task
                                .getUpdatedAt()
                                .toString()
                                : null
                )
                .updatedBy(
                        task.getUpdatedBy()
                )
                .version(
                        task.getVersion()
                )
                .message(
                        message
                )
                .build();
    }

    private String getPriorityLabel(
            MedicalAgendaTaskPriority priority
    ) {
        return switch (priority) {
            case BAJA ->
                    "Baja";

            case NORMAL ->
                    "Normal";

            case ALTA ->
                    "Alta";
        };
    }

    private String getPriorityColor(
            MedicalAgendaTaskPriority priority
    ) {
        return switch (priority) {
            case BAJA ->
                    "#3B82F6";

            case NORMAL ->
                    "#F59E0B";

            case ALTA ->
                    "#EF4444";
        };
    }
}