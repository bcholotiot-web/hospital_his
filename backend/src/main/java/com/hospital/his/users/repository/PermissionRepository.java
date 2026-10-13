package com.hospital.his.users.repository;

import com.hospital.his.users.entity.Permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio del catálogo técnico de permisos.
 */
public interface PermissionRepository
        extends JpaRepository<Permission, Long> {

    /**
     * Busca un permiso por su código técnico.
     */
    Optional<Permission>
    findByCodeIgnoreCase(
            String code
    );

    /**
     * Busca un permiso activo por ID.
     *
     * Se utiliza antes de asignarlo a un rol.
     */
    Optional<Permission>
    findByIdAndActiveTrue(
            Long permissionId
    );

    /**
     * Devuelve permisos activos agrupados
     * por módulo y código.
     */
    List<Permission>
    findByActiveTrueOrderByModuleAscCodeAsc();

    /**
     * Filtra permisos activos por módulo.
     */
    List<Permission>
    findByModuleIgnoreCaseAndActiveTrueOrderByCodeAsc(
            String module
    );
}