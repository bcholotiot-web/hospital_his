import axios from "axios";

const API_URL = "http://localhost:8080/api/specialties";

const getAuthHeaders = () => {
    const token = localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

export const getSpecialties = () => {
    return axios.get(API_URL, getAuthHeaders());
};

export const createSpecialty = (data) => {
    return axios.post(API_URL, data, getAuthHeaders());
};

export const updateSpecialty = (id, data) => {
    return axios.put(`${API_URL}/${id}`, data, getAuthHeaders());
};

export const changeSpecialtyStatus = (id, active) => {
    return axios.patch(
        `${API_URL}/${id}/status?active=${active}`,
        {},
        getAuthHeaders()
    );
};