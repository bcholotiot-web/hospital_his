package com.hospital.his.appointments.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Un documento pertenece a una cita.
     *
     * Se utiliza ManyToOne para permitir que en el futuro
     * una cita pueda tener más de un documento.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "appointment_id",
            nullable = false
    )
    private Appointment appointment;

    @Column(
            name = "original_name",
            nullable = false,
            length = 255
    )
    private String originalName;

    @Column(
            name = "content_type",
            nullable = false,
            length = 100
    )
    private String contentType;

    @Column(
            name = "file_size",
            nullable = false
    )
    private Long fileSize;

    /*
     * PostgreSQL almacenará el contenido como BYTEA.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(
            name = "file_content",
            nullable = false,
            columnDefinition = "BYTEA"
    )
    private byte[] fileContent;

    @Column(
            name = "uploaded_at",
            nullable = false
    )
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private Boolean active;
}