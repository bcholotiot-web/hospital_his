import axios from "axios";

const API_URL =
    "http://localhost:8080/api/auth";

export const login = (data) => {
    return axios.post(
        `${API_URL}/login`,
        data
    );
};

export const register = (data) => {
    return axios.post(
        `${API_URL}/register`,
        data
    );
};

/**
 * CU-00: Verificación pública del DPI antes
 * de agendar una cita.
 */
export const verifyDpi = (data) => {
    return axios.post(
        `${API_URL}/verify-dpi`,
        data
    );
};