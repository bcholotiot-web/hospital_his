import axios from "axios";

const INVENTORY_API_URL =
    "http://localhost:8080/api/pharmacy/inventory";

const MEDICATION_API_URL =
    "http://localhost:8080/api/pharmacy/medications";

/**
 * Construye la configuración de autenticación
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
 * Obtiene inventarios activos e inactivos
 * de la sucursal del usuario de Farmacia.
 *
 * GET /api/pharmacy/inventory/administration
 */
export const getInventoryAdministration =
    () => {
        return axios.get(
            `${INVENTORY_API_URL}/administration`,
            getAuthConfig()
        );
    };

/**
 * Obtiene únicamente inventarios activos.
 *
 * Se mantiene para operaciones y paneles
 * que no deben mostrar registros inactivos.
 */
export const getActiveInventory = () => {
    return axios.get(
        INVENTORY_API_URL,
        getAuthConfig()
    );
};

/**
 * Obtiene un registro de inventario por ID.
 */
export const getInventoryById =
    inventoryId => {
        return axios.get(
            `${INVENTORY_API_URL}/${inventoryId}`,
            getAuthConfig()
        );
    };

/**
 * Obtiene alertas de stock mínimo.
 */
export const getInventoryAlerts = () => {
    return axios.get(
        `${INVENTORY_API_URL}/alerts`,
        getAuthConfig()
    );
};

/**
 * Obtiene medicamentos activos disponibles
 * para crear o reactivar inventario.
 */
export const getPharmacyMedications = () => {
    return axios.get(
        MEDICATION_API_URL,
        getAuthConfig()
    );
};

/**
 * Busca medicamentos activos.
 */
export const searchPharmacyMedications =
    value => {
        return axios.get(
            `${MEDICATION_API_URL}/search`,
            {
                ...getAuthConfig(),
                params: {
                    value:
                        value?.trim() || ""
                }
            }
        );
    };

/**
 * Crea o reactiva inventario.
 *
 * Si la combinación sucursal-medicamento no existe:
 * crea STOCK_INICIAL.
 *
 * Si existe inactiva:
 * reactiva y genera REACTIVACION.
 */
export const createInventory = data => {
    return axios.post(
        INVENTORY_API_URL,
        {
            medicationId:
                data.medicationId,
            currentStock:
                data.currentStock,
            minimumStock:
                data.minimumStock
        },
        getAuthConfig()
    );
};

/**
 * Actualiza stock actual y stock mínimo.
 *
 * La versión es obligatoria.
 */
export const updateInventory = (
    inventoryId,
    data
) => {
    return axios.put(
        `${INVENTORY_API_URL}/${inventoryId}`,
        {
            currentStock:
                data.currentStock,
            minimumStock:
                data.minimumStock,
            version:
                data.version
        },
        getAuthConfig()
    );
};

/**
 * Activa o desactiva el registro.
 *
 * Actualmente el contrato del backend no recibe
 * version como parámetro para esta operación.
 */
export const changeInventoryStatus = (
    inventoryId,
    active
) => {
    return axios.patch(
        `${INVENTORY_API_URL}/${inventoryId}/status`,
        null,
        {
            ...getAuthConfig(),
            params: {
                active
            }
        }
    );
};