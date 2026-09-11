package com.hospital.his.appointments.repository;

import com.hospital.his.appointments.entity.AppointmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentDocumentRepository
        extends JpaRepository<AppointmentDocument, Long> {

    List<AppointmentDocument>
    findByAppointment_IdAndActiveTrue(
            Long appointmentId
    );

    boolean existsByAppointment_IdAndActiveTrue(
            Long appointmentId
    );
}