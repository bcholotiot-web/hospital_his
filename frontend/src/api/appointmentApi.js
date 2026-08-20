import axios from "axios";

const API_URL = "http://localhost:8080/api/appointments";

const getAuthHeaders = () => {
    const token = localStorage.getItem("token");

    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

//Obtiene las sucursales
export const getAppointmentBranches = () => {
    return axios.get(
        `${API_URL}/branches`,
        getAuthHeaders()
    );
};

//Obtiene las especialidades de una sucursal
export const getSpecialtiesByBranch = (branchId) => {
    return axios.get(
        `${API_URL}/specialties`,
        {
            params: {
                branchId
            },
            ...getAuthHeaders()
        }
    );
};

//Obtiene medicos de una sucursal y especialidad
export const getDoctors = (branchId, specialtyId) => {
    return axios.get(
        `${API_URL}/doctors`,
        {
            params: {
                branchId,
                specialtyId
            },
            ...getAuthHeaders()
        }
    );
};

//Obtiene los horarios disponibles de un medico 
export const getAvailability = (doctorId, date) => {
    return axios.get(
        `${API_URL}/availability`,
        {
            params: {
                doctorId,
                date
            },
            ...getAuthHeaders()
        }
    );
};

//Registra la cita
export const createAppointment = (data) => {
    return axios.post(
        API_URL,
        data,
        getAuthHeaders()
    );
};
``