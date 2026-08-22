import axios from "axios";

const API_URL =
    "http://localhost:8080/api/payments";

const getAuthHeaders = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

export const validateAppointmentPayment = (
    appointmentId
) => {
    return axios.get(
        `${API_URL}/appointments/${appointmentId}/validate`,
        getAuthHeaders()
    );
};

export const processPayment = (data) => {
    return axios.post(
        `${API_URL}/process`,
        data,
        getAuthHeaders()
    );

};

export const getPaymentSummary = (
    appointmentId
) => {
    return axios.get(
        `${API_URL}/appointments/${appointmentId}/summary`,
        getAuthHeaders()
    );
};