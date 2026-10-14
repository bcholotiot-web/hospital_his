import {
    useState
} from "react";

import {
    changeMedicalAgendaEventStatus
} from "../../api/medicalAgendaApi";

import AppointmentStatusBadge
    from "../../components/AppointmentStatusBadge";

/**
 * Muestra el detalle del elemento seleccionado
 * dentro del calendario.
 */
function MedicalAgendaItemDetail({
    item,
    canUpdateEvent,
    canDeleteEvent,
    onEditEvent,
    onDeleted,
    onClose
}) {
    const [
        deleting,
        setDeleting
    ] = useState(false);

    if (!item) {
        return null;
    }

    const itemType =
        item.calendarItemType;

    const isAppointment =
        itemType ===
        "APPOINTMENT";

    const isPersonalEvent =
        itemType ===
        "PERSONAL_EVENT";

    /**
     * Elimina lógicamente un evento personal.
     */
    const handleDelete =
        async () => {
            if (
                !isPersonalEvent ||
                !canDeleteEvent
            ) {
                return;
            }

            const confirmed =
                window.confirm(
                    `¿Desea eliminar el evento "${item.title}"?\n\n`
                    + "El evento dejará de aparecer y el horario volverá a estar disponible."
                );

            if (!confirmed) {
                return;
            }

            try {
                setDeleting(true);

                const response =
                    await changeMedicalAgendaEventStatus(
                        item.eventId,
                        false,
                        item.version
                    );

                alert(
                    response.data
                        ?.message ||
                    "Evento eliminado correctamente."
                );

                if (onDeleted) {
                    onDeleted(
                        response.data
                    );
                }

            } catch (error) {
                console.error(
                    "Error al eliminar evento:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible eliminar el evento."
                );

            } finally {
                setDeleting(false);
            }
        };

    return (
        <div
            className="medical-agenda-modal-backdrop"
            role="presentation"
        >
            <div
                className="medical-agenda-modal medical-agenda-detail-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="agenda-detail-title"
            >
                <div className="medical-agenda-modal-header">
                    <div>
                        <h2
                            id="agenda-detail-title"
                            className="section-title"
                        >
                            {isAppointment
                                ? "Detalle de la Cita"
                                : "Detalle del Evento"}
                        </h2>

                        <p>
                            {isAppointment
                                ? "Información clínica y operativa de la cita."
                                : "Evento personal registrado en la agenda."}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="medical-agenda-close-button"
                        onClick={
                            onClose
                        }
                        disabled={deleting}
                        aria-label="Cerrar detalle"
                    >
                        ×
                    </button>
                </div>

                {isAppointment && (
                    <AppointmentDetail
                        appointment={
                            item
                        }
                    />
                )}

                {isPersonalEvent && (
                    <PersonalEventDetail
                        event={item}
                    />
                )}

                <div className="button-row">
                    {isPersonalEvent &&
                        canUpdateEvent && (
                            <button
                                type="button"
                                className="btn btn-primary"
                                disabled={deleting}
                                onClick={() =>
                                    onEditEvent(
                                        item
                                    )
                                }
                            >
                                Editar evento
                            </button>
                        )}

                    {isPersonalEvent &&
                        canDeleteEvent && (
                            <button
                                type="button"
                                className="btn btn-danger-outline"
                                disabled={deleting}
                                onClick={
                                    handleDelete
                                }
                            >
                                {deleting
                                    ? "Eliminando..."
                                    : "Eliminar evento"}
                            </button>
                        )}

                    <button
                        type="button"
                        className="btn btn-secondary"
                        disabled={deleting}
                        onClick={
                            onClose
                        }
                    >
                        Cerrar
                    </button>
                </div>
            </div>
        </div>
    );
}

function AppointmentDetail({
    appointment
}) {
    return (
        <div className="medical-agenda-detail-content">
            <div className="medical-agenda-detail-status">
                <AppointmentStatusBadge
                    status={
                        appointment.status
                    }
                    presentation={
                        appointment
                            .statusPresentation
                    }
                    showDescription
                />
            </div>

            <div className="medical-agenda-detail-grid">
                <DetailValue
                    label="Paciente"
                    value={
                        appointment.patientName
                    }
                />

                <DetailValue
                    label="Especialidad"
                    value={
                        appointment.specialtyName
                    }
                />

                <DetailValue
                    label="Sucursal"
                    value={
                        appointment.branchName
                    }
                />

                <DetailValue
                    label="Inicio"
                    value={
                        formatDateTime(
                            appointment.startDateTime
                        )
                    }
                />

                <DetailValue
                    label="Fin"
                    value={
                        formatDateTime(
                            appointment.endDateTime
                        )
                    }
                />

                <DetailValue
                    label="Prioridad"
                    value={
                        formatTechnicalValue(
                            appointment.priority
                        )
                    }
                />

                <DetailValue
                    label="Tipo"
                    value={
                        appointment.followUp
                            ? "Seguimiento"
                            : "Consulta regular"
                    }
                />

                {appointment.followUp && (
                    <DetailValue
                        label="Tipo de seguimiento"
                        value={
                            formatTechnicalValue(
                                appointment
                                    .followUpType
                            )
                        }
                    />
                )}
            </div>

            <DetailValue
                label="Motivo"
                value={
                    appointment.reason
                }
                fullWidth
            />
        </div>
    );
}

function PersonalEventDetail({
    event
}) {
    return (
        <div className="medical-agenda-detail-content">
            <div className="medical-agenda-event-chip">
                <span
                    aria-hidden="true"
                    style={{
                        backgroundColor:
                            event.color ||
                            "#8B5CF6"
                    }}
                />

                {event.eventTypeLabel ||
                    formatTechnicalValue(
                        event.eventType
                    )}
            </div>

            <div className="medical-agenda-detail-grid">
                <DetailValue
                    label="Título"
                    value={event.title}
                />

                <DetailValue
                    label="Tipo"
                    value={
                        event.eventTypeLabel ||
                        formatTechnicalValue(
                            event.eventType
                        )
                    }
                />

                <DetailValue
                    label="Inicio"
                    value={
                        event.allDay
                            ? formatDate(
                                event
                                    .startDateTime
                            )
                            : formatDateTime(
                                event
                                    .startDateTime
                            )
                    }
                />

                <DetailValue
                    label="Fin"
                    value={
                        formatEventEnd(
                            event
                        )
                    }
                />

                <DetailValue
                    label="Modalidad"
                    value={
                        event.allDay
                            ? "Todo el día"
                            : "Con horario"
                    }
                />

                <DetailValue
                    label="Versión"
                    value={
                        event.version
                    }
                />
            </div>

            <DetailValue
                label="Descripción"
                value={
                    event.description ||
                    "Sin descripción"
                }
                fullWidth
            />
        </div>
    );
}

function DetailValue({
    label,
    value,
    fullWidth = false
}) {
    return (
        <div
            className={
                fullWidth
                    ? "medical-agenda-detail-value medical-agenda-detail-full"
                    : "medical-agenda-detail-value"
            }
        >
            <span>
                {label}
            </span>

            <strong>
                {value === null ||
                    value === undefined ||
                    value === ""
                    ? "Sin información"
                    : value}
            </strong>
        </div>
    );
}

function formatEventEnd(
    event
) {
    if (!event?.endDateTime) {
        return "Sin información";
    }

    if (!event.allDay) {
        return formatDateTime(
            event.endDateTime
        );
    }

    const endDate =
        parseLocalDate(
            event.endDateTime
        );

    if (!endDate) {
        return formatDate(
            event.endDateTime
        );
    }

    endDate.setDate(
        endDate.getDate() - 1
    );

    return endDate
        .toLocaleDateString(
            "es-GT",
            {
                day: "2-digit",
                month: "2-digit",
                year: "numeric"
            }
        );
}

function formatDateTime(
    value
) {
    if (!value) {
        return "Sin información";
    }

    const date =
        new Date(value);

    if (
        Number.isNaN(
            date.getTime()
        )
    ) {
        return value;
    }

    return date.toLocaleString(
        "es-GT",
        {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}

function formatDate(
    value
) {
    const date =
        parseLocalDate(
            value
        );

    if (!date) {
        return value ||
            "Sin información";
    }

    return date.toLocaleDateString(
        "es-GT",
        {
            day: "2-digit",
            month: "2-digit",
            year: "numeric"
        }
    );
}

function parseLocalDate(
    value
) {
    if (!value) {
        return null;
    }

    const datePart =
        String(value)
            .split("T")[0];

    const [
        year,
        month,
        day
    ] = datePart
        .split("-")
        .map(Number);

    if (
        !year ||
        !month ||
        !day
    ) {
        return null;
    }

    return new Date(
        year,
        month - 1,
        day
    );
}

function formatTechnicalValue(
    value
) {
    if (!value) {
        return "Sin información";
    }

    const text =
        String(value)
            .toLowerCase()
            .replace(
                /_/g,
                " "
            );

    return (
        text.charAt(0)
            .toUpperCase() +
        text.slice(1)
    );
}

export default MedicalAgendaItemDetail;