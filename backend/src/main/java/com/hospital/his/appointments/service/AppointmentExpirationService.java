package com.hospital.his.appointments.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentExpirationService {

    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;

    public AppointmentExpirationService(
            AppointmentRepository appointmentRepository,
            AuditService auditService
    ) {
        this.appointmentRepository =
                appointmentRepository;

        this.auditService =
                auditService;
    }

    /*
     * Se ejecuta periódicamente y actualiza todas
     * las reservas pendientes cuyo plazo terminó.
     * 30 segundos
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void expireOverdueReservations() {

        LocalDateTime now =
                LocalDateTime.now();

        List<Appointment> expiredAppointments =
                appointmentRepository
                        .findByStatusAndReservationExpiresAtLessThanEqual(
                                AppointmentStatus.PENDIENTE_DE_PAGO,
                                now
                        );

        for (Appointment appointment
                : expiredAppointments) {

            expireAppointment(appointment);
        }
    }

    /*
     * Valida una cita justo antes de iniciar
     * o procesar el pago.
     *
     * También comprueba que la cita realmente
     * pertenezca al usuario autenticado.
     */
    @Transactional
    public Appointment validateAppointmentBeforePayment(Long appointmentId, String authenticatedUsername) {
        if (appointmentId == null) {
            throw new RuntimeException(
                    "Debe indicar la cita que desea pagar."
            );
        }

        if (authenticatedUsername == null
                || authenticatedUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al paciente autenticado."
            );
        }

        Appointment appointment =
                appointmentRepository
                        .findByIdAndPatient_Username(
                                appointmentId,
                                authenticatedUsername
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "La cita no fue encontrada o no pertenece al paciente autenticado."
                                )
                        );

        if (appointment.getStatus()
                == AppointmentStatus.EXPIRADA) {

            throw new RuntimeException(
                    "El tiempo para confirmar su cita ha expirado. "
                            + "El horario seleccionado ha sido liberado. "
                            + "Por favor, seleccione un nuevo horario."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.CANCELADA) {

            throw new RuntimeException(
                    "La cita se encuentra cancelada y no puede ser pagada."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.PAGADA
                || appointment.getStatus()
                == AppointmentStatus.CONFIRMADA) {

            throw new RuntimeException(
                    "La cita ya fue pagada."
            );
        }

        if (appointment.getStatus()
                != AppointmentStatus.PENDIENTE_DE_PAGO) {

            throw new RuntimeException(
                    "La cita no se encuentra disponible para pago."
            );
        }

        if (!Boolean.TRUE.equals(
                appointment.getActive())) {

            throw new RuntimeException(
                    "La cita no se encuentra activa."
            );
        }

        LocalDateTime expiration =
                appointment.getReservationExpiresAt();

        if (expiration == null
                || !expiration.isAfter(
                LocalDateTime.now())) {

            expireAppointment(appointment);

            throw new RuntimeException(
                    "El tiempo para confirmar su cita ha expirado. "
                            + "El horario seleccionado ha sido liberado. "
                            + "Por favor, seleccione un nuevo horario."
            );
        }

        return appointment;
    }

    /*
     * Marca formalmente una reserva como expirada
     * y libera su horario.
     */
    private void expireAppointment(Appointment appointment) {
        appointment.setStatus(
                AppointmentStatus.EXPIRADA
        );

        appointment.setActive(false);

        appointmentRepository.save(appointment);

        String username =
                appointment.getPatient() != null
                        ? appointment
                        .getPatient()
                        .getUsername()
                        : "SYSTEM";

        auditService.log(
                username,
                "EXPIRE_APPOINTMENT",
                "APPOINTMENTS",
                "La reserva de la cita ID "
                        + appointment.getId()
                        + " expiró y el horario fue liberado."
        );
    }
}