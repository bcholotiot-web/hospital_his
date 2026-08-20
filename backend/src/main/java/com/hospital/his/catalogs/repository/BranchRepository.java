package com.hospital.his.catalogs.repository;

import com.hospital.his.catalogs.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    boolean existsByName(String name);
    List<Branch> findByActiveTrue();
}