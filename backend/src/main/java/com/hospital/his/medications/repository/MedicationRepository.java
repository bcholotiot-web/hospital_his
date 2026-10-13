package com.hospital.his.medications.repository;

import com.hospital.his.medications.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MedicationRepository
        extends JpaRepository<Medication, Long>,
        JpaSpecificationExecutor<Medication> {

    /**
     * Devuelve solamente medicamentos activos,
     * ordenados por nombre.
     *
     * Este método será utilizado por el médico
     * al generar una receta.
     */
    List<Medication>
    findByActiveTrueOrderByNameAsc();

    /**
     * Busca un medicamento activo por identificador.
     *
     * Se utilizará al crear una receta y al realizar
     * un despacho.
     */
    Optional<Medication>
    findByIdAndActiveTrue(
            Long medicationId
    );

    /**
     * Busca un medicamento por su código.
     */
    Optional<Medication>
    findByCodeIgnoreCase(
            String code
    );

    /**
     * Comprueba si existe otro medicamento con
     * el mismo código.
     */
    boolean existsByCodeIgnoreCase(
            String code
    );

    /**
     * Permite realizar búsquedas parciales por nombre
     * dentro del catálogo activo.
     */
    List<Medication>
    findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
            String name
    );

    /**
     * Permite buscar por principio activo.
     */
    List<Medication>
    findByActiveTrueAndActiveIngredientContainingIgnoreCaseOrderByNameAsc(
            String activeIngredient
    );

    /**
     * Busca medicamentos activos que tengan el mismo
     * principio activo que el medicamento prescrito.
     *
     * Se utilizará para presentar posibles alternativas.
     * El sistema no seleccionará una alternativa
     * automáticamente.
     */
    List<Medication>
    findByActiveTrueAndActiveIngredientIgnoreCaseOrderByNameAsc(
            String activeIngredient
    );

    /**
     * Comprueba duplicados de código durante
     * una actualización.
     */
    boolean existsByCodeIgnoreCaseAndIdNot(
            String code,
            Long medicationId
    );

    /**
     * Listado administrativo completo.
     */
    Page<Medication>
    findAllByOrderByNameAsc(
            Pageable pageable
    );

    /**
     * Búsqueda administrativa por código, nombre
     * o principio activo.
     */
    Page<Medication>
    findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrActiveIngredientContainingIgnoreCaseOrderByNameAsc(
            String code,
            String name,
            String activeIngredient,
            Pageable pageable
    );

    /**
     * Filtro administrativo por estado.
     */
    Page<Medication>
    findByActiveOrderByNameAsc(
            Boolean active,
            Pageable pageable
    );

    /**
     * Filtro por medicamento controlado.
     */
    Page<Medication>
    findByControlledOrderByNameAsc(
            Boolean controlled,
            Pageable pageable
    );

}