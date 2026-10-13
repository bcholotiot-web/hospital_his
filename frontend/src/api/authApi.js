import axios from "axios";

const API_URL =
    "http://localhost:8080/api/auth";

/**
 * Inicia sesión.
 */
export const login = data => {
    return axios.post(
        `${API_URL}/login`,
        data
    );
};

/**
 * Registra un paciente.
 */
export const register = data => {
    return axios.post(
        `${API_URL}/register`,
        data
    );
};

/**
 * Verifica el DPI antes del registro
 * o del inicio de sesión.
 */
export const verifyDpi = data => {
    return axios.post(
        `${API_URL}/verify-dpi`,
        data
    );
};

/**
 * Recupera la sesión y los permisos actuales.
 *
 * GET /api/auth/me
 */
export const getAuthenticatedUser = token => {
    return axios.get(
        `${API_URL}/me`,
        {
            headers: {
                Authorization:
                    `Bearer ${token}`
            }
        }
    );
};