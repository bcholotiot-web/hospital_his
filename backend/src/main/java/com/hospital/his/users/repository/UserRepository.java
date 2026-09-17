package com.hospital.his.users.repository;

import com.hospital.his.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    /*
     * Búsquedas individuales.
     */
    Optional<User> findByUsername(
            String username
    );

    Optional<User> findByEmail(
            String email
    );

    Optional<User> findByDpi(
            String dpi
    );

    /*
     * Validaciones de duplicados.
     */
    boolean existsByUsername(
            String username
    );

    boolean existsByEmail(
            String email
    );

    boolean existsByDpi(
            String dpi
    );

    /*
     * Listado principal del CU-01.
     *
     * Solamente devuelve usuarios activos.
     * Los registros inactivos permanecen en PostgreSQL,
     * pero no se muestran en el frontend.
     */
    List<User> findAllByActiveTrueOrderByIdAsc();

    /*
     * RN-CU01-01:
     * Búsqueda parcial por nombre completo.
     */
    List<User>
    findByActiveTrueAndFullNameContainingIgnoreCaseOrderByIdAsc(
            String fullName
    );

    /*
     * RN-CU01-01:
     * Búsqueda por DPI.
     *
     * Se permite búsqueda parcial para facilitar
     * la localización desde mantenimiento.
     */
    List<User>
    findByActiveTrueAndDpiContainingOrderByIdAsc(
            String dpi
    );

    /*
     * RN-CU01-01:
     * Búsqueda parcial por nombre de usuario.
     */
    List<User>
    findByActiveTrueAndUsernameContainingIgnoreCaseOrderByIdAsc(
            String username
    );

    /*
     * RN-CU01-01:
     * Búsqueda por nombre de rol.
     */
    List<User>
    findByActiveTrueAndRole_NameContainingIgnoreCaseOrderByIdAsc(
            String roleName
    );

    /*
     * RN-CU01-01:
     * Búsqueda por sucursal.
     */
    List<User>
    findByActiveTrueAndBranch_NameContainingIgnoreCaseOrderByIdAsc(
            String branchName
    );

    /*
     * Búsquedas utilizadas en el módulo de citas.
     */
    List<User>
    findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue(
            String roleName,
            Long branchId,
            Long specialtyId
    );

    List<User>
    findByBranch_IdAndSpecialtyIsNotNullAndActiveTrue(
            Long branchId
    );
}