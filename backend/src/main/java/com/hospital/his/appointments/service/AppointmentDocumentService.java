package com.hospital.his.appointments.service;

import com.hospital.his.appointments.dto.AppointmentDocumentDownload;
import com.hospital.his.appointments.dto.AppointmentDocumentResponse;
import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentDocument;
import com.hospital.his.appointments.repository.AppointmentDocumentRepository;
import com.hospital.his.appointments.validation.PdfDocumentValidator;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AppointmentDocumentService {

    private static final String DOCTOR_ROLE =
            "MEDICO";

    private static final String PDF_CONTENT_TYPE =
            "application/pdf";

    private final AppointmentDocumentRepository
            appointmentDocumentRepository;

    private final PdfDocumentValidator
            pdfDocumentValidator;

    private final UserRepository
            userRepository;

    public AppointmentDocumentService(
            AppointmentDocumentRepository
                    appointmentDocumentRepository,
            PdfDocumentValidator pdfDocumentValidator,
            UserRepository userRepository
    ) {
        this.appointmentDocumentRepository =
                appointmentDocumentRepository;

        this.pdfDocumentValidator =
                pdfDocumentValidator;

        this.userRepository =
                userRepository;
    }

    /**
     * Valida un documento antes de crear la cita.
     *
     * La función delega las reglas de tipo,
     * contenido y tamaño a PdfDocumentValidator.
     */
    public void validateDocument(
            MultipartFile document
    ) {
        pdfDocumentValidator.validate(
                document
        );
    }

    /**
     * Guarda el PDF asociado con una cita.
     *
     * Si el documento es null, no realiza ninguna
     * operación.
     *
     * El contenido se almacena en PostgreSQL
     * dentro de una columna BYTEA.
     */
    @Transactional
    public void saveDocument(
            Appointment appointment,
            MultipartFile document
    ) {
        if (document == null) {
            return;
        }

        /*
         * Volver a validar antes de persistir.
         *
         * Esto protege el método si en el futuro
         * es invocado desde otro flujo.
         */
        pdfDocumentValidator.validate(
                document
        );

        try {
            String originalName =
                    sanitizeFileName(
                            document
                                    .getOriginalFilename()
                    );

            String contentType =
                    document.getContentType();

            if (
                    contentType == null ||
                            contentType.isBlank()
            ) {
                contentType =
                        PDF_CONTENT_TYPE;
            }

            AppointmentDocument
                    appointmentDocument =
                    AppointmentDocument.builder()
                            .appointment(
                                    appointment
                            )
                            .originalName(
                                    originalName
                            )
                            .contentType(
                                    contentType
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

    /**
     * Lista los documentos de una cita que pertenece
     * al médico autenticado.
     *
     * Solo devuelve metadatos. El contenido binario
     * se recupera mediante el endpoint de descarga.
     */
    @Transactional(readOnly = true)
    public List<AppointmentDocumentResponse>
    getDoctorAppointmentDocuments(
            Long appointmentId,
            String doctorUsername
    ) {
        User doctor =
                validateDoctor(
                        doctorUsername
                );

        validateAppointmentId(
                appointmentId
        );

        List<AppointmentDocument> documents =
                appointmentDocumentRepository
                        .findByAppointment_IdAndActiveTrueOrderByUploadedAtDesc(
                                appointmentId
                        );

        if (!documents.isEmpty()) {
            validateAppointmentDoctor(
                    documents.get(0)
                            .getAppointment(),
                    doctor
            );
        }

        return documents.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Recupera el contenido de un PDF para que
     * el médico pueda abrirlo o descargarlo.
     *
     * La consulta exige simultáneamente:
     *
     * - documentId correcto;
     * - appointmentId correcto;
     * - documento activo;
     * - cita asignada al médico autenticado.
     */
    @Transactional(readOnly = true)
    public AppointmentDocumentDownload
    downloadDoctorAppointmentDocument(
            Long appointmentId,
            Long documentId,
            String doctorUsername
    ) {
        User doctor =
                validateDoctor(
                        doctorUsername
                );

        validateAppointmentId(
                appointmentId
        );

        if (documentId == null) {
            throw new RuntimeException(
                    "Debe indicar el documento."
            );
        }

        AppointmentDocument document =
                appointmentDocumentRepository
                        .findByIdAndAppointment_IdAndActiveTrue(
                                documentId,
                                appointmentId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Documento de la cita "
                                                + "no encontrado."
                                )
                        );

        validateAppointmentDoctor(
                document.getAppointment(),
                doctor
        );

        byte[] content =
                document.getFileContent();

        if (
                content == null ||
                        content.length == 0
        ) {
            throw new RuntimeException(
                    "El documento no contiene información "
                            + "disponible para descargar."
            );
        }

        String contentType =
                document.getContentType();

        if (
                contentType == null ||
                        contentType.isBlank()
        ) {
            contentType =
                    PDF_CONTENT_TYPE;
        }

        return AppointmentDocumentDownload
                .builder()
                .originalName(
                        sanitizeFileName(
                                document
                                        .getOriginalName()
                        )
                )
                .contentType(
                        contentType
                )
                .content(
                        content
                )
                .build();
    }

    /**
     * Comprueba si la cita tiene documentos.
     *
     * Este método se utiliza para construir
     * MedicalConsultationResponse.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveDocuments(
            Long appointmentId
    ) {
        if (appointmentId == null) {
            return false;
        }

        return appointmentDocumentRepository
                .existsByAppointment_IdAndActiveTrue(
                        appointmentId
                );
    }

    /**
     * Cuenta los documentos activos de una cita.
     */
    @Transactional(readOnly = true)
    public long countActiveDocuments(
            Long appointmentId
    ) {
        if (appointmentId == null) {
            return 0L;
        }

        return appointmentDocumentRepository
                .countByAppointment_IdAndActiveTrue(
                        appointmentId
                );
    }

    /**
     * Obtiene metadatos de documentos sin realizar
     * una segunda validación del médico.
     *
     * Debe utilizarse únicamente desde servicios que
     * ya validaron la propiedad de la cita, como
     * MedicalConsultationService.
     */
    @Transactional(readOnly = true)
    public List<AppointmentDocumentResponse>
    getDocumentMetadata(
            Long appointmentId
    ) {
        if (appointmentId == null) {
            return List.of();
        }

        return appointmentDocumentRepository
                .findByAppointment_IdAndActiveTrueOrderByUploadedAtDesc(
                        appointmentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }
    /**
     * Valida que el usuario autenticado sea
     * un médico activo.
     */
    private User validateDoctor(
            String username
    ) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al médico autenticado."
            );
        }

        User doctor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Médico autenticado no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        doctor.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del médico se encuentra inactiva."
            );
        }

        if (
                doctor.getRole() == null ||
                        !Boolean.TRUE.equals(
                                doctor
                                        .getRole()
                                        .getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El médico no tiene un rol activo asignado."
            );
        }

        String roleName =
                doctor.getRole()
                        .getName();

        if (
                roleName == null ||
                        !DOCTOR_ROLE.equals(
                                normalizeRole(roleName)
                        )
        ) {
            throw new RuntimeException(
                    "Solamente un médico puede consultar "
                            + "los documentos clínicos de una cita."
            );
        }

        return doctor;
    }

    /**
     * Comprueba que la cita pertenezca al mismo
     * médico autenticado.
     */
    private void validateAppointmentDoctor(
            Appointment appointment,
            User doctor
    ) {
        if (appointment == null) {
            throw new RuntimeException(
                    "La cita asociada al documento "
                            + "no se encuentra disponible."
            );
        }

        if (
                appointment.getDoctor() == null ||
                        !appointment
                                .getDoctor()
                                .getId()
                                .equals(
                                        doctor.getId()
                                )
        ) {
            throw new RuntimeException(
                    "El documento no pertenece a una cita "
                            + "asignada al médico autenticado."
            );
        }

        if (
                !Boolean.TRUE.equals(
                        appointment.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cita asociada al documento "
                            + "se encuentra inactiva."
            );
        }
    }

    /**
     * Valida el identificador de cita.
     */
    private void validateAppointmentId(
            Long appointmentId
    ) {
        if (appointmentId == null) {
            throw new RuntimeException(
                    "Debe indicar la cita médica."
            );
        }
    }

    /**
     * Elimina segmentos de ruta del nombre obtenido.
     *
     * Ejemplo:
     *
     * C:\documentos\resultado.pdf
     *
     * se convierte en:
     *
     * resultado.pdf
     */
    private String sanitizeFileName(
            String fileName
    ) {
        if (
                fileName == null ||
                        fileName.isBlank()
        ) {
            return "documento.pdf";
        }

        String normalized =
                fileName.replace("\\", "/");

        int lastSeparator =
                normalized.lastIndexOf("/");

        String cleanName =
                lastSeparator >= 0
                        ? normalized.substring(
                        lastSeparator + 1
                )
                        : normalized;

        cleanName =
                cleanName.replaceAll(
                        "[\\r\\n\"]",
                        "_"
                );

        if (cleanName.isBlank()) {
            return "documento.pdf";
        }

        return cleanName;
    }

    /**
     * Formatea el tamaño para mostrarlo al médico.
     */
    private String formatFileSize(
            Long fileSize
    ) {
        if (fileSize == null) {
            return "0 bytes";
        }

        if (fileSize < 1024) {
            return fileSize + " bytes";
        }

        double kilobytes =
                fileSize / 1024.0;

        if (kilobytes < 1024) {
            return String.format(
                    Locale.US,
                    "%.2f KB",
                    kilobytes
            );
        }

        double megabytes =
                kilobytes / 1024.0;

        return String.format(
                Locale.US,
                "%.2f MB",
                megabytes
        );
    }

    /**
     * Convierte la entidad en metadatos seguros.
     */
    private AppointmentDocumentResponse toResponse(
            AppointmentDocument document
    ) {
        Long appointmentId =
                document
                        .getAppointment()
                        .getId();

        return AppointmentDocumentResponse
                .builder()
                .documentId(
                        document.getId()
                )
                .appointmentId(
                        appointmentId
                )
                .originalName(
                        document
                                .getOriginalName()
                )
                .contentType(
                        document
                                .getContentType()
                )
                .fileSize(
                        document
                                .getFileSize()
                )
                .formattedSize(
                        formatFileSize(
                                document
                                        .getFileSize()
                        )
                )
                .uploadedAt(
                        document
                                .getUploadedAt()
                                .toString()
                )
                .downloadUrl(
                        "/api/doctor/appointments/"
                                + appointmentId
                                + "/documents/"
                                + document.getId()
                                + "/download"
                )
                .build();
    }

    /**
     * Normaliza el rol y elimina tildes.
     */
    private String normalizeRole(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return java.text.Normalizer
                .normalize(
                        value,
                        java.text.Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }
}