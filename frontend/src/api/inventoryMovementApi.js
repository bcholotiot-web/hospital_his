import axios from "axios";

const API_URL =
    "http://localhost:8080/api/pharmacy/inventory-movements";

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
 * Consulta la bitácora paginada de movimientos.
 *
 * Filtros:
 *
 * - search
 * - movementType
 * - startDate
 * - endDate
 * - page
 * - size
 */
export const getInventoryMovements = ({
    search = "",
    movementType = "",
    startDate = "",
    endDate = "",
    page = 0,
    size = 20
} = {}) => {
    const params = {
        page,
        size
    };

    if (search.trim()) {
        params.search =
            search.trim();
    }

    if (movementType) {
        params.movementType =
            movementType;
    }

    if (startDate) {
        params.startDate =
            startDate;
    }

    if (endDate) {
        params.endDate =
            endDate;
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
 * Obtiene el resumen mensual de movimientos.
 */
export const getInventoryMonthlySummary = (
    year,
    month
) => {
    return axios.get(
        `${API_URL}/monthly-summary`,
        {
            ...getAuthConfig(),
            params: {
                year,
                month
            }
        }
    );
};