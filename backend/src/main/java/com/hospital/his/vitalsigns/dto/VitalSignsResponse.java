package com.hospital.his.vitalsigns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSignsResponse {

    private Long vitalSignsId;

    private String sourceType;

    private Long emergencyReceptionId;

    private Long appointmentId;

    private String patientName;

    private String patientDpi;

    private String nurseName;

    private Integer systolicPressure;

    private Integer diastolicPressure;

    private BigDecimal temperature;

    private BigDecimal weight;

    private BigDecimal height;

    private Integer heartRate;

    private Boolean emergency;

    private List<String> clinicalAlerts;

    private String recordedAt;

    private String appointmentStatus;

    private String message;
}