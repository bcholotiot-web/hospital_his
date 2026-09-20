import {
    useEffect,
    useState
} from "react";

import {
    Link
} from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import {
    getMyMedicationDispensations,
    getMyPharmacyInventory,
    getPharmacyInventoryAlerts
} from "../../api/pharmacyDispensationApi";

import "./PharmacyDashboard.css";

function PharmacyDashboard() {
    /*
     * Resumen del inventario de la sucursal
     * asignada al usuario autenticado.
     */
    const [inventory, setInventory] =
        useState([]);

    /*
     * Medicamentos cuyo stock se encuentra
     * en el mínimo configurado o por debajo.
     */
    const [stockAlerts, setStockAlerts] =
        useState([]);

    /*
     * Últimos despachos registrados por el
     * usuario autenticado.
     */
    const [dispensations, setDispensations] =
        useState([]);

    const [loading, setLoading] =
        useState(true);

    const [errorMessage, setErrorMessage] =
        useState("");

    /*
     * Carga los datos generales cuando se abre
     * el Panel de Farmacia.
     */
    useEffect(() => {
        loadDashboard();
    }, []);

    /**
     * Carga inventario, alertas e historial.
     *
     * Promise.all permite ejecutar las tres
     * solicitudes al mismo tiempo.
     */
    const loadDashboard = async () => {
        try {
            setLoading(true);
            setErrorMessage("");

            const [
                inventoryResponse,
                alertsResponse,
                dispensationsResponse
            ] = await Promise.all([
                getMyPharmacyInventory(),
                getPharmacyInventoryAlerts(),
                getMyMedicationDispensations()
            ]);

            setInventory(
                Array.isArray(
                    inventoryResponse.data
                )
                    ? inventoryResponse.data
                    : []
            );

            setStockAlerts(
                Array.isArray(
                    alertsResponse.data
                )
                    ? alertsResponse.data
                    : []
            );

            setDispensations(
                Array.isArray(
                    dispensationsResponse.data
                )
                    ? dispensationsResponse.data
                    : []
            );

        } catch (error) {
            console.error(
                "Error cargando Panel de Farmacia:",
                error
            );

            setInventory([]);
            setStockAlerts([]);
            setDispensations([]);

            setErrorMessage(
                getBackendMessage(
                    error,
                    "No fue posible cargar la información de Farmacia."
                )
            );
        } finally {
            setLoading(false);
        }
    };

    /*
     * Cantidad de medicamentos que tienen
     * existencias mayores que cero.
     */
    const availableMedications =
        inventory.filter(item =>
            Number(item.currentStock) > 0
        ).length;

    /*
     * Cantidad de medicamentos que no tienen
     * existencias.
     */
    const outOfStockMedications =
        inventory.filter(item =>
            Number(item.currentStock) === 0
        ).length;

    /*
     * Mostrar solo los cinco despachos más recientes.
     */
    const recentDispensations =
        dispensations.slice(0, 5);

    return (
        <MainLayout>
            <div className="pharmacy-dashboard">
                <header className="pharmacy-dashboard-header">
                    <div>
                        <h1>
                            Panel de Farmacia
                        </h1>

                        <p>
                            Consulte recetas, registre
                            despachos y supervise las
                            existencias de medicamentos.
                        </p>
                    </div>

                    <Link
                        to="/pharmacy/dispensations"
                        className="pharmacy-primary-link"
                    >
                        Nuevo despacho
                    </Link>
                </header>

                {errorMessage && (
                    <div
                        className="pharmacy-dashboard-message error"
                        role="alert"
                    >
                        {errorMessage}
                    </div>
                )}

                {loading ? (
                    <div className="pharmacy-dashboard-loading">
                        Cargando información de Farmacia...
                    </div>
                ) : (
                    <>
                        <section className="pharmacy-summary-grid">
                            <article className="pharmacy-summary-card">
                                <span>
                                    Medicamentos registrados
                                </span>

                                <strong>
                                    {inventory.length}
                                </strong>

                                <small>
                                    Inventario activo de la sucursal
                                </small>
                            </article>

                            <article className="pharmacy-summary-card available">
                                <span>
                                    Con existencias
                                </span>

                                <strong>
                                    {availableMedications}
                                </strong>

                                <small>
                                    Disponibles para despacho
                                </small>
                            </article>

                            <article className="pharmacy-summary-card warning">
                                <span>
                                    Stock mínimo
                                </span>

                                <strong>
                                    {stockAlerts.length}
                                </strong>

                                <small>
                                    Requieren revisión
                                </small>
                            </article>

                            <article className="pharmacy-summary-card danger">
                                <span>
                                    Sin existencias
                                </span>

                                <strong>
                                    {outOfStockMedications}
                                </strong>

                                <small>
                                    No disponibles para despacho
                                </small>
                            </article>
                        </section>

                        <section className="pharmacy-dashboard-card">
                            <div className="pharmacy-section-header">
                                <div>
                                    <h2>
                                        Alertas de inventario
                                    </h2>

                                    <p>
                                        Medicamentos que alcanzaron
                                        el stock mínimo configurado.
                                    </p>
                                </div>

                                <Link
                                    to="/pharmacy/inventory"
                                    className="pharmacy-secondary-link"
                                >
                                    Ver inventario
                                </Link>
                            </div>

                            {stockAlerts.length === 0 ? (
                                <div className="pharmacy-empty-state">
                                    No existen alertas de stock
                                    mínimo en este momento.
                                </div>
                            ) : (
                                <div className="pharmacy-alert-list">
                                    {stockAlerts.map(item => (
                                        <article
                                            key={item.inventoryId}
                                            className="pharmacy-stock-alert"
                                        >
                                            <div>
                                                <strong>
                                                    {
                                                        item
                                                            .medicationName
                                                    }
                                                </strong>

                                                <span>
                                                    {
                                                        item
                                                            .medicationCode
                                                    }
                                                    {" · "}
                                                    {
                                                        item
                                                            .concentration
                                                    }
                                                    {" · "}
                                                    {
                                                        item
                                                            .presentation
                                                    }
                                                </span>
                                            </div>

                                            <div className="pharmacy-stock-values">
                                                <span>
                                                    Disponible
                                                </span>

                                                <strong>
                                                    {
                                                        item
                                                            .currentStock
                                                    }
                                                </strong>

                                                <small>
                                                    Mínimo:{" "}
                                                    {
                                                        item
                                                            .minimumStock
                                                    }
                                                </small>
                                            </div>
                                        </article>
                                    ))}
                                </div>
                            )}
                        </section>

                        <section className="pharmacy-dashboard-card">
                            <div className="pharmacy-section-header">
                                <div>
                                    <h2>
                                        Despachos recientes
                                    </h2>

                                    <p>
                                        Operaciones registradas por
                                        el usuario autenticado.
                                    </p>
                                </div>

                                <Link
                                    to="/pharmacy/dispensations"
                                    className="pharmacy-secondary-link"
                                >
                                    Realizar despacho
                                </Link>
                            </div>

                            {recentDispensations.length === 0 ? (
                                <div className="pharmacy-empty-state">
                                    No existen despachos registrados.
                                </div>
                            ) : (
                                <div className="pharmacy-table-wrapper">
                                    <table className="pharmacy-table">
                                        <thead>
                                            <tr>
                                                <th>
                                                    Despacho
                                                </th>

                                                <th>
                                                    Receta
                                                </th>

                                                <th>
                                                    Paciente
                                                </th>

                                                <th>
                                                    Método
                                                </th>

                                                <th>
                                                    Total
                                                </th>

                                                <th>
                                                    Estado
                                                </th>

                                                <th>
                                                    Fecha
                                                </th>
                                            </tr>
                                        </thead>

                                        <tbody>
                                            {recentDispensations
                                                .map(dispensation => (
                                                    <tr
                                                        key={
                                                            dispensation
                                                                .dispensationId
                                                        }
                                                    >
                                                        <td>
                                                            {
                                                                dispensation
                                                                    .dispensationNumber
                                                            }
                                                        </td>

                                                        <td>
                                                            {
                                                                dispensation
                                                                    .prescriptionNumber
                                                            }
                                                        </td>

                                                        <td>
                                                            {
                                                                dispensation
                                                                    .patientName
                                                            }
                                                        </td>

                                                        <td>
                                                            {formatStatus(
                                                                dispensation
                                                                    .paymentMethod
                                                            )}
                                                        </td>

                                                        <td>
                                                            {formatAmount(
                                                                dispensation
                                                                    .totalAmount
                                                            )}
                                                        </td>

                                                        <td>
                                                            {formatStatus(
                                                                dispensation
                                                                    .dispensationStatus
                                                            )}
                                                        </td>

                                                        <td>
                                                            {formatDateTime(
                                                                dispensation
                                                                    .confirmedAt
                                                            )}
                                                        </td>
                                                    </tr>
                                                ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </section>
                    </>
                )}
            </div>
        </MainLayout>
    );
}

/**
 * Presenta un monto en quetzales.
 */
function formatAmount(value) {
    return `Q${Number(
        value || 0
    ).toFixed(2)}`;
}

/**
 * Convierte un enum del backend a texto legible.
 */
function formatStatus(value) {
    if (!value) {
        return "No disponible";
    }

    return String(value)
        .replaceAll("_", " ");
}

/**
 * Presenta fecha y hora local.
 */
function formatDateTime(value) {
    if (!value) {
        return "No disponible";
    }

    const date = new Date(value);

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

/**
 * Obtiene un mensaje enviado por Spring Boot.
 */
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

export default PharmacyDashboard;