package com.hospital.his.catalogs.controller;

import com.hospital.his.catalogs.dto.CreateSpecialtyRequest;
import com.hospital.his.catalogs.dto.SpecialtyResponse;
import com.hospital.his.catalogs.dto.UpdateSpecialtyRequest;
import com.hospital.his.catalogs.service.SpecialtyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @PostMapping
    public ResponseEntity<SpecialtyResponse> createSpecialty(@RequestBody CreateSpecialtyRequest request) {
        return ResponseEntity.ok(specialtyService.createSpecialty(request));
    }

    @GetMapping
    public ResponseEntity<List<SpecialtyResponse>> getAllSpecialties() {
        return ResponseEntity.ok(specialtyService.getAllSpecialties());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecialtyResponse> getSpecialtyById(@PathVariable Long id) {
        return ResponseEntity.ok(specialtyService.getSpecialtyById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpecialtyResponse> updateSpecialty(@PathVariable Long id,@RequestBody UpdateSpecialtyRequest request) {
        return ResponseEntity.ok(specialtyService.updateSpecialty(id,request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SpecialtyResponse> changeStatus(@PathVariable Long id,@RequestParam Boolean active) {
        return ResponseEntity.ok(specialtyService.changeStatus(id,active));
    }
}
