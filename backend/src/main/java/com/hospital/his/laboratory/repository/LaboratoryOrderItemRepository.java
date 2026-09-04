package com.hospital.his.laboratory.repository;

import com.hospital.his.laboratory.entity.LaboratoryOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LaboratoryOrderItemRepository
        extends JpaRepository<LaboratoryOrderItem, Long> {

    List<LaboratoryOrderItem>
    findByLaboratoryOrder_IdOrderByIdAsc(
            Long laboratoryOrderId
    );

    Optional<LaboratoryOrderItem>
    findByIdAndLaboratoryOrder_Id(
            Long itemId,
            Long laboratoryOrderId
    );

    boolean existsByLaboratoryOrder_IdAndPublishedFalse(
            Long laboratoryOrderId
    );
}