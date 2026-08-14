import axios from "axios";


const API_URL = "http://localhost:8080/api/users";

//Listar a los usuarios
export const getUsers = () => {

    const token =
        localStorage.getItem("token");

    return axios.get(
        API_URL,
        {
            headers: {
                Authorization: `Bearer ${token}`
            }
        }
    );
};

//Crear a los usuarios
export const createUser = (data) => {

    const token =
        localStorage.getItem("token");

    return axios.post(
        API_URL,
        data,
        {
            headers: {
                Authorization: `Bearer ${token}`
            }
        }
    );
};

//Actualizar a los usuarios
export const updateUser = (id, data) => {
    const token = localStorage.getItem("token");

    return axios.put(
        `${API_URL}/${id}`,
        data,
        {
            headers: {
                Authorization: `Bearer ${token}`
            }
        }
    );
};

export const changeUserStatus = (id, active) => {
    const token = localStorage.getItem("token");

    return axios.patch(
        `${API_URL}/${id}/status?active=${active}`,
        {},
        {
            headers: {
                Authorization: `Bearer ${token}`
            }
        }
    );
};