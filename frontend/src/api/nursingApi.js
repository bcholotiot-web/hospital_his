import axios from "axios";

const API_URL =
    "http://localhost:8080/api/nursing";

const getAuthHeaders = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

export const getNursingQueue = () => {
    return axios.get(
        `${API_URL}/queue`,
        getAuthHeaders()
    );
};

export const callAppointmentPatient = (
    appointmentId
) => {
    return axios.patch(
        `${API_URL}/appointments/${appointmentId}/call`,
        {},
        getAuthHeaders()
    );
};

export const callEmergencyPatient = (
    emergencyReceptionId
) => {
    return axios.patch(
        `${API_URL}/emergencies/${emergencyReceptionId}/call`,
        {},
        getAuthHeaders()
    );
};

export const registerVitalSigns = (
    data
) => {
    return axios.post(
        `${API_URL}/vital-signs`,
        data,
        getAuthHeaders()
    );
};

export const getAppointmentVitalSigns = (
    appointmentId
) => {
    return axios.get(
        `${API_URL}/appointments/${appointmentId}/vital-signs`,
        getAuthHeaders()
    );
};

export const getEmergencyVitalSigns = (
    emergencyReceptionId
) => {
    return axios.get(
        `${API_URL}/emergencies/${emergencyReceptionId}/vital-signs`,
        getAuthHeaders()
    );
};