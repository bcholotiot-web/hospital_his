import axios from "axios";

const API_URL =
    "http://localhost:8080/api/specialties";

/**
 * Construye la configuración de autenticación.
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
 * Obtiene el listado administrativo paginado.
 *
 * Filtros:
 *
 * - name
 * - active
 * - page
 * - size
 */
export const getSpecialtiesPage = ({
    name = "",
    active = "",
    page = 0,
    size = 10
} = {}) => {
    const params = {
        page,
        size
    };

    if (name.trim()) {
        params.name =
            name.trim();
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
        `${API_URL}/page`,
        {
            ...getAuthConfig(),
            params
        }
    );
};

/**
 * Obtiene el listado completo.
 *
 * Se conserva temporalmente por compatibilidad.
 */
export const getSpecialties = () => {
    return axios.get(
        API_URL,
        getAuthConfig()
    );
};

/**
 * Busca una especialidad por ID.
 */
export const getSpecialtyById = id => {
    return axios.get(
        `${API_URL}/${id}`,
        getAuthConfig()
    );
};

/**
 * Crea o reactiva una especialidad.
 *
 * El backend controla el estado inicial.
 */
export const createSpecialty = data => {
    return axios.post(
        API_URL,
        {
            name: data.name,
            description:
                data.description
        },
        getAuthConfig()
    );
};

/**
 * Actualiza nombre y descripción.
 *
 * La versión es obligatoria.
 */
export const updateSpecialty = (
    id,
    data
) => {
    return axios.put(
        `${API_URL}/${id}`,
        {
            name: data.name,
            description:
                data.description,
            version:
                data.version
        },
        getAuthConfig()
    );
};

/**
 * Activa o desactiva una especialidad.
 */
export const changeSpecialtyStatus = (
    id,
    active,
    version
) => {
    return axios.patch(
        `${API_URL}/${id}/status`,
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
