package com.hospital.his.appointments.repository;

import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.entity.AppointmentStatusConfiguration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AppointmentStatusConfigurationRepository
        extends JpaRepository<
        AppointmentStatusConfiguration,
        Long
        >,
        JpaSpecificationExecutor<
                AppointmentStatusConfiguration
                >{

    Optional<AppointmentStatusConfiguration>
    findByCode(
            AppointmentStatus code
    );

    /**
     * Busca una configuración activa y visible
     * para pacientes.
     *
     * Si no existe, el servicio de presentación
     * utilizará valores predeterminados.
     */
    Optional<AppointmentStatusConfiguration>
    findByCodeAndActiveTrueAndPatientVisibleTrue(
            AppointmentStatus code
    );

    /**
     * Busca una configuración activa y visible
     * para personal interno.
     *
     * Si no existe, el servicio de presentación
     * utilizará valores predeterminados.
     */
    Optional<AppointmentStatusConfiguration>
    findByCodeAndActiveTrueAndStaffVisibleTrue(
            AppointmentStatus code
    );


    boolean existsByCode(
            AppointmentStatus code
    );

    /**
     * Listado administrativo ordenado.
     */
    Page<AppointmentStatusConfiguration>
    findAllByOrderByDisplayOrderAscCodeAsc(
            Pageable pageable
    );

    /**
     * Filtro por texto.
     */
    Page<AppointmentStatusConfiguration>
    findByDisplayNameContainingIgnoreCaseOrCodeOrderByDisplayOrderAscCodeAsc(
            String displayName,
            AppointmentStatus code,
            Pageable pageable
    );

    /**
     * Filtro por estado lógico.
     */
    Page<AppointmentStatusConfiguration>
    findByActiveOrderByDisplayOrderAscCodeAsc(
            Boolean active,
            Pageable pageable
    );

    /**
     * Configuraciones activas visibles
     * para pacientes.
     */
    List<AppointmentStatusConfiguration>
    findByActiveTrueAndPatientVisibleTrueOrderByDisplayOrderAscCodeAsc();

    /**
     * Configuraciones activas visibles
     * para personal.
     */
    List<AppointmentStatusConfiguration>
    findByActiveTrueAndStaffVisibleTrueOrderByDisplayOrderAscCodeAsc();
}