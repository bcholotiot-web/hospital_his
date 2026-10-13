import {
    useCallback,
    useEffect,
    useState
} from "react";

import MainLayout from "../../layouts/MainLayout";

import {
    createSpecialty,
    getSpecialtiesPage,
    updateSpecialty,
    changeSpecialtyStatus
} from "../../api/specialtyApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

function Specialties() {
    const {
        hasPermission
    } = useAuth();

    /*
     * Permisos efectivos.
     */
    const canCreate =
        hasPermission(
            "SPECIALTY_CREATE"
        );

    const canUpdate =
        hasPermission(
            "SPECIALTY_UPDATE"
        );

    const canChangeStatus =
        hasPermission(
            "SPECIALTY_DELETE"
        );

    /*
     * Listado y carga.
     */
    const [specialties, setSpecialties] =
        useState([]);

    const [loading, setLoading] =
        useState(false);

    const [saving, setSaving] =
        useState(false);

    /*
     * Formulario.
     */
    const [name, setName] =
        useState("");

    const [description, setDescription] =
        useState("");

    const [
        editingSpecialty,
        setEditingSpecialty
    ] = useState(null);

    const [errors, setErrors] =
        useState({});

    /*
     * Filtros.
     */
    const [nameFilter, setNameFilter] =
        useState("");

    const [activeFilter, setActiveFilter] =
        useState("");

    /*
     * Paginación.
     */
    const [page, setPage] =
        useState(0);

    const [size, setSize] =
        useState(10);

    const [
        totalElements,
        setTotalElements
    ] = useState(0);

    const [
        totalPages,
        setTotalPages
    ] = useState(0);

    const [first, setFirst] =
        useState(true);

    const [last, setLast] =
        useState(true);

    /**
     * Carga el listado administrativo.
     */
    const loadSpecialties =
        useCallback(async () => {
            try {
                setLoading(true);

                const response =
                    await getSpecialtiesPage({
                        name:
                            nameFilter,
                        active:
                            activeFilter,
                        page,
                        size
                    });

                const pageData =
                    response.data;

                setSpecialties(
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
                console.error(error);

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "Error al cargar especialidades."
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
        loadSpecialties();
    }, [loadSpecialties]);

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
                "El nombre de la especialidad es obligatorio.";

        } else if (
            cleanName.length > 100
        ) {
            newErrors.name =
                "El nombre no puede exceder los 100 caracteres.";
        }

        if (!cleanDescription) {
            newErrors.description =
                "La descripción de la especialidad es obligatoria.";

        } else if (
            cleanDescription.length > 500
        ) {
            newErrors.description =
                "La descripción no puede exceder los 500 caracteres.";
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors)
                .length === 0
        );
    };

    /**
     * Limpiar formulario.
     */
    const clearForm = () => {
        setName("");
        setDescription("");
        setEditingSpecialty(null);
        setErrors({});
    };

    /**
     * Crear o actualizar.
     */
    const handleSubmit =
        async event => {
            event.preventDefault();

            if (!validateForm()) {
                return;
            }

            try {
                setSaving(true);

                if (editingSpecialty) {
                    if (!canUpdate) {
                        alert(
                            "No tiene permiso para actualizar especialidades."
                        );

                        return;
                    }

                    const response =
                        await updateSpecialty(
                            editingSpecialty.id,
                            {
                                name:
                                    name.trim(),
                                description:
                                    description.trim(),
                                version:
                                    editingSpecialty
                                        .version
                            }
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Especialidad actualizada correctamente."
                    );

                } else {
                    if (!canCreate) {
                        alert(
                            "No tiene permiso para crear especialidades."
                        );

                        return;
                    }

                    const response =
                        await createSpecialty({
                            name:
                                name.trim(),
                            description:
                                description.trim()
                        });

                    alert(
                        response.data
                            ?.message ||
                        "Especialidad creada correctamente."
                    );
                }

                clearForm();
                await loadSpecialties();

            } catch (error) {
                console.error(error);

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "Error al guardar la especialidad."
                );

                /*
                 * Si existe conflicto de versión,
                 * recargar el listado.
                 */
                if (
                    error.response
                        ?.status === 409
                ) {
                    clearForm();
                    await loadSpecialties();
                }

            } finally {
                setSaving(false);
            }
        };

    /**
     * Cargar datos en el formulario.
     */
    const handleEdit = specialty => {
        if (!canUpdate) {
            return;
        }

        setEditingSpecialty(
            specialty
        );

        setName(
            specialty.name || ""
        );

        setDescription(
            specialty.description || ""
        );

        setErrors({});

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    /**
     * Activar o desactivar.
     */
    const handleStatusChange =
        async specialty => {
            if (!canChangeStatus) {
                return;
            }

            const newStatus =
                !specialty.active;

            const actionText =
                newStatus
                    ? "reactivar"
                    : "desactivar";

            const confirmed =
                window.confirm(
                    `¿Desea ${actionText} la especialidad "${specialty.name}"?`
                );

            if (!confirmed) {
                return;
            }

            try {
                const response =
                    await changeSpecialtyStatus(
                        specialty.id,
                        newStatus,
                        specialty.version
                    );

                alert(
                    response.data
                        ?.message ||
                    (
                        newStatus
                            ? "Especialidad reactivada correctamente."
                            : "Especialidad desactivada correctamente."
                    )
                );

                if (
                    editingSpecialty?.id ===
                    specialty.id
                ) {
                    clearForm();
                }

                await loadSpecialties();

            } catch (error) {
                console.error(error);

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cambiar el estado de la especialidad."
                );

                await loadSpecialties();
            }
        };

    /**
     * Aplicar filtros desde la primera página.
     */
    const handleSearch =
        event => {
            event.preventDefault();

            if (page === 0) {
                loadSpecialties();
            } else {
                setPage(0);
            }
        };

    /**
     * Limpiar filtros.
     */
    const clearFilters = () => {
        setNameFilter("");
        setActiveFilter("");
        setPage(0);
    };

    return (
        <MainLayout>
            <h1 className="page-title">
                Especialidades
            </h1>

            {/*
             * El formulario solamente se muestra cuando:
             *
             * - El usuario puede crear registros.
             * - O está editando y posee permiso de actualización.
             */}
            {(canCreate ||
                (
                    editingSpecialty &&
                    canUpdate
                )) && (
                    <div className="card">
                        <h2 className="section-title">
                            {editingSpecialty
                                ? "Editar Especialidad"
                                : "Crear Especialidad"}
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
                                        placeholder="Nombre de la especialidad"
                                        value={name}
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

                                    <small>
                                        {name.length}/100
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Descripción
                                    </label>

                                    <textarea
                                        maxLength={500}
                                        rows={4}
                                        placeholder="Descripción de la especialidad"
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
                                        {description.length}/500
                                    </small>
                                </div>
                            </div>

                            {editingSpecialty && (
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

                                    ID {editingSpecialty.id}

                                    {" | "}

                                    Versión {
                                        editingSpecialty.version
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
                                            editingSpecialty
                                                ? "Actualizar"
                                                : "Crear"
                                        )}
                                </button>

                                {editingSpecialty && (
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
                    Listado de Especialidades
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
                                maxLength={100}
                                placeholder="Ejemplo: Cardiología"
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

                {/*
                 * Estado de carga del listado.
                 */}
                {loading ? (
                    <p>
                        Cargando especialidades...
                    </p>

                ) : specialties.length === 0 ? (
                    <p
                        style={{
                            color: "#64748b"
                        }}
                    >
                        No hay especialidades que coincidan con los filtros seleccionados.
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
                                {specialties.map(
                                    specialty => (
                                        <tr
                                            key={
                                                specialty.id
                                            }
                                        >
                                            <td>
                                                {
                                                    specialty.id
                                                }
                                            </td>

                                            <td>
                                                {
                                                    specialty.name
                                                }
                                            </td>

                                            <td>
                                                {
                                                    specialty
                                                        .description
                                                }
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        specialty.active
                                                            ? "status-active"
                                                            : "status-inactive"
                                                    }
                                                >
                                                    {specialty.active
                                                        ? "Activo"
                                                        : "Inactivo"}
                                                </span>
                                            </td>

                                            <td>
                                                {
                                                    formatDateTime(
                                                        specialty
                                                            .updatedAt
                                                    )
                                                }
                                            </td>

                                            <td>
                                                {
                                                    specialty
                                                        .updatedBy ||
                                                    "Sin información"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    specialty.version
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
                                                                            specialty
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
                                                                        specialty.active
                                                                            ? "btn btn-danger-outline"
                                                                            : "btn btn-primary"
                                                                    }
                                                                    onClick={() =>
                                                                        handleStatusChange(
                                                                            specialty
                                                                        )
                                                                    }
                                                                    disabled={
                                                                        saving
                                                                    }
                                                                >
                                                                    {specialty.active
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

                {/*
                 * Información y controles de paginación.
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
 * Convierte una fecha ISO del backend
 * en un formato legible.
 *
 * Ejemplo:
 *
 * 2026-10-12T11:30:00
 * ->
 * 12/10/2026 11:30
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

export default Specialties;