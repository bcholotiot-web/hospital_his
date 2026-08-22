package com.hospital.his.payments.repository;

import com.hospital.his.payments.entity.Payment;
import com.hospital.his.payments.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    //detecta solicitudes duplicadas
    Optional<Payment> findByIdempotencyKey(
            UUID idempotencyKey
    );

    //permite consultar un comprobante
    Optional<Payment> findByTransactionNumber(
            String transactionNumber
    );

    //evita pagar dos veces una cita
    boolean existsByAppointment_IdAndStatus(
            Long appointmentId,
            PaymentStatus status
    );
}