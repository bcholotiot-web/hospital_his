import axios from "axios";

const API_URL =
    "http://localhost:8080/api/laboratory";

const getAuthHeaders = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

export const getLaboratoryOrders = (
    filters = {}
) => {
    const params = {};

    if (
        filters.status &&
        filters.status !== "TODOS"
    ) {
        params.status =
            filters.status;
    }

    if (filters.patient?.trim()) {
        params.patient =
            filters.patient.trim();
    }

    if (filters.doctor?.trim()) {
        params.doctor =
            filters.doctor.trim();
    }

    return axios.get(
        `${API_URL}/orders`,
        {
            params,
            ...getAuthHeaders()
        }
    );
};

export const getLaboratoryOrder = (
    orderId
) => {
    return axios.get(
        `${API_URL}/orders/${orderId}`,
        getAuthHeaders()
    );
};

export const saveLaboratoryResult = (
    orderId,
    itemId,
    data
) => {
    return axios.put(
        `${API_URL}/orders/${orderId}/items/${itemId}/result`,
        data,
        getAuthHeaders()
    );
};

export const publishLaboratoryResult = (
    orderId,
    itemId
) => {
    return axios.patch(
        `${API_URL}/orders/${orderId}/items/${itemId}/publish`,
        {},
        getAuthHeaders()
    );
};