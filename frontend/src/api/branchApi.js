import axios from "axios";

const API_URL =
    "http://localhost:8080/api/branches";

/**
 * Construye la configuración de autenticación
 * usando el token almacenado después del login.
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
 * Filtros disponibles:
 *
 * - name
 * - active
 * - page
 * - size
 */
export const getBranchesPage = ({
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
 * Se conserva para componentes antiguos
 * que todavía esperan un arreglo.
 */
export const getBranches = () => {
    return axios.get(
        API_URL,
        getAuthConfig()
    );
};

/**
 * Obtiene únicamente sucursales activas.
 *
 * Puede utilizarse en selectores.
 */
export const getActiveBranches = () => {
    return axios.get(
        `${API_URL}/active`,
        getAuthConfig()
    );
};

/**
 * Busca una sucursal por ID.
 */
export const getBranchById = id => {
    return axios.get(
        `${API_URL}/${id}`,
        getAuthConfig()
    );
};

/**
 * Crea o reactiva una sucursal.
 *
 * El estado inicial es controlado
 * por Spring Boot.
 */
export const createBranch = data => {
    return axios.post(
        API_URL,
        {
            name:
                data.name,
            phone:
                data.phone || null,
            address:
                data.address || null,
            description:
                data.description || null
        },
        getAuthConfig()
    );
};

/**
 * Actualiza la información de una sucursal.
 *
 * La versión es obligatoria para controlar
 * modificaciones simultáneas.
 */
export const updateBranch = (
    id,
    data
) => {
    return axios.put(
        `${API_URL}/${id}`,
        {
            name:
                data.name,
            phone:
                data.phone || null,
            address:
                data.address || null,
            description:
                data.description || null,
            version:
                data.version
        },
        getAuthConfig()
    );
};

/**
 * Activa o desactiva lógicamente una sucursal.
 */
export const changeBranchStatus = (
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