package com.hospital.his.catalogs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "branch_specialties",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "branch_id",
                                "specialty_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSpecialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "branch_id",
            nullable = false
    )
    private Branch branch;

    @ManyToOne
    @JoinColumn(
            name = "specialty_id",
            nullable = false
    )
    private Specialty specialty;

    @Column(nullable = false)
    private Boolean active;
}