import axios from "axios";

/*
 * Ruta base del backend para CU-10.
 *
 * Esta ruta está protegida para usuarios con rol CAJERO.
 */
const API_URL =
    "http://localhost:8080/api/cashier/laboratory-payments";

/**
 * Construye los encabezados de autorización.
 *
 * El token JWT permite al backend:
 * 1. Identificar al cajero.
 * 2. Validar el rol CAJERO.
 * 3. Validar la sucursal asignada.
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
 * Busca órdenes pendientes de pago.
 *
 * criteria:
 * - DPI
 * - ORDER_NUMBER
 *
 * value:
 * - DPI de 13 dígitos.
 * - Número completo de orden.
 */
export const searchPendingLaboratoryOrders = (
    criteria,
    value
) => {
    return axios.get(
        `${API_URL}/orders/search`,
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
 * Obtiene el detalle de la orden seleccionada.
 *
 * Incluye:
 * - Paciente.
 * - Médico.
 * - Sucursal.
 * - Exámenes.
 * - Precios históricos.
 * - Total.
 */
export const getLaboratoryOrderPaymentDetail = (
    orderId
) => {
    return axios.get(
        `${API_URL}/orders/${orderId}`,
        getAuthConfig()
    );
};

/**
 * Registra el pago de una orden.
 *
 * Admite:
 * - EFECTIVO
 * - VISA
 * - MASTERCARD
 * - DEBITO
 */
export const registerLaboratoryPayment = (
    data
) => {
    return axios.post(
        API_URL,
        data,
        getAuthConfig()
    );
};

/**
 * Recupera un comprobante mediante su
 * número de transacción.
 */
export const getLaboratoryPaymentReceipt = (
    transactionNumber
) => {
    return axios.get(
        `${API_URL}/receipts/${encodeURIComponent(
            transactionNumber
        )}`,
        getAuthConfig()
    );
};

/**
 * Obtiene el historial de pagos realizados
 * por el cajero autenticado.
 */
export const getMyLaboratoryPayments = () => {
    return axios.get(
        `${API_URL}/my`,
        getAuthConfig()
    );
};