package com.hospital.his.appointments.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatusPresentationResponse {

    private String code;

    private String displayName;

    private String description;

    private String color;

    private Integer displayOrder;

    private Boolean configured;
}