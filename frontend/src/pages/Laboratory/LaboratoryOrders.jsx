import {
    useCallback,
    useEffect,
    useState
} from "react";

import MainLayout
    from "../../layouts/MainLayout";

import {
    getLaboratoryOrders
} from "../../api/laboratoryApi";

import LaboratoryOrderDetail
    from "./LaboratoryOrderDetail";

import "./LaboratoryOrders.css";

function LaboratoryOrders() {
    const [filters, setFilters] =
        useState({
            status: "TODOS",
            patient: "",
            doctor: ""
        });

    const [orders, setOrders] =
        useState([]);

    const [
        selectedOrderId,
        setSelectedOrderId
    ] = useState(null);

    const [loading, setLoading] =
        useState(true);

    const [errorMessage, setErrorMessage] =
        useState("");

    const loadOrders = useCallback(
        async (
            currentFilters,
            showLoading = true
        ) => {
            try {
                if (showLoading) {
                    setLoading(true);
                }

                setErrorMessage("");

                const response =
                    await getLaboratoryOrders(
                        currentFilters
                    );

                setOrders(
                    Array.isArray(response.data)
                        ? response.data
                        : []
                );

            } catch (error) {
                console.error(
                    "Error cargando órdenes:",
                    error
                );

                setErrorMessage(
                    getBackendMessage(
                        error,
                        "No fue posible cargar las órdenes de laboratorio."
                    )
                );

            } finally {
                if (showLoading) {
                    setLoading(false);
                }
            }
        },
        []
    );

    useEffect(() => {
        loadOrders(filters, true);
    }, []);

    useEffect(() => {
        const intervalId =
            setInterval(() => {
                loadOrders(
                    filters,
                    false
                );
            }, 30000);

        return () => {
            clearInterval(intervalId);
        };
    }, [filters, loadOrders]);

    //Validar si el valor del filtro es diferente al anterior antes de actualizarlo
    const updateFilter = (
        field,
        value
    ) => {
        setFilters(previous => ({
            ...previous,
            [field]: value
        }));

        setErrorMessage("");
    };

    const handleSearch = (
        event
    ) => {
        event.preventDefault();

        loadOrders(filters, true);
    };

    const clearFilters = () => {
        const emptyFilters = {
            status: "TODOS",
            patient: "",
            doctor: ""
        };

        setFilters(emptyFilters);

        loadOrders(
            emptyFilters,
            true
        );
    };

    return (
        <MainLayout>
            <div className="laboratory-page">
                <header className="laboratory-header">
                    <div>
                        <h1>
                            Gestión de Laboratorio
                        </h1>

                        <p>
                            Consulte órdenes, registre
                            resultados y publique cada
                            examen individualmente.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() =>
                            loadOrders(
                                filters,
                                true
                            )
                        }
                        disabled={loading}
                    >
                        {loading
                            ? "Actualizando..."
                            : "Actualizar"}
                    </button>
                </header>

                <section className="laboratory-filter-card">
                    <form
                        className="laboratory-filter-form"
                        onSubmit={
                            handleSearch
                        }
                    >
                        <div>
                            <label htmlFor="lab-status-filter">
                                Estado
                            </label>

                            <select
                                id="lab-status-filter"
                                value={
                                    filters.status
                                }
                                onChange={(event) =>
                                    updateFilter(
                                        "status",
                                        event.target.value
                                    )
                                }
                            >
                                <option value="TODOS">
                                    Todos
                                </option>

                                <option value="PENDIENTE_DE_PAGO">
                                    Pendiente de pago
                                </option>

                                <option value="EN_PROCESO">
                                    En proceso
                                </option>

                                <option value="COMPLETADA">
                                    Completada
                                </option>

                                <option value="CANCELADA">
                                    Cancelada
                                </option>
                            </select>
                        </div>

                        <div>
                            <label htmlFor="lab-patient-filter">
                                Paciente
                            </label>

                            <input
                                id="lab-patient-filter"
                                type="text"
                                maxLength="100"
                                placeholder={
                                    "Nombre del paciente"
                                }
                                value={
                                    filters.patient
                                }
                                onChange={(event) =>
                                    updateFilter(
                                        "patient",
                                        event.target.value
                                    )
                                }
                            />
                        </div>

                        <div>
                            <label htmlFor="lab-doctor-filter">
                                Médico
                            </label>

                            <input
                                id="lab-doctor-filter"
                                type="text"
                                maxLength="100"
                                placeholder={
                                    "Nombre del médico"
                                }
                                value={
                                    filters.doctor
                                }
                                onChange={(event) =>
                                    updateFilter(
                                        "doctor",
                                        event.target.value
                                    )
                                }
                            />
                        </div>

                        <div className="laboratory-filter-actions">
                            <button
                                type="submit"
                                className="btn btn-primary"
                                disabled={loading}
                            >
                                Buscar
                            </button>

                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={
                                    clearFilters
                                }
                            >
                                Limpiar
                            </button>
                        </div>
                    </form>
                </section>

                {errorMessage && (
                    <div className="laboratory-message error">
                        {errorMessage}
                    </div>
                )}

                {loading ? (
                    <div className="laboratory-loading">
                        Cargando órdenes de laboratorio...
                    </div>

                ) : orders.length === 0 ? (
                    <div className="laboratory-empty">
                        No se encontraron órdenes con
                        los filtros seleccionados.
                    </div>

                ) : (
                    <section className="laboratory-orders-card">
                        <div className="laboratory-table-wrapper">
                            <table className="laboratory-table">
                                <thead>
                                    <tr>
                                        <th>Orden</th>
                                        <th>Paciente</th>
                                        <th>Médico</th>
                                        <th>Estado</th>
                                        <th>Progreso</th>
                                        <th>Monto</th>
                                        <th>Fecha</th>
                                        <th>Acción</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {orders.map(
                                        order => (
                                            <tr
                                                key={
                                                    order.orderId
                                                }
                                            >
                                                <td>
                                                    <strong>
                                                        {
                                                            order
                                                                .orderNumber
                                                        }
                                                    </strong>

                                                    {order.externalOrder && (
                                                        <span className="lab-external-badge">
                                                            Externa
                                                        </span>
                                                    )}
                                                </td>

                                                <td>
                                                    {
                                                        order
                                                            .patientName
                                                    }
                                                </td>

                                                <td>
                                                    {
                                                        order
                                                            .doctorName
                                                    }
                                                </td>

                                                <td>
                                                    <span
                                                        className={
                                                            `lab-order-status ${order.status
                                                                .toLowerCase()
                                                                .replaceAll(
                                                                    "_",
                                                                    "-"
                                                                )}`
                                                        }
                                                    >
                                                        {formatStatus(
                                                            order.status
                                                        )}
                                                    </span>
                                                </td>

                                                <td>
                                                    {
                                                        order
                                                            .publishedTests
                                                    }{" "}
                                                    /{" "}
                                                    {
                                                        order
                                                            .totalTests
                                                    }
                                                </td>

                                                <td>
                                                    {formatAmount(
                                                        order.totalAmount
                                                    )}
                                                </td>

                                                <td>
                                                    {formatDateTime(
                                                        order.createdAt
                                                    )}
                                                </td>

                                                <td>
                                                    <button
                                                        type="button"
                                                        className="lab-view-button"
                                                        onClick={() =>
                                                            setSelectedOrderId(
                                                                order.orderId
                                                            )
                                                        }
                                                    >
                                                        Ver detalle
                                                    </button>
                                                </td>
                                            </tr>
                                        )
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </section>
                )}

                {selectedOrderId && (
                    <LaboratoryOrderDetail
                        orderId={
                            selectedOrderId
                        }
                        onClose={() =>
                            setSelectedOrderId(
                                null
                            )
                        }
                        onOrderUpdated={() =>
                            loadOrders(
                                filters,
                                false
                            )
                        }
                    />
                )}
            </div>
        </MainLayout>
    );
}

function formatStatus(status) {
    if (!status) {
        return "No disponible";
    }

    return status.replaceAll(
        "_",
        " "
    );
}

function formatAmount(value) {
    return `Q${Number(
        value || 0
    ).toFixed(2)}`;
}

function formatDateTime(value) {
    if (!value) {
        return "No disponible";
    }

    return new Date(value)
        .toLocaleString(
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

    return data?.message || fallback;
}

export default LaboratoryOrders;