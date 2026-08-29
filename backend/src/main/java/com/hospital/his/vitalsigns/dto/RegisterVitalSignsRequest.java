package com.hospital.his.vitalsigns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterVitalSignsRequest {

    /*
     * Valores permitidos:
     * APPOINTMENT
     * EMERGENCY_RECEPTION
     */
    private String sourceType;

    private Long appointmentId;

    private Long emergencyReceptionId;

    private Integer systolicPressure;

    private Integer diastolicPressure;

    private BigDecimal temperature;

    private BigDecimal weight;

    private BigDecimal height;

    private Integer heartRate;

    private Boolean emergency;
}