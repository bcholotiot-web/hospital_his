package com.hospital.his.users.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    private String fullName;
    private String dpi;
    private String nit;
    private String phone;
    private String email;
    private String username;
    private String password;
    private Long roleId;
    private String insuranceNumber;
    private Long branchId;
    private Long specialtyId;
    private Boolean active;

}