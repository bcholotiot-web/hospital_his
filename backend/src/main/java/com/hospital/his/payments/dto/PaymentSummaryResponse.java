package com.hospital.his.payments.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//Esta clase no contiene datos de tarjeta, solo devuelve la información necesaria para mostrar la pantalla de pago
public class PaymentSummaryResponse {

    private Long appointmentId;

    private String doctorName;

    private String specialty;

    private String branch;

    private String appointmentDateTime;

    private String status;

    private BigDecimal amount;

    private String currency;

    private String reservationExpiresAt;
}