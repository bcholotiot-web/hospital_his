package com.hospital.his.medicalagenda.repository;

import com.hospital.his.medicalagenda.entity.MedicalAgendaEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de eventos personales
 * de agenda médica.
 */
public interface MedicalAgendaEventRepository
        extends JpaRepository<
        MedicalAgendaEvent,
        Long
        > {

    /**
     * Obtiene un evento activo que pertenece
     * al médico indicado.
     *
     * Impide consultar eventos pertenecientes
     * a otros médicos.
     */
    Optional<MedicalAgendaEvent>
    findByIdAndDoctor_IdAndActiveTrue(
            Long eventId,
            Long doctorId
    );

    /**
     * Obtiene un evento propio sin importar
     * si está activo o inactivo.
     *
     * Se utiliza para reactivación y cambio
     * de estado lógico.
     */
    Optional<MedicalAgendaEvent>
    findByIdAndDoctor_Id(
            Long eventId,
            Long doctorId
    );

    /**
     * Lista eventos activos que se intersectan
     * con el período solicitado.
     *
     * La condición de solapamiento es:
     *
     * evento.inicio < rango.fin
     * y
     * evento.fin > rango.inicio
     *
     * Esta condición permite incluir eventos que
     * comienzan antes del rango, pero todavía están
     * activos dentro del mismo.
     */
    @Query("""
        SELECT event
        FROM MedicalAgendaEvent event
        WHERE event.doctor.id = :doctorId
          AND event.active = true
          AND event.startDateTime < :rangeEnd
          AND event.endDateTime > :rangeStart
        ORDER BY event.startDateTime ASC
        """)
    List<MedicalAgendaEvent>
    findActiveEventsWithinRange(
            @Param("doctorId")
            Long doctorId,

            @Param("rangeStart")
            LocalDateTime rangeStart,

            @Param("rangeEnd")
            LocalDateTime rangeEnd
    );

    /**
     * Detecta un evento activo que se superpone
     * con un nuevo período.
     *
     * Se utiliza durante creación.
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(event) > 0
            THEN true
            ELSE false
        END
        FROM MedicalAgendaEvent event
        WHERE event.doctor.id = :doctorId
          AND event.active = true
          AND event.startDateTime < :endDateTime
          AND event.endDateTime > :startDateTime
        """)
    boolean existsActiveOverlap(
            @Param("doctorId")
            Long doctorId,

            @Param("startDateTime")
            LocalDateTime startDateTime,

            @Param("endDateTime")
            LocalDateTime endDateTime
    );

    /**
     * Detecta superposición durante la edición,
     * excluyendo el evento que se está actualizando.
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(event) > 0
            THEN true
            ELSE false
        END
        FROM MedicalAgendaEvent event
        WHERE event.doctor.id = :doctorId
          AND event.id <> :excludedEventId
          AND event.active = true
          AND event.startDateTime < :endDateTime
          AND event.endDateTime > :startDateTime
        """)
    boolean existsActiveOverlapExcludingEvent(
            @Param("doctorId")
            Long doctorId,

            @Param("excludedEventId")
            Long excludedEventId,

            @Param("startDateTime")
            LocalDateTime startDateTime,

            @Param("endDateTime")
            LocalDateTime endDateTime
    );
}