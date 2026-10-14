import {
    useEffect,
    useMemo,
    useState
} from "react";

import {
    createMedicalAgendaEvent,
    updateMedicalAgendaEvent
} from "../../api/medicalAgendaApi";

const EVENT_TYPES = [
    {
        value: "REUNION",
        label: "Reunión"
    },
    {
        value: "DESCANSO",
        label: "Descanso"
    },
    {
        value: "CAPACITACION",
        label: "Capacitación"
    },
    {
        value: "PERSONAL",
        label: "Personal"
    },
    {
        value: "OTRO",
        label: "Otro"
    }
];

/**
 * Formulario reutilizable para crear o editar
 * eventos personales de la agenda médica.
 */
function MedicalAgendaEventForm({
    initialData,
    onSaved,
    onCancel
}) {
    const editing =
        Boolean(
            initialData?.eventId
        );

    const [
        title,
        setTitle
    ] = useState("");

    const [
        description,
        setDescription
    ] = useState("");

    const [
        eventType,
        setEventType
    ] = useState("PERSONAL");

    const [
        allDay,
        setAllDay
    ] = useState(false);

    const [
        startDate,
        setStartDate
    ] = useState("");

    const [
        startTime,
        setStartTime
    ] = useState("08:00");

    const [
        endDate,
        setEndDate
    ] = useState("");

    const [
        endTime,
        setEndTime
    ] = useState("09:00");

    const [
        errors,
        setErrors
    ] = useState({});

    const [
        saving,
        setSaving
    ] = useState(false);

    /**
     * Título de la ventana según el modo actual.
     */
    const formTitle =
        useMemo(() => {
            return editing
                ? "Editar Evento"
                : "Nuevo Evento";
        }, [editing]);

    /**
     * Cargar datos iniciales.
     *
     * initialData puede proceder de:
     *
     * - Una selección nueva de FullCalendar.
     * - Un evento recuperado del backend.
     */
    useEffect(() => {
        const initialStart =
            parseLocalDateTime(
                initialData?.startDateTime
            );

        const initialEnd =
            parseLocalDateTime(
                initialData?.endDateTime
            );

        const initialAllDay =
            Boolean(
                initialData?.allDay
            );

        setTitle(
            initialData?.title ||
            ""
        );

        setDescription(
            initialData?.description ||
            ""
        );

        setEventType(
            initialData?.eventType ||
            "PERSONAL"
        );

        setAllDay(
            initialAllDay
        );

        setStartDate(
            initialStart.date ||
            getTodayDate()
        );

        setStartTime(
            initialStart.time ||
            "08:00"
        );

        setEndDate(
            getDisplayEndDate(
                initialData,
                initialEnd.date ||
                initialStart.date ||
                getTodayDate()
            )
        );

        setEndTime(
            initialEnd.time ||
            "09:00"
        );

        setErrors({});
    }, [initialData]);

    /**
     * Al marcar todo el día mantenemos
     * únicamente las fechas.
     */
    const handleAllDayChange =
        event => {
            const checked =
                event.target.checked;

            setAllDay(
                checked
            );

            if (checked) {
                setStartTime(
                    "00:00"
                );

                setEndTime(
                    "00:00"
                );
            } else {
                setStartTime(
                    startTime === "00:00"
                        ? "08:00"
                        : startTime
                );

                setEndTime(
                    endTime === "00:00"
                        ? "09:00"
                        : endTime
                );
            }

            setErrors({});
        };

    /**
     * Validar el formulario.
     */
    const validateForm = () => {
        const newErrors = {};

        const cleanTitle =
            title.trim();

        const cleanDescription =
            description.trim();

        if (!cleanTitle) {
            newErrors.title =
                "El título del evento es obligatorio.";

        } else if (
            cleanTitle.length > 150
        ) {
            newErrors.title =
                "El título no puede exceder los 150 caracteres.";
        }

        if (
            cleanDescription.length > 500
        ) {
            newErrors.description =
                "La descripción no puede exceder los 500 caracteres.";
        }

        if (!eventType) {
            newErrors.eventType =
                "Debe seleccionar el tipo de evento.";
        }

        if (!startDate) {
            newErrors.startDate =
                "La fecha de inicio es obligatoria.";
        }

        if (!endDate) {
            newErrors.endDate =
                "La fecha de fin es obligatoria.";
        }

        if (
            !allDay &&
            !startTime
        ) {
            newErrors.startTime =
                "La hora de inicio es obligatoria.";
        }

        if (
            !allDay &&
            !endTime
        ) {
            newErrors.endTime =
                "La hora de fin es obligatoria.";
        }

        if (
            startDate &&
            endDate
        ) {
            const start =
                buildLocalDateTime(
                    startDate,
                    allDay
                        ? "00:00"
                        : startTime
                );

            const end =
                buildLocalDateTime(
                    endDate,
                    allDay
                        ? "00:00"
                        : endTime
                );

            /**
             * Para eventos de todo el día el backend
             * permite la misma fecha y convierte el fin
             * al inicio del día siguiente.
             */
            if (
                allDay &&
                endDate < startDate
            ) {
                newErrors.dateRange =
                    "La fecha de fin no puede ser anterior a la fecha de inicio.";
            }

            if (
                !allDay &&
                (
                    !start ||
                    !end ||
                    end <= start
                )
            ) {
                newErrors.dateRange =
                    "La fecha y hora de fin deben ser posteriores al inicio.";
            }
        }

        if (
            editing &&
            (
                initialData?.version ===
                null ||
                initialData?.version ===
                undefined
            )
        ) {
            newErrors.version =
                "No se pudo determinar la versión actual del evento.";
        }

        setErrors(
            newErrors
        );

        return (
            Object.keys(
                newErrors
            ).length === 0
        );
    };

    /**
     * Crear el payload esperado por Spring Boot.
     */
    const buildPayload = () => {
        return {
            title:
                title.trim(),
            description:
                description.trim(),
            eventType,
            allDay,
            startDateTime:
                `${startDate}T${allDay
                    ? "00:00:00"
                    : `${startTime}:00`
                }`,
            endDateTime:
                `${endDate}T${allDay
                    ? "00:00:00"
                    : `${endTime}:00`
                }`,
            version:
                initialData?.version
        };
    };

    /**
     * Guardar creación o actualización.
     */
    const handleSubmit =
        async event => {
            event.preventDefault();

            if (!validateForm()) {
                return;
            }

            try {
                setSaving(true);

                const payload =
                    buildPayload();

                const response =
                    editing
                        ? await updateMedicalAgendaEvent(
                            initialData.eventId,
                            payload
                        )
                        : await createMedicalAgendaEvent(
                            payload
                        );

                alert(
                    response.data
                        ?.message ||
                    (
                        editing
                            ? "Evento actualizado correctamente."
                            : "Evento creado correctamente."
                    )
                );

                if (onSaved) {
                    onSaved(
                        response.data
                    );
                }

            } catch (error) {
                console.error(
                    "Error al guardar evento:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible guardar el evento."
                );

            } finally {
                setSaving(false);
            }
        };

    return (
        <div
            className="medical-agenda-modal-backdrop"
            role="presentation"
        >
            <div
                className="medical-agenda-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="event-form-title"
            >
                <div className="medical-agenda-modal-header">
                    <div>
                        <h2
                            id="event-form-title"
                            className="section-title"
                        >
                            {formTitle}
                        </h2>

                        <p>
                            Los eventos personales bloquean la disponibilidad para nuevas citas.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="medical-agenda-close-button"
                        onClick={
                            onCancel
                        }
                        disabled={saving}
                        aria-label="Cerrar formulario"
                    >
                        ×
                    </button>
                </div>

                <form
                    onSubmit={
                        handleSubmit
                    }
                >
                    <div className="form-grid">
                        <div className="form-group">
                            <label>
                                Título
                            </label>

                            <input
                                type="text"
                                maxLength={150}
                                placeholder="Ejemplo: Reunión clínica"
                                value={title}
                                disabled={saving}
                                className={
                                    errors.title
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setTitle(
                                            event.target
                                                .value
                                        )
                                }
                            />

                            {errors.title && (
                                <div className="error-message">
                                    {errors.title}
                                </div>
                            )}

                            <small>
                                {title.length}/150
                            </small>
                        </div>

                        <div className="form-group">
                            <label>
                                Tipo
                            </label>

                            <select
                                value={
                                    eventType
                                }
                                disabled={saving}
                                className={
                                    errors.eventType
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setEventType(
                                            event.target
                                                .value
                                        )
                                }
                            >
                                {EVENT_TYPES.map(
                                    type => (
                                        <option
                                            key={
                                                type.value
                                            }
                                            value={
                                                type.value
                                            }
                                        >
                                            {
                                                type.label
                                            }
                                        </option>
                                    )
                                )}
                            </select>

                            {errors.eventType && (
                                <div className="error-message">
                                    {
                                        errors.eventType
                                    }
                                </div>
                            )}
                        </div>

                        <div className="form-group medical-agenda-checkbox-group">
                            <label>
                                <input
                                    type="checkbox"
                                    checked={allDay}
                                    disabled={saving}
                                    onChange={
                                        handleAllDayChange
                                    }
                                />

                                <span>
                                    Evento de todo el día
                                </span>
                            </label>
                        </div>

                        <div className="form-group">
                            <label>
                                Fecha de inicio
                            </label>

                            <input
                                type="date"
                                value={
                                    startDate
                                }
                                disabled={saving}
                                className={
                                    errors.startDate
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event => {
                                        const value =
                                            event.target
                                                .value;

                                        setStartDate(
                                            value
                                        );

                                        if (
                                            !endDate ||
                                            endDate < value
                                        ) {
                                            setEndDate(
                                                value
                                            );
                                        }

                                        setErrors({});
                                    }
                                }
                            />

                            {errors.startDate && (
                                <div className="error-message">
                                    {
                                        errors.startDate
                                    }
                                </div>
                            )}
                        </div>

                        {!allDay && (
                            <div className="form-group">
                                <label>
                                    Hora de inicio
                                </label>

                                <input
                                    type="time"
                                    step="1800"
                                    value={
                                        startTime
                                    }
                                    disabled={saving}
                                    className={
                                        errors.startTime
                                            ? "input-error"
                                            : ""
                                    }
                                    onChange={
                                        event => {
                                            setStartTime(
                                                event.target
                                                    .value
                                            );

                                            setErrors({});
                                        }
                                    }
                                />

                                {errors.startTime && (
                                    <div className="error-message">
                                        {
                                            errors.startTime
                                        }
                                    </div>
                                )}
                            </div>
                        )}

                        <div className="form-group">
                            <label>
                                Fecha de fin
                            </label>

                            <input
                                type="date"
                                min={
                                    startDate ||
                                    undefined
                                }
                                value={
                                    endDate
                                }
                                disabled={saving}
                                className={
                                    errors.endDate
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event => {
                                        setEndDate(
                                            event.target
                                                .value
                                        );

                                        setErrors({});
                                    }
                                }
                            />

                            {errors.endDate && (
                                <div className="error-message">
                                    {errors.endDate}
                                </div>
                            )}
                        </div>

                        {!allDay && (
                            <div className="form-group">
                                <label>
                                    Hora de fin
                                </label>

                                <input
                                    type="time"
                                    step="1800"
                                    value={
                                        endTime
                                    }
                                    disabled={saving}
                                    className={
                                        errors.endTime
                                            ? "input-error"
                                            : ""
                                    }
                                    onChange={
                                        event => {
                                            setEndTime(
                                                event.target
                                                    .value
                                            );

                                            setErrors({});
                                        }
                                    }
                                />

                                {errors.endTime && (
                                    <div className="error-message">
                                        {
                                            errors.endTime
                                        }
                                    </div>
                                )}
                            </div>
                        )}

                        <div className="form-group medical-agenda-full-width">
                            <label>
                                Descripción
                            </label>

                            <textarea
                                rows={5}
                                maxLength={500}
                                placeholder="Descripción opcional del evento"
                                value={
                                    description
                                }
                                disabled={saving}
                                className={
                                    errors.description
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setDescription(
                                            event.target
                                                .value
                                        )
                                }
                            />

                            {errors.description && (
                                <div className="error-message">
                                    {
                                        errors.description
                                    }
                                </div>
                            )}

                            <small>
                                {description.length}/500
                            </small>
                        </div>
                    </div>

                    {errors.dateRange && (
                        <div className="error-message">
                            {errors.dateRange}
                        </div>
                    )}

                    {errors.version && (
                        <div className="error-message">
                            {errors.version}
                        </div>
                    )}

                    <div className="button-row">
                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={saving}
                        >
                            {saving
                                ? "Guardando..."
                                : (
                                    editing
                                        ? "Actualizar evento"
                                        : "Crear evento"
                                )}
                        </button>

                        <button
                            type="button"
                            className="btn btn-secondary"
                            disabled={saving}
                            onClick={
                                onCancel
                            }
                        >
                            Cancelar
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/**
 * Convierte un LocalDateTime ISO del backend
 * en fecha y hora para inputs HTML.
 */
function parseLocalDateTime(
    value
) {
    if (!value) {
        return {
            date: "",
            time: ""
        };
    }

    const parts =
        String(value)
            .split("T");

    return {
        date:
            parts[0] || "",
        time:
            parts[1]
                ? parts[1]
                    .substring(
                        0,
                        5
                    )
                : ""
    };
}

/**
 * El backend guarda el fin de un evento de todo
 * el día de manera exclusiva.
 *
 * Para mostrar la fecha elegida por el usuario,
 * se resta un día al fin exclusivo.
 */
function getDisplayEndDate(
    initialData,
    fallbackDate
) {
    if (
        !initialData?.allDay ||
        !initialData?.endDateTime
    ) {
        return fallbackDate;
    }

    const endDate =
        parseDateOnly(
            initialData.endDateTime
        );

    if (!endDate) {
        return fallbackDate;
    }

    endDate.setDate(
        endDate.getDate() - 1
    );

    return formatDateInput(
        endDate
    );
}

function parseDateOnly(
    value
) {
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

function buildLocalDateTime(
    date,
    time
) {
    if (
        !date ||
        !time
    ) {
        return null;
    }

    const parsedDate =
        new Date(
            `${date}T${time}:00`
        );

    return Number.isNaN(
        parsedDate.getTime()
    )
        ? null
        : parsedDate;
}

function getTodayDate() {
    return formatDateInput(
        new Date()
    );
}

function formatDateInput(
    date
) {
    const year =
        date.getFullYear();

    const month =
        String(
            date.getMonth() + 1
        ).padStart(
            2,
            "0"
        );

    const day =
        String(
            date.getDate()
        ).padStart(
            2,
            "0"
        );

    return `${year}-${month}-${day}`;
}

export default MedicalAgendaEventForm;