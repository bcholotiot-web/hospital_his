package com.hospital.his.catalogs.dto;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UpdateSpecialtyRequest {
    private String name;
    private String description;
    private Boolean active;
}


