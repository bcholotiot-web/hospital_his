package com.hospital.his.laboratory.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryOrderItemResponse {

    private Long itemId;

    private Long laboratoryTestId;

    private String testCode;

    private String testName;

    private String referenceRange;

    private BigDecimal unitPrice;

    private String status;

    private String resultValue;

    private String resultUnit;

    private String resultDate;

    private Boolean outOfRange;

    private String resultNotes;

    private Boolean published;

    private String resultSavedAt;

    private String publishedAt;

    private Boolean canSaveResult;

    private Boolean canPublishResult;
}