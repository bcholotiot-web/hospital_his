package com.hospital.his.laboratory.repository;

import com.hospital.his.laboratory.entity.LaboratoryTest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LaboratoryTestRepository
        extends JpaRepository<LaboratoryTest, Long> {

    /**
     * Catálogo activo utilizado por el médico.
     */
    List<LaboratoryTest>
    findByActiveTrueOrderByNameAsc();

    /**
     * Busca un examen activo por ID.
     */
    Optional<LaboratoryTest>
    findByIdAndActiveTrue(
            Long id
    );

    /**
     * Conserva la validación existente.
     */
    boolean existsByCodeIgnoreCase(
            String code
    );

    /**
     * Comprueba duplicados durante actualización.
     */
    boolean existsByCodeIgnoreCaseAndIdNot(
            String code,
            Long testId
    );

    /**
     * Busca un examen activo o inactivo por código.
     *
     * Permite reactivar la fila anterior.
     */
    Optional<LaboratoryTest>
    findByCodeIgnoreCase(
            String code
    );

    /**
     * Listado administrativo completo.
     */
    Page<LaboratoryTest>
    findAllByOrderByNameAsc(
            Pageable pageable
    );

    /**
     * Busca por código o nombre.
     */
    Page<LaboratoryTest>
    findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(
            String code,
            String name,
            Pageable pageable
    );

    /**
     * Filtro por estado.
     */
    Page<LaboratoryTest>
    findByActiveOrderByNameAsc(
            Boolean active,
            Pageable pageable
    );

    /**
     * Filtro combinado de texto y estado.
     */
    Page<LaboratoryTest>
    findByActiveAndCodeContainingIgnoreCaseOrActiveAndNameContainingIgnoreCaseOrderByNameAsc(
            Boolean codeActive,
            String code,
            Boolean nameActive,
            String name,
            Pageable pageable
    );
}