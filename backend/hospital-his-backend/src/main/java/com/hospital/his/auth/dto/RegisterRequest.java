package com.hospital.his.auth.dto;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class RegisterRequest {
    private String fullName;
    private String dpi;
    private String nit;
    private String phone;
    private String insuranceNumber;
    private String email;
    private String username; //entre 8 y 9 caracteres
    private String password; //minimo 12 caracteres

}
