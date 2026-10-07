import {
    useEffect,
    useState
} from "react";

import {
    Link,
    useLocation,
    useNavigate
} from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import {
    deactivateBranchSpecialtyAssignment,
    getBranchSpecialtyAssignments
} from "../../api/branchSpecialtyApi";

import "./BranchSpecialtyList.css";

function BranchSpecialtyList() {
    const location = useLocation();
    const navigate = useNavigate();

    /*
     * Registros devueltos por Spring Boot.
     */
    const [assignments, setAssignments] =
        useState([]);

    /*
     * ID utilizado para filtrar el listado.
     */
    const [filterId, setFilterId] =
        useState("");

    /*
     * Información de paginación.
     */
    const [page, setPage] =
        useState(0);

    const [size, setSize] =
        useState(20);

    const [totalElements, setTotalElements] =
        useState(0);

    const [totalPages, setTotalPages] =
        useState(0);

    const [first, setFirst] =
        useState(true);

    const [last, setLast] =
        useState(true);

    /*
     * Asignación seleccionada para confirmar
     * la eliminación lógica.
     */
    const [
        assignmentToDeactivate,
        setAssignmentToDeactivate
    ] = useState(null);

    const [loading, setLoading] =
        useState(true);

    const [processing, setProcessing] =
        useState(false);

    const [errorMessage, setErrorMessage] =
        useState("");

    const [successMessage, setSuccessMessage] =
        useState("");

    /**
     * Carga el listado al abrir la pantalla
     * o cambiar página y tamaño.
     */
    useEffect(() => {
        loadAssignments();
    }, [page, size]);

    /**
 * Muestra el mensaje enviado desde el formulario
 * después de crear o reactivar una asignación.
 *
 * Luego limpia location.state para que el mensaje
 * no reaparezca al actualizar el navegador.
 */
    useEffect(() => {
        const message =
            location.state?.successMessage;

        if (!message) {
            return;
        }

        setSuccessMessage(message);

        navigate(
            location.pathname,
            {
                replace: true,
                state: {}
            }
        );
    }, [
        location.state,
        location.pathname,
        navigate
    ]);

    /**
     * Consulta las asignaciones desde Spring Boot.
     *
     * El filtro por ID es opcional.
     */
    const loadAssignments = async (
        requestedFilterId = filterId
    ) => {
        try {
            setLoading(true);
            setErrorMessage("");

            const response =
                await getBranchSpecialtyAssignments(
                    requestedFilterId,
                    page,
                    size
                );

            const data =
                response.data || {};

            setAssignments(
                Array.isArray(data.content)
                    ? data.content
                    : []
            );

            setTotalElements(
                Number(data.totalElements || 0)
            );

            setTotalPages(
                Number(data.totalPages || 0)
            );

            setFirst(
                Boolean(data.first)
            );

            setLast(
                Boolean(data.last)
            );

        } catch (error) {
            console.error(
                "Error cargando asignaciones:",
                error
            );

            setAssignments([]);
            setTotalElements(0);
            setTotalPages(0);

            setErrorMessage(
                getBackendMessage(
                    error,
                    "No fue posible cargar las asignaciones."
                )
            );
        } finally {
            setLoading(false);
        }
    };

    /**
     * Ejecuta el filtro administrativo por ID.
     */
    const handleSearch = async (event) => {
        event.preventDefault();

        const cleanFilter =
            filterId.trim();

        if (
            cleanFilter &&
            (
                !/^\d+$/.test(cleanFilter) ||
                Number(cleanFilter) <= 0
            )
        ) {
            setErrorMessage(
                "El ID debe ser un número entero mayor que cero."
            );

            return;
        }

        setPage(0);
        setSuccessMessage("");

        /*
         * Si ya estábamos en página cero, useEffect
         * no se ejecutará porque page no cambió.
         */
        if (page === 0) {
            await loadAssignments(
                cleanFilter
            );
        }
    };

    /**
     * Limpia el filtro y vuelve al listado completo.
     */
    const handleClearFilter = async () => {
        setFilterId("");
        setPage(0);
        setErrorMessage("");
        setSuccessMessage("");

        if (page === 0) {
            await loadAssignments("");
        }
    };

    /**
     * Abre la confirmación de eliminación.
     */
    const handleRequestDeactivate = (
        assignment
    ) => {
        setAssignmentToDeactivate(
            assignment
        );

        setErrorMessage("");
        setSuccessMessage("");
    };

    /**
     * Desactiva la asignación seleccionada.
     *
     * Spring Boot vuelve a validar:
     *
     * - Administrador.
     * - Existencia.
     * - Estado activo.
     * - Concurrencia.
     */
    const handleConfirmDeactivate =
        async () => {
            if (
                processing ||
                !assignmentToDeactivate
            ) {
                return;
            }

            try {
                setProcessing(true);
                setErrorMessage("");

                const response =
                    await deactivateBranchSpecialtyAssignment(
                        assignmentToDeactivate.id
                    );

                setSuccessMessage(
                    response.data.message ||
                    "Asignación eliminada correctamente."
                );

                setAssignmentToDeactivate(null);

                await loadAssignments();

            } catch (error) {
                setErrorMessage(
                    getBackendMessage(
                        error,
                        "No fue posible eliminar la asignación."
                    )
                );
            } finally {
                setProcessing(false);
            }
        };

    return (
        <MainLayout>
            <div className="branch-specialty-page">
                <header className="branch-specialty-header">
                    <div>
                        <h1>
                            Especialidades por Sede
                        </h1>

                        <p>
                            Configure las especialidades
                            disponibles para cada sede
                            del hospital.
                        </p>
                    </div>

                    <Link
                        to="/branch-specialties/create"
                        className="branch-specialty-primary-link"
                    >
                        Asignar especialidad
                    </Link>
                </header>

                {errorMessage && (
                    <div
                        className="branch-specialty-message error"
                        role="alert"
                    >
                        {errorMessage}
                    </div>
                )}

                {successMessage && (
                    <div
                        className="branch-specialty-message success"
                        role="status"
                    >
                        {successMessage}
                    </div>
                )}

                <section className="branch-specialty-card">
                    <form
                        className="branch-specialty-filter"
                        onSubmit={handleSearch}
                    >
                        <div>
                            <label htmlFor="assignment-filter-id">
                                Buscar por ID
                            </label>

                            <input
                                id="assignment-filter-id"
                                type="text"
                                inputMode="numeric"
                                placeholder="Ejemplo: 15"
                                value={filterId}
                                onChange={(event) => {
                                    setFilterId(
                                        event.target.value
                                            .replace(
                                                /\D/g,
                                                ""
                                            )
                                    );

                                    setErrorMessage("");
                                }}
                                disabled={
                                    loading ||
                                    processing
                                }
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={
                                loading ||
                                processing
                            }
                        >
                            Buscar
                        </button>

                        <button
                            type="button"
                            className="secondary"
                            onClick={handleClearFilter}
                            disabled={
                                loading ||
                                processing
                            }
                        >
                            Limpiar
                        </button>
                    </form>
                </section>

                <section className="branch-specialty-card">
                    <div className="branch-specialty-list-header">
                        <h2>
                            Asignaciones
                        </h2>

                        <span>
                            Total: {totalElements}
                        </span>
                    </div>

                    {loading ? (
                        <div className="branch-specialty-empty">
                            Cargando asignaciones...
                        </div>
                    ) : assignments.length === 0 ? (
                        <div className="branch-specialty-empty">
                            No se encontraron asignaciones
                            para los criterios ingresados.
                        </div>
                    ) : (
                        <div className="branch-specialty-table-wrapper">
                            <table className="branch-specialty-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Sede</th>
                                        <th>Especialidad</th>
                                        <th>Estado</th>
                                        <th>Actualizado por</th>
                                        <th>Fecha actualización</th>
                                        <th>Acciones</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {assignments.map(
                                        assignment => (
                                            <tr
                                                key={
                                                    assignment.id
                                                }
                                            >
                                                <td>
                                                    {assignment.id}
                                                </td>

                                                <td>
                                                    <strong>
                                                        {
                                                            assignment
                                                                .branchName
                                                        }
                                                    </strong>

                                                    <small>
                                                        {
                                                            assignment
                                                                .branchAddress ||
                                                            "Sin dirección"
                                                        }
                                                    </small>
                                                </td>

                                                <td>
                                                    <strong>
                                                        {
                                                            assignment
                                                                .specialtyName
                                                        }
                                                    </strong>

                                                    <small>
                                                        {
                                                            assignment
                                                                .specialtyDescription ||
                                                            "Sin descripción"
                                                        }
                                                    </small>
                                                </td>

                                                <td>
                                                    <span
                                                        className={
                                                            assignment.active
                                                                ? "assignment-status active"
                                                                : "assignment-status inactive"
                                                        }
                                                    >
                                                        {assignment.active
                                                            ? "Activa"
                                                            : "Inactiva"}
                                                    </span>
                                                </td>

                                                <td>
                                                    {
                                                        assignment
                                                            .updatedBy
                                                    }
                                                </td>

                                                <td>
                                                    {formatDateTime(
                                                        assignment
                                                            .updatedAt
                                                    )}
                                                </td>

                                                <td>
                                                    {assignment.active ? (
                                                        <button
                                                            type="button"
                                                            className="branch-specialty-delete"
                                                            onClick={() =>
                                                                handleRequestDeactivate(
                                                                    assignment
                                                                )
                                                            }
                                                            disabled={
                                                                processing
                                                            }
                                                        >
                                                            Eliminar
                                                        </button>
                                                    ) : (
                                                        <Link
                                                            to="/branch-specialties/create"
                                                            state={{
                                                                branchId:
                                                                    assignment
                                                                        .branchId,

                                                                specialtyId:
                                                                    assignment
                                                                        .specialtyId
                                                            }}
                                                            className="branch-specialty-reactivate"
                                                        >
                                                            Reactivar
                                                        </Link>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    <div className="branch-specialty-pagination">
                        <div>
                            <label htmlFor="assignment-page-size">
                                Registros por página
                            </label>

                            <select
                                id="assignment-page-size"
                                value={size}
                                onChange={(event) => {
                                    setSize(
                                        Number(
                                            event.target.value
                                        )
                                    );

                                    setPage(0);
                                }}
                                disabled={loading}
                            >
                                <option value={10}>
                                    10
                                </option>

                                <option value={20}>
                                    20
                                </option>

                                <option value={50}>
                                    50
                                </option>

                                <option value={100}>
                                    100
                                </option>
                            </select>
                        </div>

                        <span>
                            Página{" "}
                            {totalPages === 0
                                ? 0
                                : page + 1}
                            {" de "}
                            {totalPages}
                        </span>

                        <div className="branch-specialty-page-actions">
                            <button
                                type="button"
                                onClick={() =>
                                    setPage(previous =>
                                        Math.max(
                                            previous - 1,
                                            0
                                        )
                                    )
                                }
                                disabled={
                                    loading ||
                                    first
                                }
                            >
                                Anterior
                            </button>

                            <button
                                type="button"
                                onClick={() =>
                                    setPage(previous =>
                                        previous + 1
                                    )
                                }
                                disabled={
                                    loading ||
                                    last
                                }
                            >
                                Siguiente
                            </button>
                        </div>
                    </div>
                </section>

                {assignmentToDeactivate && (
                    <div
                        className="branch-specialty-modal-overlay"
                        role="dialog"
                        aria-modal="true"
                    >
                        <div className="branch-specialty-confirm-modal">
                            <h2>
                                Confirmar eliminación
                            </h2>

                            <p>
                                Se desactivará la especialidad{" "}
                                <strong>
                                    {
                                        assignmentToDeactivate
                                            .specialtyName
                                    }
                                </strong>{" "}
                                de la sede{" "}
                                <strong>
                                    {
                                        assignmentToDeactivate
                                            .branchName
                                    }
                                </strong>.
                            </p>

                            <p>
                                La especialidad dejará de
                                aparecer en el agendamiento
                                de citas de esta sede.
                            </p>

                            <div className="branch-specialty-modal-actions">
                                <button
                                    type="button"
                                    className="secondary"
                                    onClick={() =>
                                        setAssignmentToDeactivate(
                                            null
                                        )
                                    }
                                    disabled={processing}
                                >
                                    Cancelar
                                </button>

                                <button
                                    type="button"
                                    className="danger"
                                    onClick={
                                        handleConfirmDeactivate
                                    }
                                    disabled={processing}
                                >
                                    {processing
                                        ? "Eliminando..."
                                        : "Confirmar eliminación"}
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </MainLayout>
    );
}

function formatDateTime(value) {
    if (!value) {
        return "No disponible";
    }

    const date =
        new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString(
        "es-GT",
        {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}

function getBackendMessage(
    error,
    fallback
) {
    const data =
        error.response?.data;

    if (typeof data === "string") {
        return data;
    }

    const validationErrors =
        data?.validationErrors;

    if (validationErrors) {
        const messages =
            Object.values(
                validationErrors
            );

        if (messages.length > 0) {
            return messages[0];
        }
    }

    return data?.message || fallback;
}

export default BranchSpecialtyList;