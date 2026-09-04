package com.hospital.his.laboratory.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryOrderSummaryResponse {

    private Long orderId;

    private String orderNumber;

    private String patientName;

    private String doctorName;

    private String branch;

    private String status;

    private BigDecimal totalAmount;

    private Boolean externalOrder;

    private Integer totalTests;

    private Integer publishedTests;

    private String createdAt;
}