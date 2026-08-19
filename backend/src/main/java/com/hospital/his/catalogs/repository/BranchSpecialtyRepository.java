package com.hospital.his.catalogs.repository;

import com.hospital.his.catalogs.entity.BranchSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchSpecialtyRepository
        extends JpaRepository<BranchSpecialty, Long> {

    List<BranchSpecialty> findByBranch_IdAndActiveTrue(
            Long branchId
    );

    boolean existsByBranch_IdAndSpecialty_Id(
            Long branchId,
            Long specialtyId
    );
}
