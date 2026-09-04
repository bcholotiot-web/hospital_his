package com.hospital.his.laboratory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "laboratory_tests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_laboratory_test_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryTest {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            length = 30
    )
    private String code;

    @Column(
            nullable = false,
            length = 200
    )
    private String name;

    @Column(
            length = 500
    )
    private String description;

    @Column(
            name = "reference_range",
            length = 200
    )
    private String referenceRange;

    @Column(
            name = "default_unit",
            length = 50
    )
    private String defaultUnit;

    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean active;

    @Version
    @Column(nullable = false)
    private Long version;
}