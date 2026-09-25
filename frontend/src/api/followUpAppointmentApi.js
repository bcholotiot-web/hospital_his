import axios from "axios";

/*
 * Ruta base exclusiva para citas de seguimiento
 * creadas por el médico.
 */
const API_URL =
    "http://localhost:8080/api/doctor/follow-up-appointments";

/**
 * Construye los encabezados con el JWT del usuario.
 *
 * Spring Boot utiliza el token para identificar
 * al médico. React no envía el username.
 */
const getAuthConfig = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

/**
 * Obtiene el contexto de una consulta médica.
 *
 * El backend devuelve:
 *
 * - Paciente.
 * - Médico.
 * - Especialidad.
 * - Sucursal.
 * - Diagnóstico.
 * - Plan de tratamiento.
 * - Estado y capacidad de agendar.
 */
export const getFollowUpContext = (
    consultationId
) => {
    return axios.get(
        `${API_URL}/context/${consultationId}`,
        getAuthConfig()
    );
};

/**
 * Obtiene los horarios disponibles del mismo médico
 * que atendió la consulta original.
 *
 * date debe enviarse en formato:
 * YYYY-MM-DD
 */
export const getFollowUpAvailability = (
    consultationId,
    date
) => {
    return axios.get(
        `${API_URL}/availability`,
        {
            ...getAuthConfig(),

            params: {
                consultationId,
                date
            }
        }
    );
};

/**
 * Crea la cita de seguimiento.
 *
 * El frontend solo envía:
 *
 * - Consulta padre.
 * - Tipo de seguimiento.
 * - Fecha y hora.
 * - Motivo.
 * - Prioridad.
 * - UUID de idempotencia.
 *
 * El backend decide paciente, médico, especialidad,
 * sucursal, origen y estado inicial.
 */
export const createFollowUpAppointment = (
    data
) => {
    return axios.post(
        API_URL,
        data,
        getAuthConfig()
    );
};

/**
 * Lista las citas de seguimiento creadas
 * desde una consulta concreta.
 */
export const getFollowUpsByConsultation = (
    consultationId
) => {
    return axios.get(
        `${API_URL}/consultation/${consultationId}`,
        getAuthConfig()
    );
};