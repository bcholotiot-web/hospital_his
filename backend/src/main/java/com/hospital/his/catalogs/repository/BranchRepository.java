package com.hospital.his.catalogs.repository;

import com.hospital.his.catalogs.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    /**
     * Comprueba nombres duplicados.
     */
    boolean existsByName(
            String name
    );

    /**
     * Conserva compatibilidad con servicios actuales.
     */
    List<Branch> findByActiveTrue();

    /**
     * Obtiene sedes activas ordenadas por nombre.
     *
     * CU-13 utiliza este método para llenar
     * el selector administrativo.
     */
    List<Branch>
    findByActiveTrueOrderByNameAsc();

    /**
     * Busca una sede activa por ID.
     *
     * Impide crear una asignación para una sede
     * desactivada.
     */
    Optional<Branch>
    findByIdAndActiveTrue(
            Long branchId
    );
}