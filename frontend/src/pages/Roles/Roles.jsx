import {
    useCallback,
    useEffect,
    useState
} from "react";

import MainLayout from "../../layouts/MainLayout";

import {
    changeRoleStatus,
    createRole,
    getRolesPage,
    updateRole
} from "../../api/roleApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

/**
 * Nombres técnicos utilizados directamente
 * por Spring Security y los módulos operativos.
 *
 * Estos nombres no pueden modificarse.
 */
const SYSTEM_ROLE_NAMES =
    new Set([
        "ADMIN",
        "PACIENTE",
        "RECEPCIONISTA",
        "CAJERO",
        "ENFERMERIA",
        "MEDICO",
        "LABORATORIO",
        "FARMACIA"
    ]);

function Roles() {
    const {
        hasPermission
    } = useAuth();

    /**
     * Permisos granulares.
     */
    const canCreate =
        hasPermission(
            "ROLE_CREATE"
        );

    const canUpdate =
        hasPermission(
            "ROLE_UPDATE"
        );

    const canChangeStatus =
        hasPermission(
            "ROLE_DELETE"
        );

    /**
     * Listado y carga.
     */
    const [
        roles,
        setRoles
    ] = useState([]);

    const [
        loading,
        setLoading
    ] = useState(false);

    const [
        saving,
        setSaving
    ] = useState(false);

    /**
     * Formulario.
     */
    const [
        name,
        setName
    ] = useState("");

    const [
        description,
        setDescription
    ] = useState("");

    const [
        editingRole,
        setEditingRole
    ] = useState(null);

    const [
        errors,
        setErrors
    ] = useState({});

    /**
     * Filtros.
     */
    const [
        nameFilter,
        setNameFilter
    ] = useState("");

    const [
        activeFilter,
        setActiveFilter
    ] = useState("");

    /**
     * Paginación.
     */
    const [
        page,
        setPage
    ] = useState(0);

    const [
        size,
        setSize
    ] = useState(10);

    const [
        totalElements,
        setTotalElements
    ] = useState(0);

    const [
        totalPages,
        setTotalPages
    ] = useState(0);

    const [
        first,
        setFirst
    ] = useState(true);

    const [
        last,
        setLast
    ] = useState(true);

    /**
     * Consulta el listado administrativo.
     */
    const loadRoles =
        useCallback(async () => {
            try {
                setLoading(true);

                const response =
                    await getRolesPage({
                        name:
                            nameFilter,
                        active:
                            activeFilter,
                        page,
                        size
                    });

                const pageData =
                    response.data;

                setRoles(
                    Array.isArray(
                        pageData?.content
                    )
                        ? pageData.content
                        : []
                );

                setTotalElements(
                    pageData
                        ?.totalElements ?? 0
                );

                setTotalPages(
                    pageData
                        ?.totalPages ?? 0
                );

                setFirst(
                    pageData?.first ?? true
                );

                setLast(
                    pageData?.last ?? true
                );

            } catch (error) {
                console.error(
                    "Error al cargar roles:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "Error al cargar los roles."
                );

            } finally {
                setLoading(false);
            }
        }, [
            nameFilter,
            activeFilter,
            page,
            size
        ]);

    useEffect(() => {
        loadRoles();
    }, [loadRoles]);

    /**
     * Determina si el nombre técnico está
     * protegido por el sistema.
     */
    const isSystemRole = role => {
        if (
            role?.systemRole !==
            undefined
        ) {
            return Boolean(
                role.systemRole
            );
        }

        return SYSTEM_ROLE_NAMES.has(
            normalizeRoleName(
                role?.name
            )
        );
    };

    /**
     * Validar formulario.
     */
    const validateForm = () => {
        const newErrors = {};

        const cleanName =
            name.trim();

        const cleanDescription =
            description.trim();

        if (!cleanName) {
            newErrors.name =
                "El nombre del rol es obligatorio.";

        } else if (
            cleanName.length > 50
        ) {
            newErrors.name =
                "El nombre no puede exceder los 50 caracteres.";

        } else if (
            !/^[A-Za-zÁÉÍÓÚáéíóúÑñ_ ]+$/
                .test(cleanName)
        ) {
            newErrors.name =
                "El nombre solamente puede contener letras, espacios y guion bajo.";
        }

        if (!cleanDescription) {
            newErrors.description =
                "La descripción del rol es obligatoria.";

        } else if (
            cleanDescription.length > 250
        ) {
            newErrors.description =
                "La descripción no puede exceder los 250 caracteres.";
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors)
                .length === 0
        );
    };

    /**
     * Limpia el formulario.
     */
    const clearForm = () => {
        setName("");
        setDescription("");
        setEditingRole(null);
        setErrors({});
    };

    /**
     * Crea o actualiza un rol.
     */
    const handleSubmit =
        async event => {
            event.preventDefault();

            if (!validateForm()) {
                return;
            }

            try {
                setSaving(true);

                if (editingRole) {
                    if (!canUpdate) {
                        alert(
                            "No tiene permiso para actualizar roles."
                        );

                        return;
                    }

                    const response =
                        await updateRole(
                            editingRole.id,
                            {
                                name:
                                    name.trim(),
                                description:
                                    description.trim(),
                                version:
                                    editingRole
                                        .version
                            }
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Rol actualizado correctamente."
                    );

                } else {
                    if (!canCreate) {
                        alert(
                            "No tiene permiso para crear roles."
                        );

                        return;
                    }

                    const response =
                        await createRole({
                            name:
                                name.trim(),
                            description:
                                description.trim()
                        });

                    alert(
                        response.data
                            ?.message ||
                        "Rol creado correctamente."
                    );
                }

                clearForm();

                await loadRoles();

            } catch (error) {
                console.error(
                    "Error al guardar rol:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "Error al guardar el rol."
                );

                if (
                    error.response
                        ?.status === 409
                ) {
                    clearForm();

                    await loadRoles();
                }

            } finally {
                setSaving(false);
            }
        };

    /**
     * Carga los datos de un rol en el formulario.
     */
    const handleEdit = role => {
        if (!canUpdate) {
            return;
        }

        setEditingRole(role);

        setName(
            role.name || ""
        );

        setDescription(
            role.description || ""
        );

        setErrors({});

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    /**
 * Activa o desactiva un rol.
 *
 * Reglas importantes:
 *
 * - ADMIN no puede desactivarse.
 * - Un rol con usuarios activos no puede
 *   desactivarse.
 * - La versión actual es obligatoria.
 */
    const handleStatusChange =
        async role => {
            if (!canChangeStatus) {
                alert(
                    "No tiene permiso para cambiar el estado de los roles."
                );

                return;
            }

            const newStatus =
                !role.active;

            const actionText =
                newStatus
                    ? "reactivar"
                    : "desactivar";

            const normalizedRoleName =
                normalizeRoleName(
                    role.name
                );

            /*
             * Protección visual del rol ADMIN.
             *
             * El backend también aplica esta regla.
             */
            if (
                !newStatus &&
                normalizedRoleName === "ADMIN"
            ) {
                alert(
                    "El rol ADMIN no puede desactivarse."
                );

                return;
            }

            /*
             * Un rol con usuarios activos no puede
             * desactivarse.
             */
            if (
                !newStatus &&
                Number(
                    role.activeUserCount || 0
                ) > 0
            ) {
                alert(
                    "El rol no puede desactivarse porque tiene usuarios activos asignados."
                );

                return;
            }

            const confirmed =
                window.confirm(
                    `¿Desea ${actionText} el rol "${role.name}"?`
                );

            if (!confirmed) {
                return;
            }

            try {
                setSaving(true);

                const response =
                    await changeRoleStatus(
                        role.id,
                        newStatus,
                        role.version
                    );

                alert(
                    response.data
                        ?.message ||
                    (
                        newStatus
                            ? "Rol reactivado correctamente."
                            : "Rol desactivado correctamente."
                    )
                );

                /*
                 * Si el rol modificado se encuentra
                 * actualmente en edición, limpiar
                 * el formulario.
                 */
                if (
                    editingRole?.id ===
                    role.id
                ) {
                    clearForm();
                }

                await loadRoles();

            } catch (error) {
                console.error(
                    "Error al cambiar el estado del rol:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cambiar el estado del rol."
                );

                /*
                 * Recargar para obtener la versión
                 * y el estado más recientes.
                 */
                await loadRoles();

            } finally {
                setSaving(false);
            }
        };

    /**
     * Aplica los filtros.
     *
     * Si el usuario se encuentra en otra página,
     * regresa a la página cero. El useEffect
     * cargará el nuevo resultado.
     */
    const handleSearch =
        event => {
            event.preventDefault();

            if (page === 0) {
                loadRoles();

            } else {
                setPage(0);
            }
        };

    /**
     * Limpia los filtros de búsqueda.
     */
    const clearFilters = () => {
        setNameFilter("");
        setActiveFilter("");
        setPage(0);
    };

    /**
     * Determina si el rol que se está editando
     * es un rol técnico protegido.
     */
    const editingSystemRole =
        editingRole
            ? isSystemRole(
                editingRole
            )
            : false;

    return (
        <MainLayout>
            <h1 className="page-title">
                Roles
            </h1>

            {/*
             * Formulario de creación o edición.
             *
             * Se muestra cuando:
             *
             * - El usuario posee ROLE_CREATE.
             * - O está editando y posee ROLE_UPDATE.
             */}
            {(canCreate ||
                (
                    editingRole &&
                    canUpdate
                )) && (
                    <div className="card">
                        <h2 className="section-title">
                            {editingRole
                                ? "Editar Rol"
                                : "Crear Rol"}
                        </h2>

                        <form
                            onSubmit={
                                handleSubmit
                            }
                        >
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>
                                        Nombre técnico
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={50}
                                        placeholder="Ejemplo: SOPORTE_TECNICO"
                                        value={name}
                                        disabled={
                                            editingSystemRole
                                        }
                                        className={
                                            errors.name
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setName(
                                                    event
                                                        .target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.name && (
                                        <div className="error-message">
                                            {errors.name}
                                        </div>
                                    )}

                                    {editingSystemRole ? (
                                        <small>
                                            El nombre de este rol técnico está protegido porque es utilizado por Spring Security.
                                        </small>
                                    ) : (
                                        <small>
                                            Los espacios serán convertidos a guion bajo y el nombre se almacenará en mayúsculas.
                                        </small>
                                    )}

                                    <small>
                                        {name.length}/50
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Descripción
                                    </label>

                                    <textarea
                                        rows={4}
                                        maxLength={250}
                                        placeholder="Descripción funcional del rol"
                                        value={
                                            description
                                        }
                                        className={
                                            errors.description
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setDescription(
                                                    event
                                                        .target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.description && (
                                        <div className="error-message">
                                            {
                                                errors.description
                                            }
                                        </div>
                                    )}

                                    <small>
                                        {description.length}/250
                                    </small>
                                </div>
                            </div>

                            {editingRole && (
                                <div
                                    style={{
                                        marginTop: "12px",
                                        padding: "12px",
                                        borderRadius: "6px",
                                        backgroundColor:
                                            "#f8fafc",
                                        color: "#475569"
                                    }}
                                >
                                    <strong>
                                        Registro en edición:
                                    </strong>

                                    {" "}

                                    ID {editingRole.id}

                                    {" | "}

                                    Versión {
                                        editingRole.version
                                    }

                                    {" | "}

                                    Tipo:
                                    {" "}

                                    {editingSystemRole
                                        ? "Rol técnico protegido"
                                        : "Rol personalizado"}
                                </div>
                            )}

                            <div className="button-row">
                                <button
                                    type="submit"
                                    className="btn btn-primary"
                                    disabled={saving}
                                >
                                    {saving
                                        ? "Guardando..."
                                        : (
                                            editingRole
                                                ? "Actualizar"
                                                : "Crear"
                                        )}
                                </button>

                                {editingRole && (
                                    <button
                                        type="button"
                                        className="btn btn-secondary"
                                        onClick={
                                            clearForm
                                        }
                                        disabled={saving}
                                    >
                                        Cancelar
                                    </button>
                                )}
                            </div>
                        </form>
                    </div>
                )}

            <br />

            <div className="table-card">
                <h2 className="section-title">
                    Listado de Roles
                </h2>

                {/*
                 * Filtros administrativos.
                 */}
                <form
                    onSubmit={
                        handleSearch
                    }
                >
                    <div className="form-grid">
                        <div className="form-group">
                            <label>
                                Buscar por nombre
                            </label>

                            <input
                                type="text"
                                maxLength={50}
                                placeholder="Ejemplo: FARMACIA"
                                value={
                                    nameFilter
                                }
                                onChange={
                                    event =>
                                        setNameFilter(
                                            event
                                                .target
                                                .value
                                        )
                                }
                            />
                        </div>

                        <div className="form-group">
                            <label>
                                Estado
                            </label>

                            <select
                                value={
                                    activeFilter
                                }
                                onChange={
                                    event =>
                                        setActiveFilter(
                                            event
                                                .target
                                                .value
                                        )
                                }
                            >
                                <option value="">
                                    Todos
                                </option>

                                <option value="true">
                                    Activos
                                </option>

                                <option value="false">
                                    Inactivos
                                </option>
                            </select>
                        </div>

                        <div className="form-group">
                            <label>
                                Registros por página
                            </label>

                            <select
                                value={size}
                                onChange={
                                    event => {
                                        setSize(
                                            Number(
                                                event
                                                    .target
                                                    .value
                                            )
                                        );

                                        setPage(0);
                                    }
                                }
                            >
                                <option value={5}>
                                    5 registros
                                </option>

                                <option value={10}>
                                    10 registros
                                </option>

                                <option value={20}>
                                    20 registros
                                </option>

                                <option value={50}>
                                    50 registros
                                </option>
                            </select>
                        </div>
                    </div>

                    <div className="button-row">
                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={loading}
                        >
                            {loading
                                ? "Buscando..."
                                : "Buscar"}
                        </button>

                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={
                                clearFilters
                            }
                            disabled={loading}
                        >
                            Limpiar filtros
                        </button>
                    </div>
                </form>

                <br />

                {loading ? (
                    <p>
                        Cargando roles...
                    </p>

                ) : roles.length === 0 ? (
                    <p
                        style={{
                            color: "#64748b"
                        }}
                    >
                        No hay roles que coincidan con los filtros seleccionados.
                    </p>

                ) : (
                    <div
                        style={{
                            overflowX: "auto"
                        }}
                    >
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>
                                        ID
                                    </th>

                                    <th>
                                        Nombre
                                    </th>

                                    <th>
                                        Descripción
                                    </th>

                                    <th>
                                        Tipo
                                    </th>

                                    <th>
                                        Usuarios asignados
                                    </th>

                                    <th>
                                        Usuarios activos
                                    </th>

                                    <th>
                                        Estado
                                    </th>

                                    <th>
                                        Última modificación
                                    </th>

                                    <th>
                                        Modificado por
                                    </th>

                                    <th>
                                        Versión
                                    </th>

                                    {(canUpdate ||
                                        canChangeStatus) && (
                                            <th>
                                                Acciones
                                            </th>
                                        )}
                                </tr>
                            </thead>

                            <tbody>
                                {roles.map(
                                    role => {
                                        const protectedRole =
                                            isSystemRole(
                                                role
                                            );

                                        const isAdminRole =
                                            normalizeRoleName(
                                                role.name
                                            ) === "ADMIN";

                                        const hasActiveUsers =
                                            Number(
                                                role.activeUserCount ||
                                                0
                                            ) > 0;

                                        /*
                                         * Desactivar queda bloqueado
                                         * si es ADMIN o posee usuarios activos.
                                         *
                                         * Reactivar sigue permitido.
                                         */
                                        const statusActionDisabled =
                                            role.active &&
                                            (
                                                isAdminRole ||
                                                hasActiveUsers
                                            );

                                        let statusDisabledTitle =
                                            "";

                                        if (
                                            role.active &&
                                            isAdminRole
                                        ) {
                                            statusDisabledTitle =
                                                "El rol ADMIN no puede desactivarse.";

                                        } else if (
                                            role.active &&
                                            hasActiveUsers
                                        ) {
                                            statusDisabledTitle =
                                                "El rol tiene usuarios activos asignados.";
                                        }

                                        return (
                                            <tr
                                                key={
                                                    role.id
                                                }
                                            >
                                                <td>
                                                    {
                                                        role.id
                                                    }
                                                </td>

                                                <td>
                                                    <strong>
                                                        {
                                                            role.name
                                                        }
                                                    </strong>
                                                </td>

                                                <td>
                                                    {
                                                        role.description
                                                    }
                                                </td>

                                                <td>
                                                    {protectedRole
                                                        ? "Sistema"
                                                        : "Personalizado"}
                                                </td>

                                                <td>
                                                    {
                                                        role.assignedUserCount ??
                                                        0
                                                    }
                                                </td>

                                                <td>
                                                    {
                                                        role.activeUserCount ??
                                                        0
                                                    }
                                                </td>

                                                <td>
                                                    <span
                                                        className={
                                                            role.active
                                                                ? "status-active"
                                                                : "status-inactive"
                                                        }
                                                    >
                                                        {role.active
                                                            ? "Activo"
                                                            : "Inactivo"}
                                                    </span>
                                                </td>

                                                <td>
                                                    {
                                                        formatDateTime(
                                                            role.updatedAt
                                                        )
                                                    }
                                                </td>

                                                <td>
                                                    {
                                                        role.updatedBy ||
                                                        "Sin información"
                                                    }
                                                </td>

                                                <td>
                                                    {
                                                        role.version
                                                    }
                                                </td>

                                                {(canUpdate ||
                                                    canChangeStatus) && (
                                                        <td>
                                                            <div className="button-row">
                                                                {canUpdate && (
                                                                    <button
                                                                        type="button"
                                                                        className="btn btn-secondary"
                                                                        onClick={() =>
                                                                            handleEdit(
                                                                                role
                                                                            )
                                                                        }
                                                                        disabled={
                                                                            saving
                                                                        }
                                                                    >
                                                                        Editar
                                                                    </button>
                                                                )}

                                                                {canChangeStatus && (
                                                                    <button
                                                                        type="button"
                                                                        className={
                                                                            role.active
                                                                                ? "btn btn-danger-outline"
                                                                                : "btn btn-primary"
                                                                        }
                                                                        onClick={() =>
                                                                            handleStatusChange(
                                                                                role
                                                                            )
                                                                        }
                                                                        disabled={
                                                                            saving ||
                                                                            statusActionDisabled
                                                                        }
                                                                        title={
                                                                            statusDisabledTitle
                                                                        }
                                                                    >
                                                                        {role.active
                                                                            ? "Desactivar"
                                                                            : "Reactivar"}
                                                                    </button>
                                                                )}
                                                            </div>
                                                        </td>
                                                    )}
                                            </tr>
                                        );
                                    }
                                )}
                            </tbody>
                        </table>
                    </div>
                )}

                {/*
                 * Controles de paginación.
                 */}
                <div
                    style={{
                        marginTop: "18px",
                        display: "flex",
                        alignItems: "center",
                        justifyContent:
                            "space-between",
                        gap: "12px",
                        flexWrap: "wrap"
                    }}
                >
                    <div
                        style={{
                            color: "#475569"
                        }}
                    >
                        Total:
                        {" "}
                        <strong>
                            {totalElements}
                        </strong>
                        {" "}
                        registro(s)
                    </div>

                    <div className="button-row">
                        <button
                            type="button"
                            className="btn btn-secondary"
                            disabled={
                                first ||
                                loading
                            }
                            onClick={() =>
                                setPage(
                                    currentPage =>
                                        Math.max(
                                            currentPage - 1,
                                            0
                                        )
                                )
                            }
                        >
                            Anterior
                        </button>

                        <span
                            style={{
                                alignSelf: "center",
                                color: "#334155"
                            }}
                        >
                            Página
                            {" "}
                            <strong>
                                {totalPages === 0
                                    ? 0
                                    : page + 1}
                            </strong>
                            {" "}
                            de
                            {" "}
                            <strong>
                                {totalPages}
                            </strong>
                        </span>

                        <button
                            type="button"
                            className="btn btn-secondary"
                            disabled={
                                last ||
                                loading ||
                                totalPages === 0
                            }
                            onClick={() =>
                                setPage(
                                    currentPage =>
                                        currentPage + 1
                                )
                            }
                        >
                            Siguiente
                        </button>
                    </div>
                </div>
            </div>
        </MainLayout>
    );
}

/**
 * Normaliza el nombre técnico de un rol.
 *
 * Ejemplos:
 *
 * Médico -> MEDICO
 * Soporte técnico -> SOPORTE_TECNICO
 */
function normalizeRoleName(value) {
    if (!value) {
        return "";
    }

    return String(value)
        .normalize("NFD")
        .replace(
            /[\u0300-\u036f]/g,
            ""
        )
        .trim()
        .replace(
            /\s+/g,
            "_"
        )
        .toUpperCase();
}

/**
 * Convierte una fecha ISO del backend
 * a una fecha legible.
 */
function formatDateTime(value) {
    if (!value) {
        return "Sin información";
    }

    const parsedDate =
        new Date(value);

    if (
        Number.isNaN(
            parsedDate.getTime()
        )
    ) {
        return value;
    }

    return parsedDate
        .toLocaleString(
            "es-GT",
            {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit"
            }
        );
}

export default Roles;