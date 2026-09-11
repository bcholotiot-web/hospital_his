package com.hospital.his.notifications.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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