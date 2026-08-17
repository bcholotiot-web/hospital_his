import axios from "axios";

const API_URL = "http://localhost:8080/api/branches";

const getAuthHeaders = () => {
    const token = localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

export const getBranches = () => {
    return axios.get(API_URL, getAuthHeaders());
};

export const createBranch = (data) => {
    return axios.post(API_URL, data, getAuthHeaders());
};

export const updateBranch = (id, data) => {
    return axios.put(`${API_URL}/${id}`, data, getAuthHeaders());
};

export const changeBranchStatus = (id, active) => {
    return axios.patch(
        `${API_URL}/${id}/status?active=${active}`,
        {},
        getAuthHeaders()
    );
};