package com.hospital.his.catalogs.repository;

import com.hospital.his.catalogs.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    boolean existsByName(String name);
}