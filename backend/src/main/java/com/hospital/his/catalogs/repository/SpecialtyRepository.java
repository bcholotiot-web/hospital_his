package com.hospital.his.catalogs.repository;

import com.hospital.his.catalogs.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecialtyRepository
        extends JpaRepository<Specialty, Long> {

    /**
     * Comprueba nombres duplicados.
     */
    boolean existsByName(
            String name
    );

    /**
     * Obtiene especialidades activas ordenadas
     * alfabéticamente.
     *
     * CU-13 utiliza este método para llenar
     * el selector administrativo.
     */
    List<Specialty>
    findByActiveTrueOrderByNameAsc();

    /**
     * Busca una especialidad activa por ID.
     *
     * Impide relacionar especialidades inactivas
     * con una sede.
     */
    Optional<Specialty>
    findByIdAndActiveTrue(
            Long specialtyId
    );
}