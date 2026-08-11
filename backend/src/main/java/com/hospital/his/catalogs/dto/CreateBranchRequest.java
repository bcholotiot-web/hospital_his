package com.hospital.his.catalogs.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchRequest {

    private String name;

    private String address;

    private Boolean active;
}