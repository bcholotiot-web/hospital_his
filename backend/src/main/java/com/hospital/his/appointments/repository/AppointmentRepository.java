package com.hospital.his.appointments.repository;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.users.entity.User;
import com.hospital.his.appointments.entity.FollowUpType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    //El metodo evita que un metodo tenga dos citas en la misma fecha y hora
    boolean existsByDoctorAndAppointmentDateTimeAndActiveTrue(
            User doctor,
            LocalDateTime appointmentDateTime
    );

    boolean existsByDoctorAndAppointmentDateTimeAndActiveTrueAndIdNot(
            User doctor,
            LocalDateTime appointmentDateTime,
            Long appointmentId
    );

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetween(
            Long doctorId,
            LocalDateTime start,
            LocalDateTime end
    );



    /**status = PENDIENTE_DE_PAGO
     reservation_expires_at <= fecha actual*/
    List<Appointment> findByStatusAndReservationExpiresAtLessThanEqual(
            AppointmentStatus status,
            LocalDateTime currentDateTime
    );

    //Impide que un paciente consulte o pague la cita de otra persona

    Optional<Appointment> findByIdAndPatient_Username(
            Long appointmentId,
            String username
    );

    List<Appointment>
    findByPatient_UsernameOrderByAppointmentDateTimeDesc(
            String username
    );

    List<Appointment>
    findByPatient_DpiAndStatusInOrderByAppointmentDateTimeDesc(
            String dpi,
            Collection<AppointmentStatus> statuses
    );

    boolean existsByPatient_Dpi(
            String dpi
    );

    Optional<Appointment>
    findByIdAndStatusAndActiveTrue(
            Long appointmentId,
            AppointmentStatus status
    );

    List<Appointment>
    findByPatient_DpiAndStatusAndActiveTrueOrderByAppointmentDateTimeDesc(
            String dpi,
            AppointmentStatus status
    );

    //Signos vitales CU-07
    List<Appointment>
    findByStatusInAndActiveTrueOrderByPriorityDescArrivalTimeAsc(
            Collection<AppointmentStatus> statuses
    );

    //Muestra unicamente la cita asignado al medico logueado
    List<Appointment>
    findByDoctor_UsernameAndStatusInAndActiveTrueOrderByPriorityDescArrivalTimeAsc(
            String doctorUsername,
            Collection<AppointmentStatus> statuses
    );

    Optional<Appointment>
    findByIdAndDoctor_Username(
            Long appointmentId,
            String doctorUsername
    );

    /**
     * Recupera una cita creada previamente con la misma
     * llave de idempotencia.
     */
    Optional<Appointment> findByIdempotencyKey(UUID idempotencyKey);

    /**
     * Comprueba si una consulta ya tiene una cita
     * de seguimiento activa.
     *
     * Se utilizará para evitar seguimientos duplicados
     * accidentales del mismo tipo.
     */
    boolean existsByParentConsultation_IdAndFollowUpTypeAndActiveTrue(Long parentConsultationId, FollowUpType followUpType);

    /**
     * Lista las citas de seguimiento de una consulta.
     */
    List<Appointment> findByParentConsultation_IdAndFollowUpTrueOrderByAppointmentDateTimeDesc(Long parentConsultationId);

    /**
     * Recupera las citas de un médico en una fecha
     * y hora específica.
     *
     * Después, el servicio determina cuáles estados
     * bloquean realmente el horario.
     */
    List<Appointment> findByDoctor_IdAndAppointmentDateTime(Long doctorId, LocalDateTime appointmentDateTime);

    /**
     * Comprueba si ya existe una cita de seguimiento
     * del mismo tipo y con un estado que todavía bloquea
     * otra programación equivalente.
     */
    boolean
    existsByParentConsultation_IdAndFollowUpTypeAndStatusNotInAndActiveTrue(
            Long parentConsultationId,
            FollowUpType followUpType,
            Collection<AppointmentStatus> excludedStatuses
    );

    @Query("""
    SELECT appointment
    FROM Appointment appointment
    JOIN FETCH appointment.patient patient
    JOIN FETCH appointment.doctor doctor
    JOIN FETCH appointment.branch branch
    JOIN FETCH appointment.specialty specialty
    LEFT JOIN FETCH appointment.parentConsultation
            parentConsultation
    WHERE doctor.id = :doctorId
      AND appointment.active = true
      AND appointment.appointmentDateTime >= :rangeStart
      AND appointment.appointmentDateTime < :rangeEnd
      AND appointment.status NOT IN :excludedStatuses
    ORDER BY appointment.appointmentDateTime ASC
    """)
    List<Appointment>
    findDoctorCalendarAppointments(
            @Param("doctorId")
            Long doctorId,

            @Param("rangeStart")
            LocalDateTime rangeStart,

            @Param("rangeEnd")
            LocalDateTime rangeEnd,

            @Param("excludedStatuses")
            Collection<AppointmentStatus>
                    excludedStatuses
    );
}