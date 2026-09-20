package com.hospital.his.pharmacy.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

//Respuesta final del despacho
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationDispensationResponse {

    private Long dispensationId;

    private String dispensationNumber;

    private Long prescriptionId;

    private String prescriptionNumber;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String branch;

    private String dispensationStatus;

    private String prescriptionStatus;

    private BigDecimal totalAmount;

    private String currency;

    private String paymentMethod;

    private String paymentStatus;

    private BigDecimal receivedAmount;

    private BigDecimal changeAmount;

    private String cardLastFour;

    private String transactionNumber;

    private String pharmacyUsername;

    private String confirmedAt;

    private Integer totalItems;

    private List<MedicationDispensationItemResponse>
            items;

    /**
     * Alertas generadas después de descontar
     * las existencias.
     */
    private List<String> stockAlerts;

    private String message;
}