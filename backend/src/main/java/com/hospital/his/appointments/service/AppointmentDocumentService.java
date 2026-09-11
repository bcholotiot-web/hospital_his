package com.hospital.his.appointments.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentDocument;
import com.hospital.his.appointments.repository.AppointmentDocumentRepository;
import com.hospital.his.appointments.validation.PdfDocumentValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class AppointmentDocumentService {

    private final AppointmentDocumentRepository appointmentDocumentRepository;
    private final PdfDocumentValidator pdfDocumentValidator;

    public AppointmentDocumentService(
            AppointmentDocumentRepository appointmentDocumentRepository,
            PdfDocumentValidator pdfDocumentValidator
    ) {
        this.appointmentDocumentRepository = appointmentDocumentRepository;
        this.pdfDocumentValidator = pdfDocumentValidator;
    }

    /**
     * Valida un documento antes de crear la cita.
     */
    public void validateDocument(MultipartFile document) {
        pdfDocumentValidator.validate(document);
    }

    /**
     * Guarda el documento asociado a la cita.
     * Si document es null, no realiza ninguna operación.
     */
    public void saveDocument(Appointment appointment, MultipartFile document) {
        if (document == null) {
            return;
        }

        try {
            AppointmentDocument appointmentDocument =
                    AppointmentDocument.builder()
                            .appointment(appointment)
                            .originalName(
                                    document
                                            .getOriginalFilename()
                            )
                            .contentType(
                                    document.getContentType()
                            )
                            .fileSize(
                                    document.getSize()
                            )
                            .fileContent(
                                    document.getBytes()
                            )
                            .uploadedAt(
                                    LocalDateTime.now()
                            )
                            .active(true)
                            .build();

            appointmentDocumentRepository.save(
                    appointmentDocument
            );

        } catch (IOException exception) {
            throw new RuntimeException(
                    PdfDocumentValidator
                            .INVALID_DOCUMENT_MESSAGE
            );
        }
    }
}