import axios from "axios";

const API_URL =
    "http://localhost:8080/api/cashier";

const getAuthHeaders = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

export const searchPendingAppointment = (
    type,
    value
) => {
    return axios.get(
        `${API_URL}/appointments/search`,
        {
            params: {
                type,
                value
            },
            ...getAuthHeaders()
        }
    );
};

export const registerCashierPayment = (
    data
) => {
    return axios.post(
        `${API_URL}/payments`,
        data,
        getAuthHeaders()
    );
};