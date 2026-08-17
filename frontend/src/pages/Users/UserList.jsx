import { useEffect, useState } from "react";
import { getUsers } from "../../api/userApi";

import "./Users.css";
import "../../styles/tables.css";
import "../../styles/ui.css";

function UserList({ onEdit, onDelete }) {
    const [users, setUsers] = useState([]);
    const [filterField, setFilterField] = useState("username");
    const [searchText, setSearchText] = useState("");
    const [filteredUsers, setFilteredUsers] = useState([]);

    const [pageSize, setPageSize] = useState(10);
    const [currentPage, setCurrentPage] = useState(1);

    useEffect(() => {
        loadUsers();
    }, []);

    const loadUsers = async () => {
        try {
            const response = await getUsers();
            setUsers(response.data);
            setFilteredUsers(response.data);
        } catch (error) {
            console.error(error);
        }
    };

    const handleSearch = () => {
        if (searchText.length > 25) {
            alert("El campo de búsqueda no puede exceder los 25 caracteres.");
            return;
        }

        const text = searchText.trim().toLowerCase();

        if (!text) {
            setFilteredUsers(users);
            setCurrentPage(1);
            return;
        }

        const result = users.filter(user => {
            switch (filterField) {
                case "username":
                    return user.username
                        ?.toLowerCase()
                        .includes(text);

                case "name":
                    return user.fullName
                        ?.toLowerCase()
                        .includes(text);

                case "nit":
                    return user.nit
                        ?.toLowerCase()
                        .includes(text);

                case "role":
                    return user.role
                        ?.toLowerCase()
                        .includes(text);

                case "branch":
                    return user.branch
                        ?.toLowerCase()
                        .includes(text);

                default:
                    return false;
            }
        });

        setFilteredUsers(result);
        setCurrentPage(1);
    };

    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;

    const paginatedUsers = filteredUsers.slice(
        startIndex,
        endIndex
    );

    const totalPages = Math.ceil(
        filteredUsers.length / pageSize
    );

    return (
        <div>
            <h2>Listado de Usuarios</h2>

            <div className="search-bar">
                <select
                    value={filterField}
                    onChange={(e) =>
                        setFilterField(e.target.value)}
                >
                    <option value="username">Usuario</option>
                    <option value="name">Nombre</option>
                    <option value="nit">NIT</option>
                    <option value="role">Rol</option>
                    <option value="branch">Sucursal</option>
                </select>

                <input
                    type="text"
                    placeholder="Buscar..."
                    value={searchText}
                    onChange={(e) => setSearchText(e.target.value)}
                    onKeyDown={(e) => {
                        if (e.key === "Enter") {
                            handleSearch();
                        }
                    }}
                />

                <button
                    className="search-button"
                    onClick={handleSearch}
                >
                    🔍
                </button>
            </div>

            <br />

            <div>
                <label>Elementos por página: </label>

                <select
                    value={pageSize}
                    onChange={(e) => {
                        setPageSize(Number(e.target.value));
                        setCurrentPage(1);
                    }}
                >
                    <option value="10">10</option>
                    <option value="25">25</option>
                    <option value="50">50</option>
                </select>
            </div>

            <br />

            {filteredUsers.length === 0 ? (
                <p style={{ color: "red" }}>
                    No se encontraron resultados para los criterios de búsqueda ingresados. Por favor, modifique los filtros e intente nuevamente.
                </p>
            ) : (
                <div className="table-card">
                    <table className="data-table">
                        <thead>
                            <tr>

                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Correo Electrónico</th>
                                <th>Rol</th>
                                <th>Usuario</th>
                                <th>Estado</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>

                        <tbody>
                            {paginatedUsers.map(user => (
                                <tr key={user.id}>
                                    <td>{user.id}</td>
                                    <td>{user.fullName}</td>
                                    <td>{user.email}</td>
                                    <td>{user.role}</td>
                                    <td>{user.username}</td>
                                    <td>
                                        <span className={user.active ? "status-active" : "status-inactive"}>
                                            {user.active
                                                ? "Activo"
                                                : "Inactivo"}
                                        </span>
                                    </td>
                                    <td>
                                        <div className="button-row">
                                            <button
                                                className="btn btn-secondary"
                                                onClick={() => onEdit(user)}
                                            >
                                                Editar
                                            </button>

                                            <button
                                                className="btn btn-danger-outline"
                                                onClick={() => onDelete(user)}
                                            >
                                                Eliminar
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <br />

            <div>
                <p>
                    Mostrando {paginatedUsers.length} de{" "}
                    {filteredUsers.length} registros
                </p>

                <button
                    disabled={currentPage === 1}
                    onClick={() =>
                        setCurrentPage(currentPage - 1)}
                >
                    Anterior
                </button>

                <span>
                    {" "}
                    Página {currentPage} de {totalPages || 1}{" "}
                </span>

                <button
                    disabled={currentPage === totalPages}
                    onClick={() =>
                        setCurrentPage(currentPage + 1)}
                >
                    Siguiente
                </button>
            </div>
        </div>
    );
}

export default UserList;