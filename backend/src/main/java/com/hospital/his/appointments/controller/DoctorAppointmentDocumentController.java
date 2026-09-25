package com.hospital.his.appointments.controller;

import com.hospital.his.appointments.dto.AppointmentDocumentDownload;
import com.hospital.his.appointments.dto.AppointmentDocumentResponse;
import com.hospital.his.appointments.service.AppointmentDocumentService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(
        "/api/doctor/appointments"
)
public class DoctorAppointmentDocumentController {

    private final AppointmentDocumentService
            appointmentDocumentService;

    public DoctorAppointmentDocumentController(
            AppointmentDocumentService
                    appointmentDocumentService
    ) {
        this.appointmentDocumentService =
                appointmentDocumentService;
    }

    /**
     * Lista los metadatos de los documentos
     * asociados con una cita del médico.
     *
     * GET
     * /api/doctor/appointments/{appointmentId}/documents
     */
    @GetMapping(
            "/{appointmentId}/documents"
    )
    public ResponseEntity<
            List<AppointmentDocumentResponse>
            >
    getAppointmentDocuments(
            @PathVariable
            Long appointmentId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                appointmentDocumentService
                        .getDoctorAppointmentDocuments(
                                appointmentId,
                                authentication.getName()
                        )
        );
    }

    /**
     * Descarga el documento binario.
     *
     * El backend valida que:
     *
     * - el documento pertenezca a la cita;
     * - la cita pertenezca al médico;
     * - el documento esté activo.
     *
     * GET
     * /api/doctor/appointments/{appointmentId}
     * /documents/{documentId}/download
     */
    @GetMapping(
            "/{appointmentId}/documents/"
                    + "{documentId}/download"
    )
    public ResponseEntity<byte[]>
    downloadAppointmentDocument(
            @PathVariable
            Long appointmentId,

            @PathVariable
            Long documentId,

            Authentication authentication
    ) {
        AppointmentDocumentDownload document =
                appointmentDocumentService
                        .downloadDoctorAppointmentDocument(
                                appointmentId,
                                documentId,
                                authentication.getName()
                        );

        MediaType mediaType;

        try {
            mediaType =
                    MediaType.parseMediaType(
                            document.getContentType()
                    );

        } catch (Exception exception) {
            mediaType =
                    MediaType.APPLICATION_PDF;
        }

        ContentDisposition disposition =
                ContentDisposition
                        .inline()
                        .filename(
                                document.getOriginalName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .contentType(
                        mediaType
                )
                .contentLength(
                        document
                                .getContent()
                                .length
                )
                .body(
                        document.getContent()
                );
    }
}