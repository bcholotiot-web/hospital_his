package com.hospital.his.appointments.repository;

import com.hospital.his.appointments.entity.AppointmentReminder;
import com.hospital.his.appointments.entity.AppointmentReminderStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentReminderRepository
        extends JpaRepository<
        AppointmentReminder,
        Long
        > {

    /**
     * Busca el recordatorio de una cita.
     */
    Optional<AppointmentReminder>
    findByAppointment_Id(
            Long appointmentId
    );

    /**
     * Obtiene recordatorios pendientes cuya fecha
     * programada ya llegó.
     */
    List<AppointmentReminder>
    findTop50ByStatusAndSendAtLessThanEqualOrderBySendAtAsc(
            AppointmentReminderStatus status,
            LocalDateTime currentDateTime
    );
}