package com.hospital.his.catalogs.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.dto.CreateSpecialtyRequest;
import com.hospital.his.catalogs.dto.SpecialtyResponse;
import com.hospital.his.catalogs.dto.UpdateSpecialtyRequest;
import com.hospital.his.catalogs.entity.Specialty;
import com.hospital.his.catalogs.repository.SpecialtyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    private final AuditService auditService;

    public SpecialtyService(
            SpecialtyRepository specialtyRepository,
            AuditService auditService) {

        this.specialtyRepository = specialtyRepository;
        this.auditService = auditService;
    }

    //Crear especialidad
    public SpecialtyResponse createSpecialty(CreateSpecialtyRequest request) {

        if (specialtyRepository.existsByName(request.getName())) {

            throw new RuntimeException("La especialidad ya se encuentra registrada.");
        }
        Specialty specialty = Specialty.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(request.getActive())
                .build();

        specialty = specialtyRepository.save(specialty);

        auditService.log("SYSTEM","CREATE_SPECIALTY","CATALOGS","Especialidad creada correctamente.");

        return SpecialtyResponse.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .description(specialty.getDescription())
                .active(specialty.getActive())
                .build();
    }

    //Listar especialidades
    public List<SpecialtyResponse> getAllSpecialties() {

        return specialtyRepository.findAll()
                .stream()
                .map(specialty -> SpecialtyResponse.builder()
                        .id(specialty.getId())
                        .name(specialty.getName())
                        .description(specialty.getDescription())
                        .active(specialty.getActive())
                        .build())
                .toList();
    }

    //Buscar especialidad por ID
    public SpecialtyResponse getSpecialtyById(Long id) {
        Specialty specialty = specialtyRepository.findById(id).orElseThrow(() -> new RuntimeException("Especialidad no encontrada."));

        return SpecialtyResponse.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .description(specialty.getDescription())
                .active(specialty.getActive())
                .build();
    }

    //Actualizar especialidad
    public SpecialtyResponse updateSpecialty(Long id, UpdateSpecialtyRequest request) {

        Specialty specialty = specialtyRepository.findById(id).orElseThrow(() -> new RuntimeException("Especialidad no encontrada."));

        if (!specialty.getName().equals(request.getName())&& specialtyRepository.existsByName(request.getName())) {
            throw new RuntimeException("La especialidad ya se encuentra registrada.");
        }

        specialty.setName(request.getName());
        specialty.setDescription(request.getDescription());
        specialty.setActive(request.getActive());

        specialty = specialtyRepository.save(specialty);

        auditService.log("SYSTEM","UPDATE_SPECIALTY","CATALOGS","Especialidad actualizada correctamente.");

        return SpecialtyResponse.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .description(specialty.getDescription())
                .active(specialty.getActive())
                .build();
    }

    //Cambiar estatus active/false
    public SpecialtyResponse changeStatus(Long id,Boolean active) {
        Specialty specialty = specialtyRepository.findById(id).orElseThrow(() -> new RuntimeException("Especialidad no encontrada."));

        specialty.setActive(active);

        specialty = specialtyRepository.save(specialty);

        auditService.log("SYSTEM","CHANGE_SPECIALTY_STATUS","CATALOGS","Estado de especialidad actualizado.");

        return SpecialtyResponse.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .description(specialty.getDescription())
                .active(specialty.getActive())
                .build();
    }
}