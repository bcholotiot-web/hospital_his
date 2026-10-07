package com.hospital.his.catalogs.repository;

import com.hospital.his.catalogs.entity.BranchSpecialty;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de asignaciones entre sedes
 * y especialidades.
 *
 * CU-13:
 * Permite administrar qué especialidades están
 * disponibles en cada sede del hospital.
 */
public interface BranchSpecialtyRepository
        extends JpaRepository<BranchSpecialty, Long> {

    /**
     * Método existente:
     * lista las asignaciones activas de una sede.
     *
     * Se conserva para no afectar servicios o endpoints
     * que actualmente lo estén utilizando.
     */
    List<BranchSpecialty>
    findByBranch_IdAndActiveTrue(
            Long branchId
    );

    /**
     * Comprueba si una combinación de sede y
     * especialidad ya se encuentra activa.
     *
     * Se utiliza como validación rápida antes
     * de intentar guardar una nueva asignación.
     */
    boolean
    existsByBranch_IdAndSpecialty_IdAndActiveTrue(
            Long branchId,
            Long specialtyId
    );

    /**
     * Busca una combinación de sede y especialidad
     * sin importar si se encuentra activa o inactiva.
     *
     * Es necesario para distinguir estos casos:
     *
     * 1. La asignación nunca ha existido:
     *    se crea una nueva fila.
     *
     * 2. La asignación existe y está activa:
     *    se rechaza como duplicada.
     *
     * 3. La asignación existe y está inactiva:
     *    se reactiva la misma fila.
     */
    Optional<BranchSpecialty>
    findByBranch_IdAndSpecialty_Id(
            Long branchId,
            Long specialtyId
    );

    /**
     * Lista todas las asignaciones aplicando
     * paginación y orden descendente por ID.
     *
     * Las asignaciones más recientes aparecerán
     * primero en el listado administrativo.
     */
    Page<BranchSpecialty>
    findAllByOrderByIdDesc(
            Pageable pageable
    );

    /**
     * Filtra una asignación por su ID y conserva
     * la estructura paginada requerida por CU-13.
     *
     * Este método no entra en conflicto con:
     *
     * Optional<BranchSpecialty> findById(Long id)
     *
     * heredado de JpaRepository, porque esta versión
     * recibe también un parámetro Pageable.
     */
    Page<BranchSpecialty>
    findById(
            Long assignmentId,
            Pageable pageable
    );

    /**
     * Busca una asignación activa por ID.
     *
     * Puede utilizarse en operaciones que solamente
     * admiten registros activos.
     */
    Optional<BranchSpecialty>
    findByIdAndActiveTrue(
            Long assignmentId
    );

    /**
     * Devuelve únicamente las especialidades
     * disponibles para una sede.
     *
     * Spring Data JPA genera una consulta que exige:
     *
     * 1. branch_id igual al ID recibido.
     * 2. Asignación activa.
     * 3. Sede activa.
     * 4. Especialidad activa.
     *
     * El resultado se ordena alfabéticamente por
     * el nombre de la especialidad.
     *
     * Este método será consumido por CU-03 para
     * cargar el paso de especialidades después
     * de seleccionar una sede.
     */
    List<BranchSpecialty>
    findByBranch_IdAndActiveTrueAndBranch_ActiveTrueAndSpecialty_ActiveTrueOrderBySpecialty_NameAsc(
            Long branchId
    );
}