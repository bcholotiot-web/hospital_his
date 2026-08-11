package com.hospital.his.patients.repository;

import com.hospital.his.patients.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long>{
}