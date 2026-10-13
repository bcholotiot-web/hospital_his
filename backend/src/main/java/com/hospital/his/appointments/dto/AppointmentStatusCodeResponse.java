package com.hospital.his.appointments.dto;

import lombok.*;

/**
 * Describe un código técnico disponible
 * en AppointmentStatus.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatusCodeResponse {

    private String code;

    private String suggestedDisplayName;

    /**
     * Indica si el código ya tiene una
     * configuración persistente.
     */
    private Boolean configured;

    private Long configurationId;

    private Boolean active;
}