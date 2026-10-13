import axios from "axios";

const ADMIN_API_URL =
    "http://localhost:8080/api/admin";

/**
 * Construye la configuración con el token JWT.
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
 * Obtiene todos los permisos técnicos activos.
 *
 * El módulo es opcional.
 *
 * Ejemplos:
 *
 * GET /api/admin/permissions
 *
 * GET /api/admin/permissions?module=MEDICATION
 */
export const getPermissions = (
    module = ""
) => {
    const params = {};

    if (
        module &&
        module.trim()
    ) {
        params.module =
            module.trim();
    }

    return axios.get(
        `${ADMIN_API_URL}/permissions`,
        {
            ...getAuthConfig(),
            params
        }
    );
};

/**
 * Obtiene todos los permisos disponibles
 * y el estado de asignación para un rol.
 *
 * GET /api/admin/roles/{roleId}/permissions
 */
export const getRolePermissions =
    roleId => {
        return axios.get(
            `${ADMIN_API_URL}/roles/${roleId}/permissions`,
            getAuthConfig()
        );
    };

/**
 * Reemplaza la selección completa de permisos
 * activos del rol.
 *
 * PUT /api/admin/roles/{roleId}/permissions
 */
export const updateRolePermissions = (
    roleId,
    permissionIds
) => {
    return axios.put(
        `${ADMIN_API_URL}/roles/${roleId}/permissions`,
        {
            permissionIds
        },
        getAuthConfig()
    );
};