import {
    useCallback,
    useMemo,
    useRef,
    useState
} from "react";

import FullCalendar
    from "@fullcalendar/react";

import dayGridPlugin
    from "@fullcalendar/daygrid";

import timeGridPlugin
    from "@fullcalendar/timegrid";

import interactionPlugin
    from "@fullcalendar/interaction";

import MainLayout
    from "../../layouts/MainLayout";

import {
    getMedicalAgendaCalendar
} from "../../api/medicalAgendaApi";

import {
    useAuth
} from "../../context/AuthContext";

import MedicalAgendaEventForm
    from "./MedicalAgendaEventForm";

import MedicalAgendaItemDetail
    from "./MedicalAgendaItemDetail";

import TaskPanel
    from "./TaskPanel";

import "./MedicalAgenda.css";

function MedicalAgenda() {
    const calendarRef =
        useRef(null);

    const {
        hasPermission
    } = useAuth();

    /**
     * Permisos del módulo.
     */
    const canRead =
        hasPermission(
            "MEDICAL_AGENDA_READ"
        );

    const canCreateEvent =
        hasPermission(
            "MEDICAL_AGENDA_EVENT_CREATE"
        );

    const canUpdateEvent =
        hasPermission(
            "MEDICAL_AGENDA_EVENT_UPDATE"
        );

    const canDeleteEvent =
        hasPermission(
            "MEDICAL_AGENDA_EVENT_DELETE"
        );

    const canCreateTask =
        hasPermission(
            "MEDICAL_AGENDA_TASK_CREATE"
        );

    const canUpdateTask =
        hasPermission(
            "MEDICAL_AGENDA_TASK_UPDATE"
        );

    const canDeleteTask =
        hasPermission(
            "MEDICAL_AGENDA_TASK_DELETE"
        );

    /**
     * Información del calendario.
     */
    const [
        calendarEvents,
        setCalendarEvents
    ] = useState([]);

    const [
        calendarSummary,
        setCalendarSummary
    ] = useState({
        appointmentCount: 0,
        eventCount: 0
    });

    const [
        loading,
        setLoading
    ] = useState(false);

    const [
        errorMessage,
        setErrorMessage
    ] = useState("");

    /**
     * Rango visible actual.
     */
    const [
        visibleRange,
        setVisibleRange
    ] = useState(null);

    /**
     * Formulario de creación o edición.
     */
    const [
        eventFormData,
        setEventFormData
    ] = useState(null);

    /**
     * Elemento seleccionado.
     */
    const [
        selectedItem,
        setSelectedItem
    ] = useState(null);

    /**
 * Día utilizado por el panel de tareas.
 *
 * Se inicializa con la fecha local actual.
 */
    const [
        selectedTaskDate,
        setSelectedTaskDate
    ] = useState(
        formatDateInput(
            new Date()
        )
    );

    /**
     * Leyenda visual.
     */
    const legendItems =
        useMemo(() => {
            return [
                {
                    label:
                        "Evento personal",
                    color:
                        "#8B5CF6"
                },
                {
                    label:
                        "Citas",
                    color:
                        "#64748B",
                    note:
                        "El color depende del estado"
                }
            ];
        }, []);

    /**
     * Carga citas y eventos para el rango visible.
     */
    const loadCalendarRange =
        useCallback(async (
            rangeStart,
            rangeEnd
        ) => {
            if (
                !canRead ||
                !rangeStart ||
                !rangeEnd
            ) {
                return;
            }

            try {
                setLoading(true);
                setErrorMessage("");

                const response =
                    await getMedicalAgendaCalendar({
                        start:
                            toBackendDateTime(
                                rangeStart
                            ),
                        end:
                            toBackendDateTime(
                                rangeEnd
                            )
                    });

                setCalendarEvents(
                    transformCalendarResponse(
                        response.data
                    )
                );

                setCalendarSummary({
                    appointmentCount:
                        response.data
                            ?.appointmentCount ??
                        0,
                    eventCount:
                        response.data
                            ?.eventCount ??
                        0
                });

            } catch (error) {
                console.error(
                    "Error al cargar agenda médica:",
                    error
                );

                setCalendarEvents([]);

                setCalendarSummary({
                    appointmentCount: 0,
                    eventCount: 0
                });

                setErrorMessage(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar la agenda médica."
                );

            } finally {
                setLoading(false);
            }
        }, [
            canRead
        ]);

    /**
     * FullCalendar ejecuta datesSet al iniciar
     * y cada vez que cambia el rango visible.
     */
    const handleDatesSet =
        dateInfo => {
            const range = {
                start:
                    dateInfo.start,
                end:
                    dateInfo.end
            };

            setVisibleRange(
                range
            );

            loadCalendarRange(
                range.start,
                range.end
            );
        };

    /**
     * Recarga el rango visible actual.
     */
    const refreshCalendar =
        useCallback(() => {
            if (!visibleRange) {
                return;
            }

            loadCalendarRange(
                visibleRange.start,
                visibleRange.end
            );
        }, [
            visibleRange,
            loadCalendarRange
        ]);

    /**
* Seleccionar un día para consultar
* las tareas correspondientes.
*/
    const handleDateClick =
        dateInfo => {
            setSelectedTaskDate(
                formatDateInput(
                    dateInfo.date
                )
            );
        };

    /**
     * Selección de rango desde el calendario.
     *
     * Se utiliza para crear un evento nuevo.
     */
    const handleSelect =
        selectionInfo => {
            if (!canCreateEvent) {
                selectionInfo
                    .view
                    .calendar
                    .unselect();

                return;
            }

            const allDay =
                Boolean(
                    selectionInfo.allDay
                );

            setSelectedItem(null);

            setEventFormData({
                startDateTime:
                    formatSelectionStart(
                        selectionInfo.start,
                        allDay
                    ),
                endDateTime:
                    formatSelectionEnd(
                        selectionInfo.end,
                        allDay
                    ),
                allDay,
                eventType:
                    "PERSONAL"
            });

            selectionInfo
                .view
                .calendar
                .unselect();
        };

    /**
     * Crear un evento desde el botón.
     */
    const handleNewEvent = () => {
        if (!canCreateEvent) {
            return;
        }

        const start =
            roundDateToNextHalfHour(
                new Date()
            );

        const end =
            new Date(
                start.getTime() +
                60 * 60 * 1000
            );

        setSelectedItem(null);

        setEventFormData({
            startDateTime:
                toBackendDateTime(
                    start
                ),
            endDateTime:
                toBackendDateTime(
                    end
                ),
            allDay: false,
            eventType:
                "PERSONAL"
        });
    };

    /**
     * Mostrar detalle de cita o evento.
     */
    const handleEventClick =
        clickInfo => {
            const rawData =
                clickInfo
                    .event
                    .extendedProps
                    ?.rawData;

            if (!rawData) {
                return;
            }

            setEventFormData(null);

            setSelectedItem(
                rawData
            );
        };

    /**
     * Abrir el formulario de edición.
     */
    const handleEditEvent =
        event => {
            setSelectedItem(null);

            setEventFormData(
                event
            );
        };

    /**
     * Cerrar ventanas.
     */
    const closeEventForm = () => {
        setEventFormData(null);
    };

    const closeItemDetail = () => {
        setSelectedItem(null);
    };

    /**
     * Después de guardar:
     *
     * - Cerrar formulario.
     * - Recargar el calendario.
     */
    const handleEventSaved = () => {
        setEventFormData(null);
        setSelectedItem(null);
        refreshCalendar();
    };

    /**
     * Después de eliminar:
     *
     * - Cerrar detalle.
     * - Recargar.
     */
    const handleEventDeleted = () => {
        setSelectedItem(null);
        refreshCalendar();
    };

    if (!canRead) {
        return (
            <MainLayout>
                <h1 className="page-title">
                    Agenda Médica
                </h1>

                <div className="card">
                    <p>
                        No tiene permiso para consultar la agenda médica.
                    </p>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="medical-agenda-page">
                <div className="medical-agenda-header">
                    <div>
                        <h1 className="page-title">
                            Agenda Médica
                        </h1>

                        <p>
                            Consulte sus citas y administre sus eventos personales.
                        </p>
                    </div>

                    <div className="button-row">
                        {canCreateEvent && (
                            <button
                                type="button"
                                className="btn btn-primary"
                                onClick={
                                    handleNewEvent
                                }
                            >
                                Nuevo evento
                            </button>
                        )}

                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={
                                refreshCalendar
                            }
                            disabled={
                                loading ||
                                !visibleRange
                            }
                        >
                            {loading
                                ? "Actualizando..."
                                : "Actualizar agenda"}
                        </button>
                    </div>
                </div>

                <div className="medical-agenda-summary-grid">
                    <SummaryValue
                        label="Citas visibles"
                        value={
                            calendarSummary
                                .appointmentCount
                        }
                        color="#2563EB"
                    />

                    <SummaryValue
                        label="Eventos personales"
                        value={
                            calendarSummary
                                .eventCount
                        }
                        color="#8B5CF6"
                    />
                </div>

                <div className="medical-agenda-legend">
                    {legendItems.map(
                        item => (
                            <div
                                key={
                                    item.label
                                }
                                className="medical-agenda-legend-item"
                            >
                                <span
                                    style={{
                                        backgroundColor:
                                            item.color
                                    }}
                                />

                                <strong>
                                    {item.label}
                                </strong>

                                {item.note && (
                                    <small>
                                        {item.note}
                                    </small>
                                )}
                            </div>
                        )
                    )}
                </div>

                {errorMessage && (
                    <div className="medical-agenda-error">
                        {errorMessage}
                    </div>
                )}

                <div className="medical-agenda-workspace">
                    <div className="medical-agenda-calendar-card">
                        <FullCalendar
                            ref={
                                calendarRef
                            }
                            plugins={[
                                dayGridPlugin,
                                timeGridPlugin,
                                interactionPlugin
                            ]}
                            initialView="dayGridMonth"
                            locale="es"
                            firstDay={1}
                            height="auto"
                            nowIndicator
                            selectable={
                                canCreateEvent
                            }
                            selectMirror
                            select={
                                handleSelect
                            }
                            dateClick={
                                handleDateClick
                            }
                            eventClick={
                                handleEventClick
                            }
                            events={
                                calendarEvents
                            }
                            datesSet={
                                handleDatesSet
                            }
                            headerToolbar={{
                                left:
                                    "prev,next today",
                                center:
                                    "title",
                                right:
                                    "dayGridMonth,timeGridWeek,timeGridDay"
                            }}
                            buttonText={{
                                today:
                                    "Hoy",
                                month:
                                    "Mes",
                                week:
                                    "Semana",
                                day:
                                    "Día"
                            }}
                            dayMaxEvents={4}
                            slotMinTime="07:00:00"
                            slotMaxTime="18:00:00"
                            slotDuration="00:30:00"
                            snapDuration="00:30:00"
                            allDayText="Todo el día"
                            noEventsText="No hay eventos"
                            eventTimeFormat={{
                                hour:
                                    "2-digit",
                                minute:
                                    "2-digit",
                                hour12:
                                    false
                            }}
                            slotLabelFormat={{
                                hour:
                                    "2-digit",
                                minute:
                                    "2-digit",
                                hour12:
                                    false
                            }}
                        />
                    </div>

                    <TaskPanel
                        selectedDate={
                            selectedTaskDate
                        }
                        canCreate={
                            canCreateTask
                        }
                        canUpdate={
                            canUpdateTask
                        }
                        canDelete={
                            canDeleteTask
                        }
                    />
                </div>
            </div>

            {eventFormData && (
                <MedicalAgendaEventForm
                    initialData={
                        eventFormData
                    }
                    onSaved={
                        handleEventSaved
                    }
                    onCancel={
                        closeEventForm
                    }
                />
            )}

            {selectedItem && (
                <MedicalAgendaItemDetail
                    item={
                        selectedItem
                    }
                    canUpdateEvent={
                        canUpdateEvent
                    }
                    canDeleteEvent={
                        canDeleteEvent
                    }
                    onEditEvent={
                        handleEditEvent
                    }
                    onDeleted={
                        handleEventDeleted
                    }
                    onClose={
                        closeItemDetail
                    }
                />
            )}
        </MainLayout>
    );
}

function SummaryValue({
    label,
    value,
    color
}) {
    return (
        <div
            className="medical-agenda-summary-value"
            style={{
                borderLeftColor:
                    color
            }}
        >
            <span>
                {label}
            </span>

            <strong
                style={{
                    color
                }}
            >
                {value}
            </strong>
        </div>
    );
}

/**
 * Transforma las listas del backend
 * en eventos para FullCalendar.
 */
function transformCalendarResponse(
    response
) {
    const appointments =
        Array.isArray(
            response?.appointments
        )
            ? response.appointments
            : [];

    const personalEvents =
        Array.isArray(
            response?.events
        )
            ? response.events
            : [];

    const appointmentItems =
        appointments.map(
            appointment => ({
                id:
                    `appointment-${appointment.appointmentId}`,
                title:
                    appointment.title ||
                    appointment.patientName ||
                    "Cita médica",
                start:
                    appointment.startDateTime,
                end:
                    appointment.endDateTime,
                allDay: false,
                backgroundColor:
                    safeColor(
                        appointment.color,
                        "#64748B"
                    ),
                borderColor:
                    safeColor(
                        appointment.color,
                        "#64748B"
                    ),
                extendedProps: {
                    calendarItemType:
                        "APPOINTMENT",
                    rawData: {
                        ...appointment,
                        calendarItemType:
                            "APPOINTMENT"
                    }
                }
            })
        );

    const eventItems =
        personalEvents.map(
            event => ({
                id:
                    `event-${event.eventId}`,
                title:
                    event.title ||
                    "Evento personal",
                start:
                    event.startDateTime,
                end:
                    event.endDateTime,
                allDay:
                    Boolean(
                        event.allDay
                    ),
                backgroundColor:
                    safeColor(
                        event.color,
                        "#8B5CF6"
                    ),
                borderColor:
                    safeColor(
                        event.color,
                        "#8B5CF6"
                    ),
                extendedProps: {
                    calendarItemType:
                        "PERSONAL_EVENT",
                    rawData: {
                        ...event,
                        calendarItemType:
                            "PERSONAL_EVENT"
                    }
                }
            })
        );

    return [
        ...appointmentItems,
        ...eventItems
    ];
}

/**
 * Convierte Date local a LocalDateTime para Spring.
 */
function toBackendDateTime(
    value
) {
    const date =
        value instanceof Date
            ? value
            : new Date(value);

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

    const hours =
        String(
            date.getHours()
        ).padStart(
            2,
            "0"
        );

    const minutes =
        String(
            date.getMinutes()
        ).padStart(
            2,
            "0"
        );

    const seconds =
        String(
            date.getSeconds()
        ).padStart(
            2,
            "0"
        );

    return (
        `${year}-${month}-${day}`
        + `T${hours}:${minutes}:${seconds}`
    );
}

function formatSelectionStart(
    date,
    allDay
) {
    if (!allDay) {
        return toBackendDateTime(
            date
        );
    }

    return (
        `${formatDateInput(date)}`
        + "T00:00:00"
    );
}

/**
 * FullCalendar utiliza fin exclusivo para una
 * selección de todo el día.
 *
 * El backend de creación espera la última fecha
 * incluida y después suma un día.
 */
function formatSelectionEnd(
    date,
    allDay
) {
    if (!allDay) {
        return toBackendDateTime(
            date
        );
    }

    const inclusiveEnd =
        new Date(
            date.getTime()
        );

    inclusiveEnd.setDate(
        inclusiveEnd.getDate() - 1
    );

    return (
        `${formatDateInput(inclusiveEnd)}`
        + "T00:00:00"
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

function roundDateToNextHalfHour(
    date
) {
    const rounded =
        new Date(
            date.getTime()
        );

    rounded.setSeconds(
        0,
        0
    );

    const minutes =
        rounded.getMinutes();

    if (minutes === 0) {
        rounded.setMinutes(
            30
        );

    } else if (minutes <= 30) {
        rounded.setMinutes(
            30
        );

    } else {
        rounded.setHours(
            rounded.getHours() + 1
        );

        rounded.setMinutes(
            0
        );
    }

    return rounded;
}

function safeColor(
    value,
    fallback
) {
    return /^#[0-9A-Fa-f]{6}$/
        .test(value || "")
        ? value
        : fallback;
}

export default MedicalAgenda;