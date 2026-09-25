package com.hospital.his.appointments.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentReminder;
import com.hospital.his.appointments.entity.AppointmentReminderStatus;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentReminderRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.notifications.service.EmailService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AppointmentReminderService {

    private static final Set<AppointmentStatus>
            NON_REMINDABLE_STATUSES =
            Set.of(
                    AppointmentStatus.CANCELADA,
                    AppointmentStatus.EXPIRADA,
                    AppointmentStatus.NO_ASISTIO,
                    AppointmentStatus.ATENCION_FINALIZADA
            );

    private final AppointmentReminderRepository
            appointmentReminderRepository;

    private final EmailService emailService;

    private final AuditService auditService;

    public AppointmentReminderService(
            AppointmentReminderRepository
                    appointmentReminderRepository,
            EmailService emailService,
            AuditService auditService
    ) {
        this.appointmentReminderRepository =
                appointmentReminderRepository;

        this.emailService =
                emailService;

        this.auditService =
                auditService;
    }

    /**
     * Crea un recordatorio persistente para la cita.
     *
     * El recordatorio se programa 24 horas antes.
     * Si la cita se creó con menos de 24 horas,
     * se programa cinco minutos después de la creación.
     */
    @Transactional
    public AppointmentReminder createReminder(
            Appointment appointment
    ) {
        if (appointment == null) {
            throw new RuntimeException(
                    "La cita es obligatoria para crear "
                            + "el recordatorio."
            );
        }

        AppointmentReminder existing =
                appointmentReminderRepository
                        .findByAppointment_Id(
                                appointment.getId()
                        )
                        .orElse(null);

        if (existing != null) {
            return existing;
        }

        LocalDateTime now =
                LocalDateTime.now();

        LocalDateTime sendAt =
                appointment
                        .getAppointmentDateTime()
                        .minusHours(24);

        if (!sendAt.isAfter(now)) {
            sendAt =
                    now.plusMinutes(5);
        }

        /*
         * El recordatorio nunca debe quedar después
         * de la cita.
         */
        if (
                !sendAt.isBefore(
                        appointment
                                .getAppointmentDateTime()
                )
        ) {
            sendAt =
                    appointment
                            .getAppointmentDateTime()
                            .minusMinutes(5);
        }

        AppointmentReminder reminder =
                AppointmentReminder.builder()
                        .appointment(
                                appointment
                        )
                        .sendAt(
                                sendAt
                        )
                        .status(
                                AppointmentReminderStatus
                                        .PENDIENTE
                        )
                        .attemptCount(0)
                        .sentAt(null)
                        .lastError(null)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        return appointmentReminderRepository
                .saveAndFlush(reminder);
    }

    /**
     * Busca recordatorios vencidos cada minuto.
     *
     * fixedDelay evita ejecutar una nueva ronda antes
     * de que termine la anterior.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processPendingReminders() {
        List<AppointmentReminder> reminders =
                appointmentReminderRepository
                        .findTop50ByStatusAndSendAtLessThanEqualOrderBySendAtAsc(
                                AppointmentReminderStatus
                                        .PENDIENTE,
                                LocalDateTime.now()
                        );

        for (
                AppointmentReminder reminder
                : reminders
        ) {
            processReminder(
                    reminder
            );
        }
    }

    /**
     * Procesa un recordatorio individual.
     *
     * Si la cita fue cancelada, expirada, finalizada
     * o desactivada, cancela el recordatorio.
     */
    private void processReminder(
            AppointmentReminder reminder
    ) {
        Appointment appointment =
                reminder.getAppointment();

        if (
                appointment == null ||
                        !Boolean.TRUE.equals(
                                appointment.getActive()
                        ) ||
                        NON_REMINDABLE_STATUSES.contains(
                                appointment.getStatus()
                        )
        ) {
            reminder.setStatus(
                    AppointmentReminderStatus
                            .CANCELADO
            );

            reminder.setUpdatedAt(
                    LocalDateTime.now()
            );

            appointmentReminderRepository.save(
                    reminder
            );

            return;
        }

        /*
         * Una cita todavía pendiente de pago no debe
         * generar un recordatorio clínico.
         */
        if (
                appointment.getStatus()
                        == AppointmentStatus
                        .PENDIENTE_DE_PAGO
        ) {
            LocalDateTime newAttempt =
                    LocalDateTime.now()
                            .plusMinutes(30);

            if (
                    newAttempt.isBefore(
                            appointment
                                    .getAppointmentDateTime()
                    )
            ) {
                reminder.setSendAt(
                        newAttempt
                );

                reminder.setUpdatedAt(
                        LocalDateTime.now()
                );

                appointmentReminderRepository.save(
                        reminder
                );

            } else {
                reminder.setStatus(
                        AppointmentReminderStatus
                                .CANCELADO
                );

                reminder.setUpdatedAt(
                        LocalDateTime.now()
                );

                appointmentReminderRepository.save(
                        reminder
                );
            }

            return;
        }

        reminder.setAttemptCount(
                reminder.getAttemptCount() + 1
        );

        try {
            emailService
                    .sendFollowUpAppointmentReminder(
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
                                    .getBranch()
                                    .getName(),
                            appointment
                                    .getAppointmentDateTime()
                    );

            reminder.setStatus(
                    AppointmentReminderStatus
                            .ENVIADO
            );

            reminder.setSentAt(
                    LocalDateTime.now()
            );

            reminder.setLastError(null);

            auditService.log(
                    "SYSTEM",
                    "SEND_FOLLOW_UP_REMINDER",
                    "APPOINTMENTS",
                    "Recordatorio enviado para cita ID "
                            + appointment.getId()
                            + "."
            );

        } catch (RuntimeException exception) {
            reminder.setStatus(
                    AppointmentReminderStatus.ERROR
            );

            reminder.setLastError(
                    limitErrorMessage(
                            exception.getMessage()
                    )
            );

            auditService.log(
                    "SYSTEM",
                    "FOLLOW_UP_REMINDER_ERROR",
                    "APPOINTMENTS",
                    "No fue posible enviar el recordatorio "
                            + "de la cita ID "
                            + appointment.getId()
                            + "."
            );
        }

        reminder.setUpdatedAt(
                LocalDateTime.now()
        );

        appointmentReminderRepository.save(
                reminder
        );
    }

    /**
     * Limita el mensaje almacenado para respetar
     * la longitud de la columna.
     */
    private String limitErrorMessage(
            String message
    ) {
        if (
                message == null ||
                        message.isBlank()
        ) {
            return "Error no especificado.";
        }

        return message.length() <= 1000
                ? message
                : message.substring(
                0,
                1000
        );
    }
}