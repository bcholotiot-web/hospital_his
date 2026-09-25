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
public class AppointmentDocumentResponse {

  /**
   * Identificador utilizado para solicitar
   * posteriormente la descarga del documento.
   */
  private Long documentId;

  /**
   * Cita a la que pertenece el documento.
   */
  private Long appointmentId;

  /**
   * Nombre original recibido desde el paciente.
   */
  private String originalName;

  /**
   * Tipo MIME del documento.
   *
   * Para este flujo debe ser application/pdf.
   */
  private String contentType;

  /**
   * Tamaño total en bytes.
   */
  private Long fileSize;

  /**
   * Tamaño legible para mostrar en React.
   *
   * Ejemplo:
   * 245.60 KB
   */
  private String formattedSize;

  /**
   * Fecha y hora de carga.
   */
  private String uploadedAt;

  /**
   * URL relativa que puede utilizar React
   * para descargar el archivo.
   */
  private String downloadUrl;
}