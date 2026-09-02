package com.hospital.his.medicalconsultation.repository;

import com.hospital.his.medicalconsultation.entity.Icd10Code;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface Icd10CodeRepository
        extends JpaRepository<Icd10Code, Long> {

    @Query("""
            SELECT code
            FROM Icd10Code code
            WHERE code.active = true
              AND (
                    LOWER(code.code) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(code.description) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY code.code ASC
            """)
    List<Icd10Code> searchActiveCodes(
            @Param("query") String query,
            Pageable pageable
    );

    Optional<Icd10Code> findByCodeIgnoreCaseAndActiveTrue(
            String code
    );
}