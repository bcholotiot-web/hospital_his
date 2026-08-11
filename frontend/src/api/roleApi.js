import axios from "axios";

const API_URL =
    "http://localhost:8080/api/roles";

export const getRoles = () => {

    const token =
        localStorage.getItem("token");

    return axios.get(
        API_URL,
        {
            headers: {
                Authorization:
                    `Bearer ${token}`
            }
        }
    );
};