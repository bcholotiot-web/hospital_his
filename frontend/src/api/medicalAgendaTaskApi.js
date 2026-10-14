import axios from "axios";

const TASK_API_URL =
    "http://localhost:8080/api/doctor/agenda/tasks";

/**
 * Construye los encabezados con el JWT.
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
 * Obtiene las tareas activas del médico
 * correspondientes a una fecha.
 *
 * status:
 *
 * PENDING
 * COMPLETED
 * ALL
 */
export const getMedicalAgendaTasks = ({
    date,
    status = "PENDING"
}) => {
    return axios.get(
        TASK_API_URL,
        {
            ...getAuthConfig(),
            params: {
                date,
                status
            }
        }
    );
};

/**
 * Obtiene una tarea propia por ID.
 */
export const getMedicalAgendaTaskById =
    taskId => {
        return axios.get(
            `${TASK_API_URL}/${taskId}`,
            getAuthConfig()
        );
    };

/**
 * Crea una tarea personal.
 */
export const createMedicalAgendaTask =
    data => {
        return axios.post(
            TASK_API_URL,
            {
                title:
                    data.title
                        ?.trim(),
                description:
                    data.description
                        ?.trim() ||
                    null,
                priority:
                    data.priority,
                dueDateTime:
                    data.dueDateTime
            },
            getAuthConfig()
        );
    };

/**
 * Actualiza una tarea activa.
 *
 * El estado completed no se modifica aquí.
 */
export const updateMedicalAgendaTask = (
    taskId,
    data
) => {
    return axios.put(
        `${TASK_API_URL}/${taskId}`,
        {
            title:
                data.title
                    ?.trim(),
            description:
                data.description
                    ?.trim() ||
                null,
            priority:
                data.priority,
            dueDateTime:
                data.dueDateTime,
            version:
                Number(
                    data.version
                )
        },
        getAuthConfig()
    );
};

/**
 * Completa o reabre una tarea.
 */
export const changeMedicalAgendaTaskCompletion = (
    taskId,
    completed,
    version
) => {
    return axios.patch(
        `${TASK_API_URL}/${taskId}/completion`,
        null,
        {
            ...getAuthConfig(),
            params: {
                completed,
                version
            }
        }
    );
};

/**
 * Activa o elimina lógicamente una tarea.
 */
export const changeMedicalAgendaTaskStatus = (
    taskId,
    active,
    version
) => {
    return axios.patch(
        `${TASK_API_URL}/${taskId}/status`,
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