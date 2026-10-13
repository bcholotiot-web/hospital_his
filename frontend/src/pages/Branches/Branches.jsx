import {
    useCallback,
    useEffect,
    useState
} from "react";

import MainLayout from "../../layouts/MainLayout";

import {
    changeBranchStatus,
    createBranch,
    getBranchesPage,
    updateBranch
} from "../../api/branchApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

function Branches() {
    const {
        hasPermission
    } = useAuth();

    /**
     * Permisos granulares.
     */
    const canCreate =
        hasPermission(
            "BRANCH_CREATE"
        );

    const canUpdate =
        hasPermission(
            "BRANCH_UPDATE"
        );

    const canChangeStatus =
        hasPermission(
            "BRANCH_DELETE"
        );

    /**
     * Listado y estados de carga.
     */
    const [
        branches,
        setBranches
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
     * Campos del formulario.
     */
    const [
        name,
        setName
    ] = useState("");

    const [
        phone,
        setPhone
    ] = useState("");

    const [
        address,
        setAddress
    ] = useState("");

    const [
        description,
        setDescription
    ] = useState("");

    const [
        editingBranch,
        setEditingBranch
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
     * Carga el listado administrativo
     * desde el backend.
     */
    const loadBranches =
        useCallback(async () => {
            try {
                setLoading(true);

                const response =
                    await getBranchesPage({
                        name:
                            nameFilter,
                        active:
                            activeFilter,
                        page,
                        size
                    });

                const pageData =
                    response.data;

                setBranches(
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
                    "Error al cargar sucursales:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "Error al cargar las sucursales."
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
        loadBranches();
    }, [loadBranches]);

    /**
     * Valida la información antes de enviar
     * la solicitud.
     */
    const validateForm = () => {
        const newErrors = {};

        const cleanName =
            name.trim();

        const cleanPhone =
            phone.trim();

        const cleanAddress =
            address.trim();

        const cleanDescription =
            description.trim();

        if (!cleanName) {
            newErrors.name =
                "El nombre de la sucursal es obligatorio.";

        } else if (
            cleanName.length > 100
        ) {
            newErrors.name =
                "El nombre no puede exceder los 100 caracteres.";
        }

        if (
            cleanPhone &&
            !/^[0-9]{8}$/.test(
                cleanPhone
            )
        ) {
            newErrors.phone =
                "El teléfono debe contener exactamente 8 dígitos.";
        }

        if (
            cleanAddress.length > 500
        ) {
            newErrors.address =
                "La dirección no puede exceder los 500 caracteres.";
        }

        if (
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
        setPhone("");
        setAddress("");
        setDescription("");
        setEditingBranch(null);
        setErrors({});
    };

    /**
     * Crea o actualiza una sucursal.
     */
    const handleSubmit =
        async event => {
            event.preventDefault();

            if (!validateForm()) {
                return;
            }

            try {
                setSaving(true);

                const normalizedData = {
                    name:
                        name.trim(),
                    phone:
                        phone.trim() || null,
                    address:
                        address.trim() || null,
                    description:
                        description.trim() || null
                };

                if (editingBranch) {
                    if (!canUpdate) {
                        alert(
                            "No tiene permiso para actualizar sucursales."
                        );

                        return;
                    }

                    const response =
                        await updateBranch(
                            editingBranch.id,
                            {
                                ...normalizedData,
                                version:
                                    editingBranch
                                        .version
                            }
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Sucursal actualizada correctamente."
                    );

                } else {
                    if (!canCreate) {
                        alert(
                            "No tiene permiso para crear sucursales."
                        );

                        return;
                    }

                    const response =
                        await createBranch(
                            normalizedData
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Sucursal creada correctamente."
                    );
                }

                clearForm();

                await loadBranches();

            } catch (error) {
                console.error(
                    "Error al guardar sucursal:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "Error al guardar la sucursal."
                );

                /**
                 * Recargar en caso de conflicto
                 * de concurrencia.
                 */
                if (
                    error.response
                        ?.status === 409
                ) {
                    clearForm();

                    await loadBranches();
                }

            } finally {
                setSaving(false);
            }
        };

    /**
     * Carga una sucursal dentro del formulario
     * para editarla.
     */
    const handleEdit = branch => {
        if (!canUpdate) {
            return;
        }

        setEditingBranch(
            branch
        );

        setName(
            branch.name || ""
        );

        setPhone(
            branch.phone || ""
        );

        setAddress(
            branch.address || ""
        );

        setDescription(
            branch.description || ""
        );

        setErrors({});

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    /**
     * Activa o desactiva una sucursal.
     */
    const handleStatusChange =
        async branch => {
            if (!canChangeStatus) {
                return;
            }

            const newStatus =
                !branch.active;

            const actionText =
                newStatus
                    ? "reactivar"
                    : "desactivar";

            const confirmed =
                window.confirm(
                    `¿Desea ${actionText} la sucursal "${branch.name}"?`
                );

            if (!confirmed) {
                return;
            }

            try {
                const response =
                    await changeBranchStatus(
                        branch.id,
                        newStatus,
                        branch.version
                    );

                alert(
                    response.data
                        ?.message ||
                    (
                        newStatus
                            ? "Sucursal reactivada correctamente."
                            : "Sucursal desactivada correctamente."
                    )
                );

                if (
                    editingBranch?.id ===
                    branch.id
                ) {
                    clearForm();
                }

                await loadBranches();

            } catch (error) {
                console.error(
                    "Error al cambiar estado:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cambiar el estado de la sucursal."
                );

                await loadBranches();
            }
        };

    /**
     * Aplica filtros desde la primera página.
     */
    const handleSearch =
        event => {
            event.preventDefault();

            if (page === 0) {
                loadBranches();
            } else {
                setPage(0);
            }
        };

    /**
     * Limpia los filtros.
     */
    const clearFilters = () => {
        setNameFilter("");
        setActiveFilter("");
        setPage(0);
    };

    return (
        <MainLayout>
            <h1 className="page-title">
                Sucursales
            </h1>

            {/*
             * Formulario de creación y edición.
             */}
            {(canCreate ||
                (
                    editingBranch &&
                    canUpdate
                )) && (
                    <div className="card">
                        <h2 className="section-title">
                            {editingBranch
                                ? "Editar Sucursal"
                                : "Crear Sucursal"}
                        </h2>

                        <form
                            onSubmit={
                                handleSubmit
                            }
                        >
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>
                                        Nombre
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={100}
                                        placeholder="Nombre de la sucursal"
                                        value={name}
                                        className={
                                            errors.name
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setName(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.name && (
                                        <div className="error-message">
                                            {errors.name}
                                        </div>
                                    )}

                                    <small>
                                        {name.length}/100
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Teléfono
                                    </label>

                                    <input
                                        type="text"
                                        inputMode="numeric"
                                        maxLength={8}
                                        placeholder="Ejemplo: 22334455"
                                        value={phone}
                                        className={
                                            errors.phone
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event => {
                                                const value =
                                                    event.target
                                                        .value
                                                        .replace(
                                                            /\D/g,
                                                            ""
                                                        );

                                                setPhone(value);
                                            }
                                        }
                                    />

                                    {errors.phone && (
                                        <div className="error-message">
                                            {errors.phone}
                                        </div>
                                    )}

                                    <small>
                                        Campo opcional. Debe contener ocho dígitos.
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Dirección
                                    </label>

                                    <textarea
                                        rows={3}
                                        maxLength={500}
                                        placeholder="Dirección física de la sucursal"
                                        value={address}
                                        className={
                                            errors.address
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setAddress(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.address && (
                                        <div className="error-message">
                                            {errors.address}
                                        </div>
                                    )}

                                    <small>
                                        {address.length}/500
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Descripción
                                    </label>

                                    <textarea
                                        rows={3}
                                        maxLength={250}
                                        placeholder="Descripción administrativa de la sucursal"
                                        value={description}
                                        className={
                                            errors.description
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setDescription(
                                                    event.target
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

                            {editingBranch && (
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

                                    ID {editingBranch.id}

                                    {" | "}

                                    Versión {
                                        editingBranch.version
                                    }
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
                                            editingBranch
                                                ? "Actualizar"
                                                : "Crear"
                                        )}
                                </button>

                                {editingBranch && (
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
                    Listado de Sucursales
                </h2>

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
                                maxLength={100}
                                placeholder="Ejemplo: Zona 10"
                                value={
                                    nameFilter
                                }
                                onChange={
                                    event =>
                                        setNameFilter(
                                            event.target
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
                                            event.target
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
                                                event.target
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
                        Cargando sucursales...
                    </p>

                ) : branches.length === 0 ? (
                    <p
                        style={{
                            color: "#64748b"
                        }}
                    >
                        No hay sucursales que coincidan con los filtros seleccionados.
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
                                        Teléfono
                                    </th>

                                    <th>
                                        Dirección
                                    </th>

                                    <th>
                                        Descripción
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
                                {branches.map(
                                    branch => (
                                        <tr
                                            key={
                                                branch.id
                                            }
                                        >
                                            <td>
                                                {
                                                    branch.id
                                                }
                                            </td>

                                            <td>
                                                {
                                                    branch.name
                                                }
                                            </td>

                                            <td>
                                                {
                                                    branch.phone ||
                                                    "No indicado"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    branch.address ||
                                                    "No indicada"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    branch.description ||
                                                    "No indicada"
                                                }
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        branch.active
                                                            ? "status-active"
                                                            : "status-inactive"
                                                    }
                                                >
                                                    {branch.active
                                                        ? "Activo"
                                                        : "Inactivo"}
                                                </span>
                                            </td>

                                            <td>
                                                {
                                                    formatDateTime(
                                                        branch.updatedAt
                                                    )
                                                }
                                            </td>

                                            <td>
                                                {
                                                    branch.updatedBy ||
                                                    "Sin información"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    branch.version
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
                                                                            branch
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
                                                                        branch.active
                                                                            ? "btn btn-danger-outline"
                                                                            : "btn btn-primary"
                                                                    }
                                                                    onClick={() =>
                                                                        handleStatusChange(
                                                                            branch
                                                                        )
                                                                    }
                                                                    disabled={
                                                                        saving
                                                                    }
                                                                >
                                                                    {branch.active
                                                                        ? "Desactivar"
                                                                        : "Reactivar"}
                                                                </button>
                                                            )}
                                                        </div>
                                                    </td>
                                                )}
                                        </tr>
                                    )
                                )}
                            </tbody>
                        </table>
                    </div>
                )}

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

                        <span>
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
 * Convierte fechas ISO del backend
 * a un formato legible.
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

export default Branches;