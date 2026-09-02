package com.hospital.his.medicalconsultation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "icd10_codes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_icd10_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Icd10Code {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            length = 20
    )
    private String code;

    @Column(
            nullable = false,
            length = 500
    )
    private String description;

    @Column(nullable = false)
    private Boolean active;
}