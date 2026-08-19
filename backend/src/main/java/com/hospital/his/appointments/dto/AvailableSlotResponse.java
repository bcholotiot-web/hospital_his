package com.hospital.his.appointments.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlotResponse {

    private String dateTime;

    private String time;

    private Boolean available;
}