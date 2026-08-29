package com.hospital.his.payments.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashAppointmentResponse {

    private Long appointmentId;

    private String patientName;

    private String patientDpi;

    private String specialty;

    private String doctorName;

    private String branch;

    private String appointmentDateTime;

    private String status;

    private BigDecimal amount;

    private String currency;

    private String message;
}