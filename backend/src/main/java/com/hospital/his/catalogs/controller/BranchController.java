package com.hospital.his.catalogs.controller;

import com.hospital.his.catalogs.dto.*;
import com.hospital.his.catalogs.service.BranchService;

import com.hospital.his.users.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(@RequestBody CreateBranchRequest request) {
        return ResponseEntity.ok(branchService.createBranch(request));
    }

    @GetMapping
    public ResponseEntity<List<BranchResponse>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    //Buscar sucursal por ID
    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse> getBranchById(@PathVariable Long id) {

        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    //Actualizar sucursal
    @PutMapping("/{id}")
    public ResponseEntity<BranchResponse> updateBranch(@PathVariable Long id, @RequestBody UpdateBranchRequest request) {
        return ResponseEntity.ok(branchService.updateBranch(id, request));
    }

    //Cambio de estatus
    @PatchMapping("/{id}/status")
    public ResponseEntity<BranchResponse> changeStatus(@PathVariable Long id, @RequestParam Boolean active) {
        return ResponseEntity.ok(branchService.changeStatus(id,active));
    }

}