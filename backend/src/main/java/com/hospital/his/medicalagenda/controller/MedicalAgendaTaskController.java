package com.hospital.his.medicalagenda.controller;

import com.hospital.his.medicalagenda.dto
        .CreateMedicalAgendaTaskRequest;
import com.hospital.his.medicalagenda.dto
        .MedicalAgendaTaskResponse;
import com.hospital.his.medicalagenda.dto
        .UpdateMedicalAgendaTaskRequest;
import com.hospital.his.medicalagenda.service
        .MedicalAgendaTaskService;

import jakarta.validation.Valid;

import org.springframework.format.annotation
        .DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost
        .PreAuthorize;
import org.springframework.security.core
        .Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * CU-16:
 * Tareas y recordatorios personales del
 * médico autenticado.
 */
@RestController
@RequestMapping(
        "/api/doctor/agenda/tasks"
)
public class MedicalAgendaTaskController {

    private final MedicalAgendaTaskService
            taskService;

    public MedicalAgendaTaskController(
            MedicalAgendaTaskService
                    taskService
    ) {
        this.taskService =
                taskService;
    }

    /**
     * Lista las tareas del día seleccionado.
     *
     * status:
     *
     * PENDING
     * COMPLETED
     * ALL
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_READ')"
    )
    @GetMapping
    public ResponseEntity<
            List<MedicalAgendaTaskResponse>
            >
    getMyTasks(
            @RequestParam
            @DateTimeFormat(
                    iso =
                            DateTimeFormat.ISO.DATE
            )
            LocalDate date,

            @RequestParam(
                    defaultValue = "PENDING"
            )
            String status,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService
                        .getMyTasks(
                                date,
                                status,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Obtiene una tarea propia.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_READ')"
    )
    @GetMapping("/{taskId}")
    public ResponseEntity<
            MedicalAgendaTaskResponse
            >
    getMyTaskById(
            @PathVariable
            Long taskId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService
                        .getMyTaskById(
                                taskId,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Crea una tarea.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_TASK_CREATE')"
    )
    @PostMapping
    public ResponseEntity<
            MedicalAgendaTaskResponse
            >
    createTask(
            @Valid
            @RequestBody
            CreateMedicalAgendaTaskRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService
                        .createTask(
                                request,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Actualiza una tarea.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_TASK_UPDATE')"
    )
    @PutMapping("/{taskId}")
    public ResponseEntity<
            MedicalAgendaTaskResponse
            >
    updateTask(
            @PathVariable
            Long taskId,

            @Valid
            @RequestBody
            UpdateMedicalAgendaTaskRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService
                        .updateTask(
                                taskId,
                                request,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Completa o reabre una tarea.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_TASK_UPDATE')"
    )
    @PatchMapping(
            "/{taskId}/completion"
    )
    public ResponseEntity<
            MedicalAgendaTaskResponse
            >
    changeTaskCompletion(
            @PathVariable
            Long taskId,

            @RequestParam
            Boolean completed,

            @RequestParam
            Long version,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService
                        .changeTaskCompletion(
                                taskId,
                                completed,
                                version,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Activa o desactiva lógicamente
     * una tarea.
     */
    @PreAuthorize(
            "hasAuthority('MEDICAL_AGENDA_TASK_DELETE')"
    )
    @PatchMapping(
            "/{taskId}/status"
    )
    public ResponseEntity<
            MedicalAgendaTaskResponse
            >
    changeTaskStatus(
            @PathVariable
            Long taskId,

            @RequestParam
            Boolean active,

            @RequestParam
            Long version,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService
                        .changeTaskStatus(
                                taskId,
                                active,
                                version,
                                authentication
                                        .getName()
                        )
        );
    }
}