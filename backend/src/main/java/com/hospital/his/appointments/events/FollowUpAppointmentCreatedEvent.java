package com.hospital.his.appointments.events;

/**
 * Evento publicado después de guardar una cita
 * de seguimiento.
 *
 * El evento contiene únicamente el ID de la cita.
 * El listener volverá a consultar PostgreSQL después
 * de que la transacción haya sido confirmada.
 */
public record FollowUpAppointmentCreatedEvent(
        Long appointmentId
) {
}