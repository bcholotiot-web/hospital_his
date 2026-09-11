package com.hospital.his.laboratorypayment.repository;

import com.hospital.his.laboratorypayment.entity.LaboratoryPayment;
import com.hospital.his.payments.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LaboratoryPaymentRepository
        extends JpaRepository<LaboratoryPayment, Long> {


    //Evita procesar dos veces la misma solicitud
    Optional<LaboratoryPayment>
    findByIdempotencyKey(
            UUID idempotencyKey
    );

    Optional<LaboratoryPayment>
    findByTransactionNumber(
            String transactionNumber
    );


    //Comprueba si la orden ya tiene un estado APROBADO
    boolean existsByLaboratoryOrder_IdAndStatus(
            Long laboratoryOrderId,
            PaymentStatus status
    );

    Optional<LaboratoryPayment>
    findFirstByLaboratoryOrder_IdAndStatusOrderByProcessedAtDesc(
            Long laboratoryOrderId,
            PaymentStatus status
    );

    List<LaboratoryPayment>
    findByLaboratoryOrder_IdOrderByCreatedAtDesc(
            Long laboratoryOrderId
    );

    List<LaboratoryPayment>
    findByCashierUsernameOrderByCreatedAtDesc(
            String cashierUsername
    );
}