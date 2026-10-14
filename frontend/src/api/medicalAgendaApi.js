import axios from "axios";

const AGENDA_API_URL =
    "http://localhost:8080/api/doctor/agenda";

const EVENT_API_URL =
    `${AGENDA_API_URL}/events`;

/**
 * Construye la configuración de autorización
 * utilizando el JWT almacenado después del login.
 */
const getAuthConfig = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization:
                `Bearer ${token}`
        }
    };
};

/**
 * Obtiene el calendario unificado del médico.
 *
 * Incluye:
 *
 * - Citas programadas.
 * - Eventos personales activos.
 *
 * GET /api/doctor/agenda
 *     ?start=2026-10-01T00:00:00
 *     &end=2026-11-01T00:00:00
 */
export const getMedicalAgendaCalendar = ({
    start,
    end
}) => {
    return axios.get(
        AGENDA_API_URL,
        {
            ...getAuthConfig(),
            params: {
                start,
                end
            }
        }
    );
};

/**
 * Obtiene los eventos personales activos
 * dentro de un rango.
 *
 * Normalmente la pantalla principal utilizará
 * getMedicalAgendaCalendar().
 *
 * Este método queda disponible para recargas
 * específicas del formulario de eventos.
 */
export const getMedicalAgendaEvents = ({
    start,
    end
}) => {
    return axios.get(
        EVENT_API_URL,
        {
            ...getAuthConfig(),
            params: {
                start,
                end
            }
        }
    );
};

/**
 * Obtiene el detalle de un evento propio.
 */
export const getMedicalAgendaEventById =
    eventId => {
        return axios.get(
            `${EVENT_API_URL}/${eventId}`,
            getAuthConfig()
        );
    };

/**
 * Crea un evento personal.
 */
export const createMedicalAgendaEvent =
    data => {
        return axios.post(
            EVENT_API_URL,
            {
                title:
                    data.title
                        ?.trim(),
                description:
                    data.description
                        ?.trim() ||
                    null,
                startDateTime:
                    data.startDateTime,
                endDateTime:
                    data.endDateTime,
                eventType:
                    data.eventType,
                allDay:
                    Boolean(
                        data.allDay
                    )
            },
            getAuthConfig()
        );
    };

/**
 * Actualiza un evento personal.
 *
 * version es obligatoria.
 */
export const updateMedicalAgendaEvent = (
    eventId,
    data
) => {
    return axios.put(
        `${EVENT_API_URL}/${eventId}`,
        {
            title:
                data.title
                    ?.trim(),
            description:
                data.description
                    ?.trim() ||
                null,
            startDateTime:
                data.startDateTime,
            endDateTime:
                data.endDateTime,
            eventType:
                data.eventType,
            allDay:
                Boolean(
                    data.allDay
                ),
            version:
                Number(
                    data.version
                )
        },
        getAuthConfig()
    );
};

/**
 * Activa o desactiva lógicamente un evento.
 *
 * En la interfaz utilizaremos active=false como
 * eliminación lógica.
 */
export const changeMedicalAgendaEventStatus = (
    eventId,
    active,
    version
) => {
    return axios.patch(
        `${EVENT_API_URL}/${eventId}/status`,
        null,
        {
            ...getAuthConfig(),
            params: {
                active,
                version
            }
        }
    );
};