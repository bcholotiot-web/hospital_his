package com.hospital.his.laboratory.repository;

import com.hospital.his.laboratory.entity.LaboratoryOrder;
import com.hospital.his.laboratory.entity.LaboratoryOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LaboratoryOrderRepository
        extends JpaRepository<LaboratoryOrder, Long> {

    Optional<LaboratoryOrder>
    findByOrderNumberIgnoreCase(
            String orderNumber
    );

    Optional<LaboratoryOrder>
    findByIdAndActiveTrue(
            Long id
    );
    Optional<LaboratoryOrder>
    findByIdAndDoctor_UsernameAndActiveTrue(
            Long orderId,
            String doctorUsername
    );

    List<LaboratoryOrder>
    findByActiveTrueOrderByCreatedAtDesc();

    List<LaboratoryOrder>
    findByDoctor_UsernameAndActiveTrueOrderByCreatedAtDesc(
            String doctorUsername
    );

    List<LaboratoryOrder>
    findByStatusAndActiveTrueOrderByCreatedAtDesc(
            LaboratoryOrderStatus status
    );

    List<LaboratoryOrder>
    findByPatient_FullNameContainingIgnoreCaseAndActiveTrueOrderByCreatedAtDesc(
            String patientName
    );

    List<LaboratoryOrder>
    findByDoctor_FullNameContainingIgnoreCaseAndActiveTrueOrderByCreatedAtDesc(
            String doctorName
    );

    List<LaboratoryOrder>
    findByAppointment_Branch_IdAndActiveTrueOrderByCreatedAtDesc(
            Long branchId
    );

    List<LaboratoryOrder>
    findByAppointment_Branch_IdAndStatusAndActiveTrueOrderByCreatedAtDesc(
            Long branchId,
            LaboratoryOrderStatus status
    );

    boolean existsByMedicalConsultation_IdAndActiveTrue(
            Long medicalConsultationId
    );


}