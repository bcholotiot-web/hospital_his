package com.hospital.his.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class LoginResponse {

    private Long userId;
    private String fullName;
    private String role;
    private String token;
    private String message;
}
