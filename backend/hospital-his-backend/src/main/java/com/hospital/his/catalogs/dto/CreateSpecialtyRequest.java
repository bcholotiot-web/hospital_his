package com.hospital.his.catalogs.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSpecialtyRequest {

    private String name;

    private String description;

    private Boolean active;
}