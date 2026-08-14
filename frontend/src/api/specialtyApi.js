import axios from "axios";

const API_URL = "http://localhost:8080/api/specialties";

export const getSpecialties = () => {

    const token = localStorage.getItem("token");

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