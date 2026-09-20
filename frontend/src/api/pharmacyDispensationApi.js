import axios from "axios";

/*
 * Ruta base del módulo de despacho.
 *
 * Todos estos endpoints están protegidos
 * en Spring Boot para el rol FARMACIA.
 */
const DISPENSATION_API_URL =
    "http://localhost:8080/api/pharmacy/dispensations";

/*
 * Ruta base del inventario de Farmacia.
 */
const INVENTORY_API_URL =
    "http://localhost:8080/api/pharmacy/inventory";

/**
 * Construye los encabezados de autorización
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
 * Busca una receta para despacho.
 *
 * Criterios admitidos:
 *
 * PRESCRIPTION_NUMBER
 * CONSULTATION_ID
 *
 * Ejemplo:
 *
 * GET /api/pharmacy/dispensations/prescriptions/search
 *     ?criteria=PRESCRIPTION_NUMBER
 *     &value=REC-A1B2C3D4E5F6
 */
export const searchPrescriptionsForDispensation = (
    criteria,
    value
) => {
    return axios.get(
        `${DISPENSATION_API_URL}/prescriptions/search`,
        {
            ...getAuthConfig(),

            params: {
                criteria,
                value
            }
        }
    );
};

/**
 * Obtiene el detalle de una receta.
 *
 * La respuesta incluye:
 *
 * - Vigencia.
 * - Cantidades prescritas.
 * - Cantidades ya entregadas.
 * - Cantidades pendientes.
 * - Existencias por sucursal.
 * - Alternativas disponibles.
 * - Alertas de stock.
 */
export const getPrescriptionDispensationDetail = (
    prescriptionId
) => {
    return axios.get(
        `${DISPENSATION_API_URL}/prescriptions/${prescriptionId}`,
        getAuthConfig()
    );
};

/**
 * Confirma un despacho.
 *
 * Spring Boot:
 *
 * - Revalida la receta.
 * - Revalida la vigencia.
 * - Bloquea el inventario.
 * - Calcula el total.
 * - Procesa el cobro.
 * - Descuenta existencias si el cobro es aprobado.
 * - Actualiza cantidades de la receta.
 */
export const confirmMedicationDispensation = (
    data
) => {
    return axios.post(
        DISPENSATION_API_URL,
        data,
        getAuthConfig()
    );
};

/**
 * Recupera un comprobante de despacho.
 */
export const getMedicationDispensationReceipt = (
    dispensationId
) => {
    return axios.get(
        `${DISPENSATION_API_URL}/${dispensationId}/receipt`,
        getAuthConfig()
    );
};

/**
 * Obtiene el historial de despachos realizados
 * por el usuario de Farmacia autenticado.
 */
export const getMyMedicationDispensations = () => {
    return axios.get(
        `${DISPENSATION_API_URL}/my`,
        getAuthConfig()
    );
};

/**
 * Obtiene el inventario activo de la sucursal
 * asignada al usuario de Farmacia.
 */
export const getMyPharmacyInventory = () => {
    return axios.get(
        INVENTORY_API_URL,
        getAuthConfig()
    );
};

/**
 * Obtiene las alertas de stock mínimo
 * de la sucursal del usuario autenticado.
 */
export const getPharmacyInventoryAlerts = () => {
    return axios.get(
        `${INVENTORY_API_URL}/alerts`,
        getAuthConfig()
    );
};

/**
 * Registra que el paciente decidió no adquirir
 * los medicamentos durante esta visita.
 *
 * La receta permanece vigente y el inventario
 * no se modifica.
 */
export const declinePrescriptionDispensation = (
    prescriptionId,
    reason
) => {
    return axios.post(
        `${DISPENSATION_API_URL}/prescriptions/${prescriptionId}/decline`,
        {
            reason
        },
        getAuthConfig()
    );
};