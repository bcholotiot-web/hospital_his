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
/**
 * Registra una cita con un documento PDF opcional.
 *
 * La petición contiene:
 * - appointment: información JSON de la cita.
 * - document: archivo PDF opcional.
 */
export const createAppointment = (
    appointmentData,
    document
) => {
    const formData = new FormData();

    /*
     * Spring Boot recibirá esta parte mediante:
     *
     * @RequestPart("appointment")
     * CreateAppointmentRequest request
     */
    const appointmentBlob = new Blob(
        [JSON.stringify(appointmentData)],
        {
            type: "application/json"
        }
    );

    formData.append(
        "appointment",
        appointmentBlob
    );

    /*
     * El documento es opcional.
     * Si no se selecciona, no se agrega esta parte.
     */
    if (document) {
        formData.append(
            "document",
            document
        );
    }

    const token =
        localStorage.getItem("token");

    /*
     * No se establece manualmente Content-Type.
     *
     * El navegador agregará:
     * multipart/form-data; boundary=...
     */
    return axios.post(
        API_URL,
        formData,
        {
            headers: {
                Authorization:
                    `Bearer ${token}`
            }
        }
    );
};

export const getMyAppointments = () => {
    return axios.get(
        `${API_URL}/my`,
        getAuthHeaders()
    );
};