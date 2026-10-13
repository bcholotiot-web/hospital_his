package com.hospital.his.catalogs.repository;

import com.hospital.his.catalogs.entity.Specialty;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecialtyRepository
        extends JpaRepository<Specialty, Long> {

    /**
     * Conserva compatibilidad con código existente.
     */
    boolean existsByName(
            String name
    );

    /**
     * Comprueba nombres sin distinguir mayúsculas
     * y minúsculas.
     */
    boolean existsByNameIgnoreCase(
            String name
    );

    /**
     * Comprueba si otro registro utiliza el mismo
     * nombre durante una actualización.
     */
    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long specialtyId
    );

    /**
     * Busca una especialidad existente, activa
     * o inactiva, para permitir reactivación.
     */
    Optional<Specialty>
    findByNameIgnoreCase(
            String name
    );

    /**
     * Catálogo activo utilizado por citas,
     * usuarios y CU-13.
     */
    List<Specialty>
    findByActiveTrueOrderByNameAsc();

    /**
     * Busca una especialidad activa por ID.
     */
    Optional<Specialty>
    findByIdAndActiveTrue(
            Long specialtyId
    );

    /**
     * Listado administrativo completo.
     */
    Page<Specialty>
    findAllByOrderByNameAsc(
            Pageable pageable
    );

    /**
     * Filtro por texto en el nombre.
     */
    Page<Specialty>
    findByNameContainingIgnoreCaseOrderByNameAsc(
            String name,
            Pageable pageable
    );

    /**
     * Filtro por estado.
     */
    Page<Specialty>
    findByActiveOrderByNameAsc(
            Boolean active,
            Pageable pageable
    );

    /**
     * Filtro combinado por nombre y estado.
     */
    Page<Specialty>
    findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(
            String name,
            Boolean active,
            Pageable pageable
    );
}