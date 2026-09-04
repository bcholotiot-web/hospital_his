package com.hospital.his.laboratory.repository;

import com.hospital.his.laboratory.entity.LaboratoryTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LaboratoryTestRepository
        extends JpaRepository<LaboratoryTest, Long> {

    List<LaboratoryTest>
    findByActiveTrueOrderByNameAsc();

    Optional<LaboratoryTest>
    findByIdAndActiveTrue(
            Long id
    );

    boolean existsByCodeIgnoreCase(
            String code
    );
}