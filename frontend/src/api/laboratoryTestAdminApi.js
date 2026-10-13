import axios from "axios";

const API_URL =
    "http://localhost:8080/api/admin/laboratory-tests";

/**
 * Construye la configuración con el token JWT.
 */
const getAuthConfig = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization:
                `Bearer ${token}`
        }
    };
};

/**
 * Obtiene el catálogo administrativo paginado.
 *
 * Filtros:
 *
 * - search: código o nombre.
 * - active: true, false o vacío.
 * - page: página basada en cero.
 * - size: cantidad de registros.
 */
export const getLaboratoryTestsPage = ({
    search = "",
    active = "",
    page = 0,
    size = 10
} = {}) => {
    const params = {
        page,
        size
    };

    if (search.trim()) {
        params.search =
            search.trim();
    }

    if (
        active !== "" &&
        active !== null &&
        active !== undefined
    ) {
        params.active =
            active;
    }

    return axios.get(
        API_URL,
        {
            ...getAuthConfig(),
            params
        }
    );
};

/**
 * Obtiene únicamente exámenes activos.
 */
export const getActiveLaboratoryTests =
    () => {
        return axios.get(
            `${API_URL}/active`,
            getAuthConfig()
        );
    };

/**
 * Busca un examen por ID.
 */
export const getLaboratoryTestById =
    testId => {
        return axios.get(
            `${API_URL}/${testId}`,
            getAuthConfig()
        );
    };

/**
 * Crea o reactiva un examen.
 *
 * El backend controla active=true.
 */
export const createLaboratoryTest =
    data => {
        return axios.post(
            API_URL,
            {
                code:
                    data.code,
                name:
                    data.name,
                description:
                    data.description || null,
                referenceRange:
                    data.referenceRange || null,
                defaultUnit:
                    data.defaultUnit || null,
                price:
                    data.price
            },
            getAuthConfig()
        );
    };

/**
 * Actualiza un examen.
 *
 * La versión es obligatoria.
 */
export const updateLaboratoryTest = (
    testId,
    data
) => {
    return axios.put(
        `${API_URL}/${testId}`,
        {
            code:
                data.code,
            name:
                data.name,
            description:
                data.description || null,
            referenceRange:
                data.referenceRange || null,
            defaultUnit:
                data.defaultUnit || null,
            price:
                data.price,
            version:
                data.version
        },
        getAuthConfig()
    );
};

/**
 * Activa o desactiva el examen.
 */
export const changeLaboratoryTestStatus = (
    testId,
    active,
    version
) => {
    return axios.patch(
        `${API_URL}/${testId}/status`,
        null,
        {
            ...getAuthConfig(),
            params: {
                active,
                version
            }
        }
    );
}; 