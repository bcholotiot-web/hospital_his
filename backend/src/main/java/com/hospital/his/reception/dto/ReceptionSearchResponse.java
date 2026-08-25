/*Cita encontrada
Paciente no registrado
Paciente registrado sin citas activas*/

package com.hospital.his.reception.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionSearchResponse {
    private String resultType;
    private String message;
    private String subText;
    private Boolean showRegisterPatientButton;
    private Boolean showNewAppointmentButton;
    private ReceptionAppointmentResponse appointment;
}