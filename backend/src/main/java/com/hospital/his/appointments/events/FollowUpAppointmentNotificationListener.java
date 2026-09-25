package com.hospital.his.appointments.events;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.appointments.service.AppointmentReminderService;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.notifications.service.EmailService;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FollowUpAppointmentNotificationListener {

    private final AppointmentRepository
            appointmentRepository;

    private final AppointmentReminderService
            appointmentReminderService;

    private final EmailService emailService;

    private final AuditService auditService;

    public FollowUpAppointmentNotificationListener(
            AppointmentRepository appointmentRepository,
            AppointmentReminderService
                    appointmentReminderService,
            EmailService emailService,
            AuditService auditService
    ) {
        this.appointmentRepository =
                appointmentRepository;

        this.appointmentReminderService =
                appointmentReminderService;

        this.emailService =
                emailService;

        this.auditService =
                auditService;
    }

    /**
     * Se ejecuta únicamente después de confirmar
     * la transacción que creó la cita.
     *
     * Si el correo falla, la cita ya guardada no
     * se elimina ni se revierte.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void onFollowUpAppointmentCreated(
            FollowUpAppointmentCreatedEvent event
    ) {
        Appointment appointment =
                appointmentRepository
                        .findById(
                                event.appointmentId()
                        )
                        .orElse(null);

        if (appointment == null) {
            return;
        }

        /*
         * Crear el recordatorio aunque el correo de
         * confirmación falle.
         */
        appointmentReminderService
                .createReminder(
                        appointment
                );

        try {
            emailService
                    .sendFollowUpAppointmentConfirmation(
                            appointment
                                    .getPatient()
                                    .getEmail(),
                            appointment
                                    .getPatient()
                                    .getFullName(),
                            appointment
                                    .getDoctor()
                                    .getFullName(),
                            appointment
                                    .getSpecialty()
                                    .getName(),
                            appointment
                                    .getBranch()
                                    .getName(),
                            appointment
                                    .getAppointmentDateTime(),
                            appointment
                                    .getFollowUpType()
                                    .name(),
                            appointment
                                    .getReason()
                    );

            auditService.log(
                    appointment
                            .getDoctor()
                            .getUsername(),
                    "SEND_FOLLOW_UP_CONFIRMATION",
                    "APPOINTMENTS",
                    "Correo de confirmación enviado "
                            + "para cita de seguimiento ID "
                            + appointment.getId()
                            + "."
            );

        } catch (RuntimeException exception) {
            /*
             * El correo es una acción secundaria.
             * La cita permanece creada.
             */
            auditService.log(
                    appointment
                            .getDoctor()
                            .getUsername(),
                    "FOLLOW_UP_CONFIRMATION_EMAIL_ERROR",
                    "APPOINTMENTS",
                    "La cita de seguimiento ID "
                            + appointment.getId()
                            + " fue creada, pero el correo "
                            + "de confirmación no pudo enviarse."
            );
        }
    }
}