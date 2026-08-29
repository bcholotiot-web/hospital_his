package com.hospital.his.vitalsigns.entity;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.users.entity.User;
import com.hospital.his.reception.entity.EmergencyReception;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vital_signs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSigns {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @OneToOne
    @JoinColumn(
            name = "appointment_id",
            unique = true
    )
    private Appointment appointment;

    @OneToOne
    @JoinColumn(
            name ="emergency_reception_id",
            unique = true
    )
    private EmergencyReception emergencyReception;

    @ManyToOne
    @JoinColumn(
            name = "nurse_user_id",
            nullable = false
    )
    private User nurse;

    @Column(
            name = "systolic_pressure",
            nullable = false
    )
    private Integer systolicPressure;

    @Column(
            name = "diastolic_pressure",
            nullable = false
    )
    private Integer diastolicPressure;

    @Column(
            nullable = false,
            precision = 4,
            scale = 1
    )
    private BigDecimal temperature;

    @Column(
            nullable = false,
            precision = 5,
            scale = 1
    )
    private BigDecimal weight;

    @Column(
            nullable = false,
            precision = 5,
            scale = 1
    )
    private BigDecimal height;

    @Column(
            name = "heart_rate",
            nullable = false
    )
    private Integer heartRate;

    @Column(
            name = "is_emergency",
            nullable = false
    )
    private Boolean emergency;

    @Column(
            name = "clinical_alerts",
            length = 2000
    )
    private String clinicalAlerts;

    @Column(
            name = "recorded_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime recordedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}