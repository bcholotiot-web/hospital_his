import axios from "axios";

const API_URL =
    "http://localhost:8080/api/roles";

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
export const getRolesPage = ({
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
 * Obtiene todos los roles.
 *
 * Se conserva para componentes que todavía
 * consumen el listado completo.
 */
export const getRoles = () => {
    return axios.get(
        API_URL,
        getAuthConfig()
    );
};

/**
 * Obtiene únicamente roles activos.
 *
 * Este endpoint puede ser utilizado en el
 * formulario de administración de usuarios.
 */
export const getActiveRoles = () => {
    return axios.get(
        `${API_URL}/active`,
        getAuthConfig()
    );
};

/**
 * Busca un rol por ID.
 */
export const getRoleById = roleId => {
    return axios.get(
        `${API_URL}/${roleId}`,
        getAuthConfig()
    );
};

/**
 * Crea o reactiva un rol.
 *
 * El backend controla el estado inicial.
 */
export const createRole = data => {
    return axios.post(
        API_URL,
        {
            name:
                data.name,
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
export const updateRole = (
    roleId,
    data
) => {
    return axios.put(
        `${API_URL}/${roleId}`,
        {
            name:
                data.name,
            description:
                data.description,
            version:
                data.version
        },
        getAuthConfig()
    );
};

/**
 * Activa o desactiva lógicamente un rol.
 */
export const changeRoleStatus = (
    roleId,
    active,
    version
) => {
    return axios.patch(
        `${API_URL}/${roleId}/status`,
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
