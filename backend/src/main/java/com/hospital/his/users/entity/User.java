/**
 * usuarios representados por
 Administradores
 Médicos
 Recepcionistas
 Enfermeros
 Cajeros
 Laboratoristas
 Farmacéuticos
 Pacientes

 -------usuarios internos ------------
 Nombre completo
 Correo Electrónico
 Usuario
 Contraseña
 DPI
 Teléfono
 Rol
 NIT
 Número de Seguro
 Sucursal
 Especialidad (solo médicos)
 Estado

 ------------Usuarios externos -------------
 Nombre Completo
 DPI
 NIT
 Teléfono
 Seguro Médico
 Correo
 Usuario
 Contraseña

 */

package com.hospital.his.users.entity;

import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.entity.Specialty;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 13)
    private String dpi;

    @Column(length = 9)
    private String nit;

    @Column(length = 8)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 9)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String insuranceNumber;

    @Column(nullable = false)
    private Boolean active;

    @Column(
            name = "failed_login_attempts",
            nullable = false
    )
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;
}


