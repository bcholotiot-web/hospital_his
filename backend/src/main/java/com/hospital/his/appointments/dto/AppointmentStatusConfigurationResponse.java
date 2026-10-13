package com.hospital.his.appointments.dto;

import lombok.*;

/**
 * Respuesta completa de una configuración
 * de estado de cita.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatusConfigurationResponse {

    private Long id;

    private String code;

    private String displayName;

    private String description;

    private Integer displayOrder;

    private String color;

    private Boolean patientVisible;

    private Boolean staffVisible;

    private Boolean active;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;

    private String message;
}