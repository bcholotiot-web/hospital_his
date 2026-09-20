package com.hospital.his.medications.service;

import com.hospital.his.medications.dto.MedicationResponse;
import com.hospital.his.medications.entity.Medication;
import com.hospital.his.medications.repository.MedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicationCatalogService {

    private final MedicationRepository
            medicationRepository;

    public MedicationCatalogService(
            MedicationRepository medicationRepository
    ) {
        this.medicationRepository =
                medicationRepository;
    }

    /**
     * Lista los medicamentos activos.
     *
     * Este método será utilizado por el médico
     * cuando genere una receta.
     */
    @Transactional(readOnly = true)
    public List<MedicationResponse>
    getActiveMedications() {
        return medicationRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Busca medicamentos activos por nombre.
     *
     * Si el criterio está vacío, devuelve el catálogo
     * activo completo.
     */
    @Transactional(readOnly = true)
    public List<MedicationResponse>
    searchActiveMedications(
            String searchValue
    ) {
        if (
                searchValue == null ||
                        searchValue.isBlank()
        ) {
            return getActiveMedications();
        }

        String cleanValue =
                searchValue.trim();

        if (cleanValue.length() > 100) {
            throw new RuntimeException(
                    "El criterio de búsqueda no puede "
                            + "exceder los 100 caracteres."
            );
        }

        return medicationRepository
                .findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
                        cleanValue
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Busca un medicamento activo por ID.
     *
     * Esta operación será reutilizada posteriormente
     * en la creación de recetas.
     */
    @Transactional(readOnly = true)
    public Medication getActiveMedication(
            Long medicationId
    ) {
        if (medicationId == null) {
            throw new RuntimeException(
                    "Debe indicar el medicamento."
            );
        }

        return medicationRepository
                .findByIdAndActiveTrue(
                        medicationId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "El medicamento no existe "
                                        + "o se encuentra inactivo."
                        )
                );
    }

    /**
     * Convierte la entidad JPA en una respuesta segura.
     */
    private MedicationResponse toResponse(
            Medication medication
    ) {
        return MedicationResponse.builder()
                .id(
                        medication.getId()
                )
                .code(
                        medication.getCode()
                )
                .name(
                        medication.getName()
                )
                .activeIngredient(
                        medication.getActiveIngredient()
                )
                .concentration(
                        medication.getConcentration()
                )
                .presentation(
                        medication.getPresentation()
                )
                .description(
                        medication.getDescription()
                )
                .unitPrice(
                        medication.getUnitPrice()
                )
                .controlled(
                        medication.getControlled()
                )
                .active(
                        medication.getActive()
                )
                .build();
    }
}