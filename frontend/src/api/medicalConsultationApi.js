import axios from "axios";

const API_URL =
    "http://localhost:8080/api/doctor";

const getAuthHeaders = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

export const getDoctorQueue = () => {
    return axios.get(
        `${API_URL}/consultations/queue`,
        getAuthHeaders()
    );
};

export const startMedicalConsultation = (
    appointmentId
) => {
    return axios.patch(
        `${API_URL}/appointments/${appointmentId}/start`,
        {},
        getAuthHeaders()
    );
};

export const getMedicalConsultation = (
    appointmentId
) => {
    return axios.get(
        `${API_URL}/appointments/${appointmentId}/consultation`,
        getAuthHeaders()
    );
};

export const saveMedicalConsultation = (
    appointmentId,
    data
) => {
    return axios.put(
        `${API_URL}/appointments/${appointmentId}/consultation`,
        data,
        getAuthHeaders()
    );
};

export const finishPatientCare = (
    appointmentId
) => {
    return axios.patch(
        `${API_URL}/appointments/${appointmentId}/finish-care`,
        {},
        getAuthHeaders()
    );
};

export const markAppointmentNoShow = (
    appointmentId
) => {
    return axios.patch(
        `${API_URL}/appointments/${appointmentId}/no-show`,
        {},
        getAuthHeaders()
    );
};

export const searchIcd10Codes = (
    query
) => {
    return axios.get(
        `${API_URL}/icd10/search`,
        {
            params: {
                query
            },
            ...getAuthHeaders()
        }
    );
};