import axios from "axios";

const API_URL = "http://localhost:8080/api/branches";

export const getBranches = () => {

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