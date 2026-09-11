package com.hospital.his.appointments.validation;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/*Extensión
MIME
Tamaño
Firma %PDF-
Estructura interna
Cifrado
Número de páginas*/
@Component
public class PdfDocumentValidator {

    private static final long MAX_FILE_SIZE =
            2L * 1024L * 1024L;

    public static final String INVALID_DOCUMENT_MESSAGE =
            "El documento debe ser un archivo PDF válido, "
                    + "no encriptado y con tamaño máximo de 2 MB.";

    /**
     * El documento es opcional.
     *
     * Si existe una parte multipart, debe contener un PDF
     * válido, no vacío, no cifrado y de máximo 2 MB.
     */
    public void validate(MultipartFile document) {
        /*
         * La ausencia completa del documento está permitida.
         */
        if (document == null) {
            return;
        }

        validateBasicProperties(document);

        byte[] content;

        try {
            content = document.getBytes();
        } catch (IOException exception) {
            throw invalidDocumentException();
        }

        validatePdfSignature(content);
        validatePdfStructure(content);
    }

    private void validateBasicProperties(MultipartFile document) {
        if (
                document.isEmpty()
                        || document.getSize() <= 0
                        || document.getSize() > MAX_FILE_SIZE
        ) {
            throw invalidDocumentException();
        }

        String originalFilename =
                document.getOriginalFilename();

        boolean validExtension =
                originalFilename != null
                        && originalFilename
                        .toLowerCase(Locale.ROOT)
                        .endsWith(".pdf");

        boolean validContentType =
                MediaType.APPLICATION_PDF_VALUE
                        .equalsIgnoreCase(
                                document.getContentType()
                        );

        if (!validExtension || !validContentType) {
            throw invalidDocumentException();
        }
    }

    /**
     * Verifica la firma binaria inicial %PDF-.
     *
     * Esto evita aceptar archivos que solamente fueron
     * renombrados con extensión .pdf.
     */
    private void validatePdfSignature(byte[] content) {
        if (content.length < 5) {
            throw invalidDocumentException();
        }

        String signature = new String(
                content,
                0,
                5,
                StandardCharsets.US_ASCII
        );

        if (!"%PDF-".equals(signature)) {
            throw invalidDocumentException();
        }
    }

    /**
     * PDFBox intenta abrir el documento.
     *
     * Si está dañado, protegido con contraseña o cifrado,
     * la validación falla.
     */
    private void validatePdfStructure(byte[] content) {
        try (
                PDDocument pdfDocument =
                        Loader.loadPDF(content)
        ) {
            if (pdfDocument.isEncrypted()) {
                throw invalidDocumentException();
            }

            /*
             * Debe contener al menos una página.
             */
            if (pdfDocument.getNumberOfPages() <= 0) {
                throw invalidDocumentException();
            }

        } catch (IOException exception) {
            throw invalidDocumentException();
        }
    }

    private RuntimeException
    invalidDocumentException() {
        return new RuntimeException(
                INVALID_DOCUMENT_MESSAGE
        );
    }
}