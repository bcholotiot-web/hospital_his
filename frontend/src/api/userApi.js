import axios from "axios";

const API_URL =
    "http://localhost:8080/api/users";

/**
 * Construye la configuración de autorización.
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
 * Listar usuarios activos.
 */
export const getUsers = () => {
    return axios.get(
        API_URL,
        getAuthConfig()
    );
};

/**
 * RN-CU01-01:
 * Buscar usuarios por el campo seleccionado.
 *
 * Petición generada:
 * GET /api/users/search?field=USERNAME&value=admin
 */
export const searchUsers = (
    field,
    value
) => {
    return axios.get(
        `${API_URL}/search`,
        {
            ...getAuthConfig(),

            params: {
                field,
                value
            }
        }
    );
};

/**
 * Crear usuario.
 */
export const createUser = (data) => {
    return axios.post(
        API_URL,
        data,
        getAuthConfig()
    );
};

/**
 * Actualizar usuario.
 */
export const updateUser = (
    id,
    data
) => {
    return axios.put(
        `${API_URL}/${id}`,
        data,
        getAuthConfig()
    );
};

/**
 * Activación o eliminación lógica.
 */
export const changeUserStatus = (
    id,
    active
) => {
    return axios.patch(
        `${API_URL}/${id}/status`,
        {},
        {
            ...getAuthConfig(),

            params: {
                active
            }
        }
    );
};