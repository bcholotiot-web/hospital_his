package com.hospital.his.catalogs.dto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyResponse {

    private Long id;

    private String name;

    private String description;

    private Boolean active;
}