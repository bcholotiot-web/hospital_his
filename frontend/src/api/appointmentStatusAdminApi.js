import axios from "axios";

const API_URL =
    "http://localhost:8080/api/admin/appointment-statuses";

/**
 * Construye la configuración de autorización
 * utilizando el JWT almacenado.
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
 * Obtiene las configuraciones paginadas.
 *
 * Filtros:
 *
 * - search
 * - active
 * - page
 * - size
 */
export const getAppointmentStatusesPage = ({
    search = "",
    active = "",
    page = 0,
    size = 10
} = {}) => {
    const params = {
        page,
        size
    };

    if (search.trim()) {
        params.search =
            search.trim();
    }

    if (
        active !== "" &&
        active !== null &&
        active !== undefined
    ) {
        params.active =
            active;
    }

    return axios.get(
        API_URL,
        {
            ...getAuthConfig(),
            params
        }
    );
};

/**
 * Obtiene todos los códigos técnicos del enum
 * AppointmentStatus.
 *
 * La respuesta indica cuáles ya tienen una
 * configuración persistida.
 */
export const getAvailableAppointmentStatusCodes =
    () => {
        return axios.get(
            `${API_URL}/available-codes`,
            getAuthConfig()
        );
    };

/**
 * Obtiene una configuración por ID.
 */
export const getAppointmentStatusById =
    configurationId => {
        return axios.get(
            `${API_URL}/${configurationId}`,
            getAuthConfig()
        );
    };

/**
 * Crea o reactiva una configuración.
 */
export const createAppointmentStatus =
    data => {
        return axios.post(
            API_URL,
            {
                code:
                    data.code,
                displayName:
                    data.displayName,
                description:
                    data.description,
                displayOrder:
                    data.displayOrder,
                color:
                    data.color,
                patientVisible:
                    data.patientVisible,
                staffVisible:
                    data.staffVisible
            },
            getAuthConfig()
        );
    };

/**
 * Actualiza la presentación de un estado.
 *
 * El código técnico no se modifica.
 */
export const updateAppointmentStatus = (
    configurationId,
    data
) => {
    return axios.put(
        `${API_URL}/${configurationId}`,
        {
            displayName:
                data.displayName,
            description:
                data.description,
            displayOrder:
                data.displayOrder,
            color:
                data.color,
            patientVisible:
                data.patientVisible,
            staffVisible:
                data.staffVisible,
            version:
                data.version
        },
        getAuthConfig()
    );
};

/**
 * Activa o desactiva una configuración.
 */
export const changeAppointmentStatusState = (
    configurationId,
    active,
    version
) => {
    return axios.patch(
        `${API_URL}/${configurationId}/status`,
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