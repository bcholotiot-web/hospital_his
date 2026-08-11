package com.hospital.his.catalogs.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.dto.BranchResponse;
import com.hospital.his.catalogs.dto.CreateBranchRequest;
import com.hospital.his.catalogs.dto.UpdateBranchRequest;
import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;

    private final AuditService auditService;

    public BranchService(
            BranchRepository branchRepository,
            AuditService auditService) {

        this.branchRepository = branchRepository;
        this.auditService = auditService;
    }

    //Crear sucursal
    public BranchResponse createBranch(CreateBranchRequest request) {

        if (branchRepository.existsByName(request.getName())) {
            throw new RuntimeException("La sucursal ya se encuentra registrada.");
        }

        Branch branch = Branch.builder()
                .name(request.getName())
                .address(request.getAddress())
                .active(request.getActive())
                .build();

        branch = branchRepository.save(branch);

        auditService.log("SYSTEM","CREATE_BRANCH","CATALOGS","Sucursal creada correctamente.");

        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .active(branch.getActive())
                .build();
    }

    //Listar sucursal
    public List<BranchResponse> getAllBranches() {

        return branchRepository.findAll()
                .stream()
                .map(branch -> BranchResponse.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .address(branch.getAddress())
                        .active(branch.getActive())
                        .build())
                .toList();
    }

    //Buscar sucursal por ID
    public BranchResponse getBranchById(Long id) {

        Branch branch = branchRepository.findById(id).orElseThrow(() -> new RuntimeException("Sucursal no encontrada."));

        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .active(branch.getActive())
                .build();
    }

    //Actualizar sucursal
    public BranchResponse updateBranch(Long id,UpdateBranchRequest request) {

        Branch branch = branchRepository.findById(id).orElseThrow(() -> new RuntimeException("Sucursal no encontrada."));

        //Evitar duplicidad de nombre al actualizar
        if (!branch.getName().equals(request.getName()) && branchRepository.existsByName(request.getName())) {
            throw new RuntimeException("La sucursal ya se encuentra registrada.");
        }

        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setActive(request.getActive());

        branch = branchRepository.save(branch);

        auditService.log("SYSTEM","UPDATE_BRANCH","CATALOGS","Sucursal actualizada.");

        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .active(branch.getActive())
                .build();
    }

    //Cambiar estado de sucursal
    public BranchResponse changeStatus(Long id,Boolean active) {

        Branch branch = branchRepository.findById(id).orElseThrow(() -> new RuntimeException("Sucursal no encontrada."));

        branch.setActive(active);

        branch = branchRepository.save(branch);

        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .active(branch.getActive())
                .build();
    }

}