package com.hospital.his.appointments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDocumentDownload {

    /**
     * Nombre utilizado en la descarga.
     */
    private String originalName;

    /**
     * Tipo del archivo.
     */
    private String contentType;

    /**
     * Contenido binario recuperado desde PostgreSQL.
     */
    private byte[] content;
}