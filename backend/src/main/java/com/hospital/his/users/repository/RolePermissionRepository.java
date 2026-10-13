package com.hospital.his.users.repository;

import com.hospital.his.users.entity.RolePermission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de permisos asignados a roles.
 */
public interface RolePermissionRepository
        extends JpaRepository<RolePermission, Long> {

    /**
     * Busca una relación activa o inactiva.
     *
     * Permite reactivar sin duplicar la fila.
     */
    Optional<RolePermission>
    findByRole_IdAndPermission_Id(
            Long roleId,
            Long permissionId
    );

    /**
     * Recupera todas las relaciones de un rol,
     * activas e inactivas.
     */
    List<RolePermission>
    findByRole_Id(
            Long roleId
    );

    /**
     * Recupera las relaciones ordenadas por
     * módulo y código.
     */
    List<RolePermission>
    findByRole_IdOrderByPermission_ModuleAscPermission_CodeAsc(
            Long roleId
    );

    /**
     * Comprueba si el rol tiene asignado
     * activamente un permiso determinado.
     */
    boolean
    existsByRole_IdAndPermission_CodeIgnoreCaseAndActiveTrue(
            Long roleId,
            String permissionCode
    );

    /**
     * Obtiene únicamente los permisos efectivos:
     *
     * - Rol activo.
     * - Relación activa.
     * - Permiso activo.
     */
    @Query("""
        SELECT rolePermission
        FROM RolePermission rolePermission
        JOIN FETCH rolePermission.permission permission
        JOIN FETCH rolePermission.role role
        WHERE role.id = :roleId
          AND role.active = true
          AND rolePermission.active = true
          AND permission.active = true
        ORDER BY permission.module ASC,
                 permission.code ASC
        """)
    List<RolePermission>
    findActivePermissionsByRoleId(
            @Param("roleId")
            Long roleId
    );
}