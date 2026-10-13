package com.hospital.his.catalogs.repository;

import com.hospital.his.catalogs.entity.Branch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    /**
     * Conserva compatibilidad con código anterior.
     */
    boolean existsByName(
            String name
    );

    /**
     * Busca nombres duplicados sin distinguir
     * mayúsculas y minúsculas.
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
            Long branchId
    );

    /**
     * Busca una sucursal por nombre sin importar
     * si está activa o inactiva.
     *
     * Permite reactivar la fila histórica.
     */
    Optional<Branch>
    findByNameIgnoreCase(
            String name
    );

    /**
     * Conserva compatibilidad con CU-03.
     */
    List<Branch>
    findByActiveTrue();

    /**
     * Devuelve sedes activas ordenadas por nombre.
     */
    List<Branch>
    findByActiveTrueOrderByNameAsc();

    /**
     * Busca una sede activa por ID.
     */
    Optional<Branch>
    findByIdAndActiveTrue(
            Long branchId
    );

    /**
     * Listado administrativo completo.
     */
    Page<Branch>
    findAllByOrderByNameAsc(
            Pageable pageable
    );

    /**
     * Filtro por nombre.
     */
    Page<Branch>
    findByNameContainingIgnoreCaseOrderByNameAsc(
            String name,
            Pageable pageable
    );

    /**
     * Filtro por estado.
     */
    Page<Branch>
    findByActiveOrderByNameAsc(
            Boolean active,
            Pageable pageable
    );

    /**
     * Filtro combinado.
     */
    Page<Branch>
    findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(
            String name,
            Boolean active,
            Pageable pageable
    );
}