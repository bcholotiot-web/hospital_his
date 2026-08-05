package com.hospital.his.users.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String dpi;
    private String nit;
    private String phone;
    private String email;
    private String username;
    private String role;
    private String insuranceNumber;
    private Boolean active;
}