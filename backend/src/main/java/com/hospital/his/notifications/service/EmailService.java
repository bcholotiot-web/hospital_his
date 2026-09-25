package com.hospital.his.notifications.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${hospital.mail.from}")
    private String fromEmail;

    @Value("${hospital.mail.name:Hospital HIS}")
    private String hospitalName;

    public EmailService(
            JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    /**
     * CU-02, flujo normal, paso 14:
     * Envía el correo de bienvenida después de completar
     * correctamente el registro del usuario externo.
     */
    public void sendWelcomeEmail(
            String recipientEmail,
            String fullName
    ) {
        validateEmailData(
                recipientEmail,
                fullName
        );

        String subject = "Bienvenido al Sistema de Citas - Hospital " + fullName;

        String body = "Estimado(a) " + fullName + ",\n\n"
                        + "Su registro ha sido completado exitosamente. "
                        + "Ya puede agendar sus citas médicas a través "
                        + "de nuestro portal.\n\n"
                        + "Atentamente,\n"
                        + hospitalName;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new RuntimeException(
                    "El usuario fue registrado, pero no fue posible "
                            + "enviar el correo electrónico de bienvenida.",
                    exception
            );
        }
    }

    /**
     * CU-12:
     * Envía la confirmación de la nueva cita
     * de seguimiento.
     *
     * Este método debe ejecutarse después de que
     * la transacción de creación haya sido confirmada.
     */
    public void sendFollowUpAppointmentConfirmation(
            String recipientEmail,
            String patientName,
            String doctorName,
            String specialtyName,
            String branchName,
            LocalDateTime appointmentDateTime,
            String followUpType,
            String reason
    ) {
        validateEmailData(
                recipientEmail,
                patientName
        );

        String formattedDate =
                formatAppointmentDateTime(
                        appointmentDateTime
                );

        String readableType =
                formatEnumText(
                        followUpType
                );

        String subject =
                "Cita de seguimiento programada - "
                        + hospitalName;

        String body =
                "Estimado(a) "
                        + patientName
                        + ",\n\n"
                        + "Se ha programado una cita de "
                        + "seguimiento médico.\n\n"
                        + "Tipo de seguimiento: "
                        + readableType
                        + "\n"
                        + "Médico: "
                        + doctorName
                        + "\n"
                        + "Especialidad: "
                        + specialtyName
                        + "\n"
                        + "Sucursal: "
                        + branchName
                        + "\n"
                        + "Fecha y hora: "
                        + formattedDate
                        + "\n"
                        + "Motivo: "
                        + reason
                        + "\n\n"
                        + "Atentamente,\n"
                        + hospitalName;

        sendSimpleEmail(
                recipientEmail,
                subject,
                body,
                "La cita fue creada, pero no fue posible "
                        + "enviar el correo de confirmación."
        );
    }

    /**
     * CU-12:
     * Envía el recordatorio programado antes
     * de la cita de seguimiento.
     */
    public void sendFollowUpAppointmentReminder(
            String recipientEmail,
            String patientName,
            String doctorName,
            String branchName,
            LocalDateTime appointmentDateTime
    ) {
        validateEmailData(
                recipientEmail,
                patientName
        );

        String subject =
                "Recordatorio de cita de seguimiento - "
                        + hospitalName;

        String body =
                "Estimado(a) "
                        + patientName
                        + ",\n\n"
                        + "Le recordamos su cita de seguimiento.\n\n"
                        + "Médico: "
                        + doctorName
                        + "\n"
                        + "Sucursal: "
                        + branchName
                        + "\n"
                        + "Fecha y hora: "
                        + formatAppointmentDateTime(
                        appointmentDateTime
                )
                        + "\n\n"
                        + "Atentamente,\n"
                        + hospitalName;

        sendSimpleEmail(
                recipientEmail,
                subject,
                body,
                "No fue posible enviar el recordatorio "
                        + "de la cita."
        );
    }

    /**
     * Centraliza el envío de correos simples.
     */
    private void sendSimpleEmail(
            String recipientEmail,
            String subject,
            String body,
            String failureMessage
    ) {
        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(
                fromEmail
        );

        message.setTo(
                recipientEmail
        );

        message.setSubject(
                subject
        );

        message.setText(
                body
        );

        try {
            mailSender.send(
                    message
            );

        } catch (MailException exception) {
            throw new RuntimeException(
                    failureMessage,
                    exception
            );
        }
    }

    /**
     * Presenta la fecha en español de Guatemala.
     */
    private String formatAppointmentDateTime(LocalDateTime value) {
        if (value == null) {
            return "No disponible";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return value.format(formatter);
    }

    /**
     * Convierte valores de enum en texto legible.
     */
    private String formatEnumText(String value) {
        if (value == null || value.isBlank()) {
            return "No disponible";
        }
        return value.replace("_", " ");
    }


    private void validateEmailData(
            String recipientEmail,
            String fullName
    ) {
        if (
                recipientEmail == null ||
                        recipientEmail.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El correo del destinatario es obligatorio."
            );
        }

        if (
                fullName == null ||
                        fullName.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El nombre del destinatario es obligatorio."
            );
        }
    }
}