package com.hospital.his.users.repository;

import com.hospital.his.users.entity.Role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository
        extends JpaRepository<Role, Long> {

    /**
     * Conserva compatibilidad con el login
     * y servicios actuales.
     */
    Optional<Role> findByName(
            String name
    );

    /**
     * Busca un rol por nombre sin distinguir
     * mayúsculas y minúsculas.
     */
    Optional<Role> findByNameIgnoreCase(
            String name
    );

    /**
     * Comprueba nombres duplicados.
     */
    boolean existsByNameIgnoreCase(
            String name
    );

    /**
     * Comprueba duplicados durante una actualización.
     */
    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long roleId
    );

    /**
     * Busca un rol activo por ID.
     *
     * Debe utilizarse al crear o actualizar usuarios.
     */
    Optional<Role> findByIdAndActiveTrue(
            Long roleId
    );

    /**
     * Roles activos utilizados en selectores.
     */
    List<Role>
    findByActiveTrueOrderByNameAsc();

    /**
     * Listado administrativo completo.
     */
    Page<Role>
    findAllByOrderByNameAsc(
            Pageable pageable
    );

    /**
     * Filtro por nombre.
     */
    Page<Role>
    findByNameContainingIgnoreCaseOrderByNameAsc(
            String name,
            Pageable pageable
    );

    /**
     * Filtro por estado.
     */
    Page<Role>
    findByActiveOrderByNameAsc(
            Boolean active,
            Pageable pageable
    );

    /**
     * Filtro por nombre y estado.
     */
    Page<Role>
    findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(
            String name,
            Boolean active,
            Pageable pageable
    );
}