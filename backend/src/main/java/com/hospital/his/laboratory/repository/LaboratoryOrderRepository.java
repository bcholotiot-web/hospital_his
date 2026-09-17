package com.hospital.his.laboratory.repository;

import com.hospital.his.laboratory.entity.LaboratoryOrder;
import com.hospital.his.laboratory.entity.LaboratoryOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
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

    /*Evita crear mas de una orden activa para la misma consulta*/
    boolean existsByMedicalConsultation_IdAndActiveTrue(
            Long medicalConsultationId
    );

    /**
     * CU-10:
     * Busca órdenes activas y pendientes de pago
     * mediante el DPI exacto del paciente.
     */
    List<LaboratoryOrder> findByPatient_DpiAndStatusAndActiveTrueOrderByCreatedAtDesc(
            String patientDpi,
            LaboratoryOrderStatus status
    );

    /**
     * CU-10:
     * Busca una orden pendiente por su número.
     */
    Optional<LaboratoryOrder> findByOrderNumberIgnoreCaseAndStatusAndActiveTrue(
            String orderNumber,
            LaboratoryOrderStatus status
    );

    /**
     * CU-10:
     * Recupera y bloquea temporalmente la fila de la orden
     * mientras se registra el cobro.
     *
     * Esto evita que dos cajeros procesen la misma orden
     * simultáneamente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT laboratoryOrder
    FROM LaboratoryOrder laboratoryOrder
    WHERE laboratoryOrder.id = :orderId
      AND laboratoryOrder.active = true
""")
    Optional<LaboratoryOrder> findByIdForPayment(
            @Param("orderId")
            Long orderId
    );


}