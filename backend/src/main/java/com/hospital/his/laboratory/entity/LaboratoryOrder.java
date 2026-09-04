package com.hospital.his.laboratory.entity;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.medicalconsultation.entity.MedicalConsultation;
import com.hospital.his.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "laboratory_orders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_laboratory_order_number",
                        columnNames = "order_number"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryOrder {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "order_number",
            nullable = false,
            length = 30
    )
    private String orderNumber;

    @ManyToOne
    @JoinColumn(
            name = "appointment_id",
            nullable = false
    )
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(
            name = "medical_consultation_id",
            nullable = false
    )
    private MedicalConsultation medicalConsultation;

    @ManyToOne
    @JoinColumn(
            name = "patient_user_id",
            nullable = false
    )
    private User patient;

    @ManyToOne
    @JoinColumn(
            name = "doctor_user_id",
            nullable = false
    )
    private User doctor;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private LaboratoryOrderStatus status;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
            name = "external_order",
            nullable = false
    )
    private Boolean externalOrder;

    @Column(
            length = 1000
    )
    private String notes;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(
            name = "paid_at"
    )
    private LocalDateTime paidAt;

    @Column(
            name = "completed_at"
    )
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Boolean active;

    @OneToMany(
            mappedBy = "laboratoryOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<LaboratoryOrderItem> items =
            new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version;
}