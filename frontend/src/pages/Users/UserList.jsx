import {
    useEffect,
    useState
} from "react";

import {
    getUsers,
    searchUsers
} from "../../api/userApi";

import "./Users.css";
import "../../styles/tables.css";
import "../../styles/ui.css";

function UserList({
    onEdit,
    onDelete
}) {
    const [users, setUsers] =
        useState([]);

    const [filterField, setFilterField] =
        useState("USERNAME");

    const [searchText, setSearchText] =
        useState("");

    const [pageSize, setPageSize] =
        useState(10);

    const [currentPage, setCurrentPage] =
        useState(1);

    const [loading, setLoading] =
        useState(false);

    const [searching, setSearching] =
        useState(false);

    const [errorMessage, setErrorMessage] =
        useState("");

    const [hasSearched, setHasSearched] =
        useState(false);

    /**
     * Cargar usuarios cuando se monta el componente.
     */
    useEffect(() => {
        loadUsers();
    }, []);

    /**
     * Obtiene el primer mensaje enviado por Spring Boot.
     */
    const getBackendMessage = (error) => {
        const errorData =
            error.response?.data;

        const validationErrors =
            errorData?.validationErrors;

        if (validationErrors) {
            const messages =
                Object.values(
                    validationErrors
                );

            if (messages.length > 0) {
                return messages[0];
            }
        }

        return (
            errorData?.message ||
            "No fue posible consultar los usuarios."
        );
    };

    /**
     * Obtiene el listado general.
     *
     * Se aplica un filtro defensivo para evitar mostrar
     * un usuario inactivo aunque el backend lo devolviera.
     */
    const loadUsers = async () => {
        setLoading(true);
        setErrorMessage("");
        setHasSearched(false);

        try {
            const response =
                await getUsers();

            console.log(
                "Respuesta de usuarios:",
                response.data
            );

            const activeUsers = (
                response.data || []
            ).filter(
                user =>
                    user.active === true
            );

            setUsers(activeUsers);
            setCurrentPage(1);

        } catch (error) {
            console.error(
                "Error al cargar usuarios:",
                error
            );

            setUsers([]);

            if (error.response) {
                setErrorMessage(
                    getBackendMessage(error)
                );
            } else {
                setErrorMessage(
                    "No fue posible comunicarse con el servidor."
                );
            }
        } finally {
            setLoading(false);
        }
    };

    /**
     * Valida RN-CU01-01 antes de enviar la petición.
     */
    const validateSearch = () => {
        const normalizedText =
            searchText.trim();

        if (!filterField) {
            setErrorMessage(
                "Debe seleccionar un campo de filtro."
            );

            return false;
        }

        if (!normalizedText) {
            setErrorMessage(
                "Debe ingresar un criterio de búsqueda."
            );

            return false;
        }

        if (normalizedText.length > 25) {
            setErrorMessage(
                "El campo de búsqueda no puede exceder los 25 caracteres."
            );

            return false;
        }

        if (
            filterField === "DPI" &&
            !/^\d{1,13}$/.test(
                normalizedText
            )
        ) {
            setErrorMessage(
                "El criterio de búsqueda del DPI debe contener únicamente números y no exceder los 13 dígitos."
            );

            return false;
        }

        setErrorMessage("");
        return true;
    };

    /**
     * Realiza la búsqueda en Spring Boot.
     *
     * Coloca el breakpoint dentro de esta función.
     */
    const handleSearch = async () => {
        console.log(
            "Inicio de búsqueda"
        );

        if (searching) {
            return;
        }

        if (!validateSearch()) {
            return;
        }

        const normalizedText =
            searchText.trim();

        console.log(
            "Campo seleccionado:",
            filterField
        );

        console.log(
            "Criterio enviado:",
            normalizedText
        );

        setSearching(true);
        setErrorMessage("");
        setHasSearched(true);

        try {
            const response =
                await searchUsers(
                    filterField,
                    normalizedText
                );

            console.log(
                "Respuesta de búsqueda:",
                response.data
            );

            const activeUsers = (
                response.data || []
            ).filter(
                user =>
                    user.active === true
            );

            setUsers(activeUsers);
            setCurrentPage(1);

        } catch (error) {
            console.error(
                "Error durante la búsqueda:",
                error
            );

            setUsers([]);

            if (error.response) {
                setErrorMessage(
                    getBackendMessage(error)
                );
            } else {
                setErrorMessage(
                    "No fue posible comunicarse con el servidor."
                );
            }
        } finally {
            setSearching(false);
        }
    };

    /**
     * Limpia el filtro y vuelve a cargar los usuarios.
     */
    const handleClearSearch = async () => {
        setFilterField("USERNAME");
        setSearchText("");
        setErrorMessage("");
        setHasSearched(false);

        await loadUsers();
    };

    /**
     * Calcula la paginación.
     */
    const startIndex =
        (currentPage - 1) * pageSize;

    const endIndex =
        startIndex + pageSize;

    const paginatedUsers =
        users.slice(
            startIndex,
            endIndex
        );

    const totalPages =
        Math.ceil(
            users.length / pageSize
        );

    return (
        <div>
            <h2>
                Listado de Usuarios
            </h2>

            {/* Barra de búsqueda */}
            <div className="search-bar">
                <select
                    value={filterField}
                    onChange={(event) => {
                        setFilterField(
                            event.target.value
                        );

                        setErrorMessage("");
                    }}
                    disabled={loading || searching}
                >
                    <option value="USERNAME">
                        Usuario
                    </option>

                    <option value="NAME">
                        Nombre
                    </option>

                    <option value="DPI">
                        DPI
                    </option>

                    <option value="ROLE">
                        Rol
                    </option>

                    <option value="BRANCH">
                        Sucursal
                    </option>
                </select>

                <input
                    type="text"
                    placeholder="Ingrese el criterio de búsqueda"
                    value={searchText}
                    maxLength={25}
                    onChange={(event) => {
                        let value =
                            event.target.value;

                        /*
                         * Para DPI solo se conservan números.
                         */
                        if (
                            filterField === "DPI"
                        ) {
                            value = value
                                .replace(
                                    /\D/g,
                                    ""
                                )
                                .slice(0, 13);
                        }

                        setSearchText(value);
                        setErrorMessage("");
                    }}
                    onKeyDown={(event) => {
                        if (
                            event.key ===
                            "Enter"
                        ) {
                            event.preventDefault();
                            handleSearch();
                        }
                    }}
                    disabled={loading || searching}
                />

                <button
                    type="button"
                    className="search-button"
                    onClick={handleSearch}
                    disabled={loading || searching}
                    title="Buscar usuarios"
                >
                    {searching
                        ? "Buscando..."
                        : "Buscar"}
                </button>

                <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={handleClearSearch}
                    disabled={loading || searching}
                >
                    Limpiar
                </button>
            </div>

            {/* Error */}
            {errorMessage && (
                <div
                    className="error-message"
                    role="alert"
                >
                    {errorMessage}
                </div>
            )}

            <br />

            {/* Elementos por página */}
            <div>
                <label htmlFor="page-size">
                    Elementos por página:{" "}
                </label>

                <select
                    id="page-size"
                    value={pageSize}
                    onChange={(event) => {
                        setPageSize(
                            Number(
                                event.target.value
                            )
                        );

                        setCurrentPage(1);
                    }}
                    disabled={
                        loading || searching
                    }
                >
                    <option value="10">
                        10
                    </option>

                    <option value="25">
                        25
                    </option>

                    <option value="50">
                        50
                    </option>
                </select>
            </div>

            <br />

            {/* Indicador de carga */}
            {loading && (
                <p>
                    Cargando usuarios...
                </p>
            )}

            {/*
             * Sin resultados.
             */}
            {!loading &&
                !errorMessage &&
                users.length === 0 ? (
                <p
                    className="empty-results-message"
                    role="status"
                >
                    {hasSearched
                        ? "No se encontraron resultados para los criterios de búsqueda ingresados. Por favor, modifique los filtros e intente nuevamente."
                        : "No existen usuarios activos para mostrar."}
                </p>
            ) : (
                !loading && (
                    <div className="table-card">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Nombre</th>
                                    <th>
                                        Correo Electrónico
                                    </th>
                                    <th>Rol</th>
                                    <th>Usuario</th>
                                    <th>Estado</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>

                            <tbody>
                                {paginatedUsers.map(
                                    user => (
                                        <tr key={user.id}>
                                            <td>
                                                {user.id}
                                            </td>

                                            <td>
                                                {
                                                    user.fullName
                                                }
                                            </td>

                                            <td>
                                                {user.email}
                                            </td>

                                            <td>
                                                {user.role}
                                            </td>

                                            <td>
                                                {
                                                    user.username
                                                }
                                            </td>

                                            <td>
                                                <span className="status-active">
                                                    Activo
                                                </span>
                                            </td>

                                            <td>
                                                <div className="button-row">
                                                    <button
                                                        type="button"
                                                        className="btn btn-secondary"
                                                        onClick={() =>
                                                            onEdit(
                                                                user
                                                            )
                                                        }
                                                    >
                                                        Editar
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="btn btn-danger-outline"
                                                        onClick={() =>
                                                            onDelete(
                                                                user
                                                            )
                                                        }
                                                    >
                                                        Eliminar
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    )
                                )}
                            </tbody>
                        </table>
                    </div>
                )
            )}

            <br />

            {/* Paginación */}
            {!loading &&
                users.length > 0 && (
                    <div>
                        <p>
                            Mostrando{" "}
                            {paginatedUsers.length} de{" "}
                            {users.length} registros
                        </p>

                        <button
                            type="button"
                            disabled={
                                currentPage === 1
                            }
                            onClick={() =>
                                setCurrentPage(
                                    currentPage - 1
                                )
                            }
                        >
                            Anterior
                        </button>

                        <span>
                            {" "}
                            Página {currentPage} de{" "}
                            {totalPages || 1}{" "}
                        </span>

                        <button
                            type="button"
                            disabled={
                                currentPage >=
                                totalPages
                            }
                            onClick={() =>
                                setCurrentPage(
                                    currentPage + 1
                                )
                            }
                        >
                            Siguiente
                        </button>
                    </div>
                )}
        </div>
    );
}

export default UserList;