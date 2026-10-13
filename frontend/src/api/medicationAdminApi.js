import axios from "axios";

const API_URL =
    "http://localhost:8080/api/admin/medications";

/**
 * Construye la configuración de autorización.
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
 * Obtiene el catálogo administrativo paginado.
 *
 * Filtros:
 *
 * - search: código, nombre o principio activo.
 * - active: true, false o vacío.
 * - controlled: true, false o vacío.
 * - page: página basada en cero.
 * - size: cantidad de registros.
 */
export const getMedicationsPage = ({
    search = "",
    active = "",
    controlled = "",
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

    if (
        controlled !== "" &&
        controlled !== null &&
        controlled !== undefined
    ) {
        params.controlled =
            controlled;
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
 * Obtiene únicamente medicamentos activos.
 */
export const getActiveMedications = () => {
    return axios.get(
        `${API_URL}/active`,
        getAuthConfig()
    );
};

/**
 * Busca un medicamento por ID.
 */
export const getMedicationById =
    medicationId => {
        return axios.get(
            `${API_URL}/${medicationId}`,
            getAuthConfig()
        );
    };

/**
 * Crea o reactiva un medicamento.
 *
 * El backend controla el estado inicial.
 */
export const createMedication = data => {
    return axios.post(
        API_URL,
        {
            code:
                data.code,
            name:
                data.name,
            activeIngredient:
                data.activeIngredient,
            concentration:
                data.concentration,
            presentation:
                data.presentation,
            unit:
                data.unit,
            description:
                data.description,
            unitPrice:
                data.unitPrice,
            controlled:
                data.controlled
        },
        getAuthConfig()
    );
};

/**
 * Actualiza un medicamento.
 *
 * La versión es obligatoria.
 */
export const updateMedication = (
    medicationId,
    data
) => {
    return axios.put(
        `${API_URL}/${medicationId}`,
        {
            code:
                data.code,
            name:
                data.name,
            activeIngredient:
                data.activeIngredient,
            concentration:
                data.concentration,
            presentation:
                data.presentation,
            unit:
                data.unit,
            description:
                data.description,
            unitPrice:
                data.unitPrice,
            controlled:
                data.controlled,
            version:
                data.version
        },
        getAuthConfig()
    );
};

/**
 * Activa o desactiva lógicamente
 * un medicamento.
 */
export const changeMedicationStatus = (
    medicationId,
    active,
    version
) => {
    return axios.patch(
        `${API_URL}/${medicationId}/status`,
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
