import axios from "axios";

/*
 * Ruta administrativa de CU-13.
 *
 * Spring Boot exige que el usuario autenticado
 * tenga rol ADMIN.
 */
const API_URL =
    "http://localhost:8080/api/admin/branch-specialties";

/**
 * Construye los encabezados de autenticación
 * utilizando el JWT almacenado después del login.
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
 * Obtiene las asignaciones sede-especialidad.
 *
 * Parámetros:
 *
 * id:
 * filtro opcional por ID de asignación.
 *
 * page:
 * página actual iniciada en cero.
 *
 * size:
 * cantidad de registros por página.
 */
export const getBranchSpecialtyAssignments = (
    id,
    page = 0,
    size = 20
) => {
    const params = {
        page,
        size
    };

    /*
     * El filtro solamente se envía cuando contiene
     * un ID válido.
     */
    if (
        id !== null &&
        id !== undefined &&
        String(id).trim() !== ""
    ) {
        params.id = id;
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
 * Obtiene las sedes activas disponibles
 * para crear una asignación.
 */
export const getActiveBranchOptions = () => {
    return axios.get(
        `${API_URL}/options/branches`,
        getAuthConfig()
    );
};

/**
 * Obtiene las especialidades activas disponibles
 * para crear una asignación.
 */
export const getActiveSpecialtyOptions = () => {
    return axios.get(
        `${API_URL}/options/specialties`,
        getAuthConfig()
    );
};

/**
 * Crea una asignación nueva o reactiva una
 * asignación inactiva.
 *
 * El backend decide:
 *
 * - Estado.
 * - Usuario de creación.
 * - Usuario de actualización.
 * - Fechas.
 * - Versión.
 */
export const createBranchSpecialtyAssignment = (
    branchId,
    specialtyId
) => {
    return axios.post(
        API_URL,
        {
            branchId,
            specialtyId
        },
        getAuthConfig()
    );
};

/**
 * Desactiva lógicamente una asignación.
 *
 * No elimina físicamente la fila en PostgreSQL.
 */
export const deactivateBranchSpecialtyAssignment = (
    assignmentId
) => {
    return axios.delete(
        `${API_URL}/${assignmentId}`,
        getAuthConfig()
    );
};

/**
 * Endpoint utilizado por CU-03.
 *
 * Obtiene únicamente las especialidades activas
 * configuradas para la sede seleccionada.
 */
export const getSpecialtiesByBranch = (
    branchId
) => {
    return axios.get(
        `http://localhost:8080/api/appointments/branches/${branchId}/specialties`,
        getAuthConfig()
    );
};