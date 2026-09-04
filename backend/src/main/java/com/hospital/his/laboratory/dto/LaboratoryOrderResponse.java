package com.hospital.his.laboratory.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryOrderResponse {

    private Long orderId;

    private String orderNumber;

    private Long appointmentId;

    private Long consultationId;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String branch;

    private String status;

    private BigDecimal totalAmount;

    private String currency;

    private Boolean externalOrder;

    private String notes;

    private String createdAt;

    private String paidAt;

    private String completedAt;

    private Integer totalTests;

    private Integer publishedTests;

    private Boolean allResultsPublished;

    private List<LaboratoryOrderItemResponse> items;

    private String message;
}