package com.hospital.his.medicalagenda.repository;

import com.hospital.his.medicalagenda.entity.MedicalAgendaTask;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de tareas y recordatorios
 * personales de la agenda médica.
 */
public interface MedicalAgendaTaskRepository
        extends JpaRepository<
        MedicalAgendaTask,
        Long
        > {

    /**
     * Obtiene una tarea propia sin importar
     * si está activa o inactiva.
     *
     * Se utiliza en:
     *
     * - Consulta de detalle.
     * - Cambio de estado.
     * - Reactivación.
     */
    Optional<MedicalAgendaTask>
    findByIdAndDoctor_Id(
            Long taskId,
            Long doctorId
    );

    /**
     * Obtiene una tarea activa propia.
     *
     * Se utiliza durante la edición y el
     * cambio de estado de completado.
     */
    Optional<MedicalAgendaTask>
    findByIdAndDoctor_IdAndActiveTrue(
            Long taskId,
            Long doctorId
    );

    /**
     * Lista todas las tareas activas del médico
     * cuya fecha límite pertenece al período.
     */
    @Query("""
        SELECT task
        FROM MedicalAgendaTask task
        WHERE task.doctor.id = :doctorId
          AND task.active = true
          AND task.dueDateTime >= :rangeStart
          AND task.dueDateTime < :rangeEnd
        ORDER BY
            CASE task.priority
                WHEN com.hospital.his.medicalagenda.entity.MedicalAgendaTaskPriority.ALTA
                    THEN 1
                WHEN com.hospital.his.medicalagenda.entity.MedicalAgendaTaskPriority.NORMAL
                    THEN 2
                WHEN com.hospital.his.medicalagenda.entity.MedicalAgendaTaskPriority.BAJA
                    THEN 3
                ELSE 4
            END,
            task.dueDateTime ASC,
            task.id ASC
        """)
    List<MedicalAgendaTask>
    findActiveTasksWithinRange(
            @Param("doctorId")
            Long doctorId,

            @Param("rangeStart")
            LocalDateTime rangeStart,

            @Param("rangeEnd")
            LocalDateTime rangeEnd
    );

    /**
     * Lista tareas pendientes activas dentro
     * del período seleccionado.
     */
    @Query("""
        SELECT task
        FROM MedicalAgendaTask task
        WHERE task.doctor.id = :doctorId
          AND task.active = true
          AND task.completed = false
          AND task.dueDateTime >= :rangeStart
          AND task.dueDateTime < :rangeEnd
        ORDER BY
            CASE task.priority
                WHEN com.hospital.his.medicalagenda.entity.MedicalAgendaTaskPriority.ALTA
                    THEN 1
                WHEN com.hospital.his.medicalagenda.entity.MedicalAgendaTaskPriority.NORMAL
                    THEN 2
                WHEN com.hospital.his.medicalagenda.entity.MedicalAgendaTaskPriority.BAJA
                    THEN 3
                ELSE 4
            END,
            task.dueDateTime ASC,
            task.id ASC
        """)
    List<MedicalAgendaTask>
    findPendingTasksWithinRange(
            @Param("doctorId")
            Long doctorId,

            @Param("rangeStart")
            LocalDateTime rangeStart,

            @Param("rangeEnd")
            LocalDateTime rangeEnd
    );

    /**
     * Lista tareas completadas activas dentro
     * del período seleccionado.
     */
    @Query("""
        SELECT task
        FROM MedicalAgendaTask task
        WHERE task.doctor.id = :doctorId
          AND task.active = true
          AND task.completed = true
          AND task.dueDateTime >= :rangeStart
          AND task.dueDateTime < :rangeEnd
        ORDER BY
            task.completedAt DESC,
            task.dueDateTime ASC,
            task.id ASC
        """)
    List<MedicalAgendaTask>
    findCompletedTasksWithinRange(
            @Param("doctorId")
            Long doctorId,

            @Param("rangeStart")
            LocalDateTime rangeStart,

            @Param("rangeEnd")
            LocalDateTime rangeEnd
    );
}