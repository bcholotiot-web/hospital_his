package com.hospital.his.appointments.repository;

import com.hospital.his.appointments.entity.AppointmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppointmentDocumentRepository
        extends JpaRepository<
        AppointmentDocument,
        Long
        > {

    /**
     * Lista los documentos activos de una cita,
     * ordenados desde el más reciente.
     */
    List<AppointmentDocument>
    findByAppointment_IdAndActiveTrueOrderByUploadedAtDesc(
            Long appointmentId
    );

    /**
     * Conserva compatibilidad con operaciones
     * existentes que no requieren orden explícito.
     */
    List<AppointmentDocument>
    findByAppointment_IdAndActiveTrue(
            Long appointmentId
    );

    /**
     * Busca un documento activo comprobando que
     * pertenezca a la cita indicada.
     *
     * Evita consultar un documentId usando el ID
     * de otra cita.
     */
    Optional<AppointmentDocument>
    findByIdAndAppointment_IdAndActiveTrue(
            Long documentId,
            Long appointmentId
    );

    /**
     * Determina si la cita contiene al menos
     * un documento activo.
     */
    boolean existsByAppointment_IdAndActiveTrue(
            Long appointmentId
    );

    /**
     * Cuenta documentos activos.
     */
    long countByAppointment_IdAndActiveTrue(
            Long appointmentId
    );
}