package com.hospital.his.auth.dto;

import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class RegisterResponse {
    private Long userId;
    private String message;
}
