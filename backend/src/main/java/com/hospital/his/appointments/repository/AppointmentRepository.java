package com.hospital.his.appointments.repository;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    //El metodo evita que un metodo tenga dos citas en la misma fecha y hora
    boolean existsByDoctorAndAppointmentDateTime(
            User doctor,
            LocalDateTime appointmentDateTime
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

}