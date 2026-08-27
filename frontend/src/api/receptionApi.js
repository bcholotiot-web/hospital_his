import axios from "axios";

const API_URL =
    "http://localhost:8080/api/reception";

const getAuthHeaders = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

export const searchReceptionAppointment = (
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

export const registerPatientArrival = (
    appointmentId
) => {
    return axios.patch(
        `${API_URL}/appointments/${appointmentId}/arrival`,
        {},
        getAuthHeaders()
    );
};

export const getAvailableReceptionDoctors = (
    appointmentId
) => {
    return axios.get(
        `${API_URL}/appointments/${appointmentId}/available-doctors`,
        getAuthHeaders()
    );
};

export const reassignAppointmentDoctor = (
    appointmentId,
    data
) => {
    return axios.patch(
        `${API_URL}/appointments/${appointmentId}/reassign-doctor`,
        data,
        getAuthHeaders()
    );
};

export const getReceptionBranches = () => {
    return axios.get(
        `${API_URL}/branches`,
        getAuthHeaders()
    );
};

export const registerEmergencyReception = (
    data
) => {
    return axios.post(
        `${API_URL}/emergencies`,
        data,
        getAuthHeaders()
    );
};