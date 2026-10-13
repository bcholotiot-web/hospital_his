import {
    useCallback,
    useEffect,
    useMemo,
    useState
} from "react";

import {
    Link
} from "react-router-dom";

import MainLayout from "../../layouts/MainLayout";

import {
    getInventoryMonthlySummary,
    getInventoryMovements
} from "../../api/inventoryMovementApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

/**
 * Tipos de movimiento soportados por el backend.
 */
const MOVEMENT_TYPES = [
    {
        value: "STOCK_INICIAL",
        label: "Stock inicial"
    },
    {
        value: "ENTRADA",
        label: "Entrada"
    },
    {
        value: "SALIDA",
        label: "Salida manual"
    },
    {
        value: "AJUSTE_POSITIVO",
        label: "Ajuste positivo"
    },
    {
        value: "AJUSTE_NEGATIVO",
        label: "Ajuste negativo"
    },
    {
        value: "DESPACHO",
        label: "Despacho"
    },
    {
        value: "REACTIVACION",
        label: "Reactivación"
    }
];

/**
 * Nombres visibles de los meses.
 */
const MONTHS = [
    {
        value: 1,
        label: "Enero"
    },
    {
        value: 2,
        label: "Febrero"
    },
    {
        value: 3,
        label: "Marzo"
    },
    {
        value: 4,
        label: "Abril"
    },
    {
        value: 5,
        label: "Mayo"
    },
    {
        value: 6,
        label: "Junio"
    },
    {
        value: 7,
        label: "Julio"
    },
    {
        value: 8,
        label: "Agosto"
    },
    {
        value: 9,
        label: "Septiembre"
    },
    {
        value: 10,
        label: "Octubre"
    },
    {
        value: 11,
        label: "Noviembre"
    },
    {
        value: 12,
        label: "Diciembre"
    }
];

function InventoryMovements() {
    const {
        hasPermission
    } = useAuth();

    const canRead =
        hasPermission(
            "INVENTORY_MOVEMENT_READ"
        );

    /**
     * Fecha actual utilizada como valor inicial
     * del resumen mensual.
     */
    const currentDate =
        useMemo(
            () => new Date(),
            []
        );

    /**
     * Movimientos paginados.
     */
    const [
        movements,
        setMovements
    ] = useState([]);

    const [
        loading,
        setLoading
    ] = useState(false);

    /**
     * Filtros aplicados al backend.
     */
    const [
        searchFilter,
        setSearchFilter
    ] = useState("");

    const [
        movementTypeFilter,
        setMovementTypeFilter
    ] = useState("");

    const [
        startDateFilter,
        setStartDateFilter
    ] = useState("");

    const [
        endDateFilter,
        setEndDateFilter
    ] = useState("");

    /**
     * Errores de los filtros.
     */
    const [
        filterErrors,
        setFilterErrors
    ] = useState({});

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
    ] = useState(20);

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
     * Resumen mensual.
     */
    const [
        summaryYear,
        setSummaryYear
    ] = useState(
        currentDate.getFullYear()
    );

    const [
        summaryMonth,
        setSummaryMonth
    ] = useState(
        currentDate.getMonth() + 1
    );

    const [
        monthlySummary,
        setMonthlySummary
    ] = useState(null);

    const [
        loadingSummary,
        setLoadingSummary
    ] = useState(false);

    const [
        summaryError,
        setSummaryError
    ] = useState("");

    /**
     * Cargar bitácora paginada.
     */
    const loadMovements =
        useCallback(async () => {
            if (!canRead) {
                return;
            }

            try {
                setLoading(true);

                const response =
                    await getInventoryMovements({
                        search:
                            searchFilter,
                        movementType:
                            movementTypeFilter,
                        startDate:
                            startDateFilter,
                        endDate:
                            endDateFilter,
                        page,
                        size
                    });

                const pageData =
                    response.data;

                setMovements(
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
                    pageData
                        ?.first ?? true
                );

                setLast(
                    pageData
                        ?.last ?? true
                );

            } catch (error) {
                console.error(
                    "Error al cargar movimientos:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar los movimientos de inventario."
                );

                setMovements([]);
                setTotalElements(0);
                setTotalPages(0);
                setFirst(true);
                setLast(true);

            } finally {
                setLoading(false);
            }
        }, [
            canRead,
            searchFilter,
            movementTypeFilter,
            startDateFilter,
            endDateFilter,
            page,
            size
        ]);

    /**
     * Cargar movimientos al abrir la pantalla
     * o cambiar página.
     */
    useEffect(() => {
        loadMovements();
    }, [loadMovements]);

    /**
     * Consulta el resumen mensual.
     */
    const loadMonthlySummary =
        useCallback(async () => {
            if (!canRead) {
                return;
            }

            const numericYear =
                Number(summaryYear);

            const numericMonth =
                Number(summaryMonth);

            const currentYear =
                new Date().getFullYear();

            if (
                !Number.isInteger(
                    numericYear
                ) ||
                numericYear < 2000 ||
                numericYear > currentYear
            ) {
                setSummaryError(
                    `El año debe estar entre 2000 y ${currentYear}.`
                );

                setMonthlySummary(null);

                return;
            }

            if (
                !Number.isInteger(
                    numericMonth
                ) ||
                numericMonth < 1 ||
                numericMonth > 12
            ) {
                setSummaryError(
                    "El mes debe estar entre 1 y 12."
                );

                setMonthlySummary(null);

                return;
            }

            try {
                setLoadingSummary(true);
                setSummaryError("");

                const response =
                    await getInventoryMonthlySummary(
                        numericYear,
                        numericMonth
                    );

                setMonthlySummary(
                    response.data
                );

            } catch (error) {
                console.error(
                    "Error al cargar resumen mensual:",
                    error
                );

                setSummaryError(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar el resumen mensual."
                );

                setMonthlySummary(null);

            } finally {
                setLoadingSummary(false);
            }
        }, [
            canRead,
            summaryYear,
            summaryMonth
        ]);

    /**
     * Cargar el resumen del mes actual
     * al abrir la pantalla.
     */
    useEffect(() => {
        loadMonthlySummary();
    }, [loadMonthlySummary]);

    /**
     * Validar rango de fechas.
     */
    const validateFilters = () => {
        const newErrors = {};

        if (
            startDateFilter &&
            endDateFilter &&
            startDateFilter >
            endDateFilter
        ) {
            newErrors.dateRange =
                "La fecha inicial no puede ser posterior a la fecha final.";
        }

        setFilterErrors(
            newErrors
        );

        return (
            Object.keys(newErrors)
                .length === 0
        );
    };

    /**
     * Aplicar filtros.
     */
    const handleSearch =
        event => {
            event.preventDefault();

            if (!validateFilters()) {
                return;
            }

            if (page === 0) {
                loadMovements();
            } else {
                setPage(0);
            }
        };

    /**
     * Limpiar filtros.
     */
    const clearFilters = () => {
        setSearchFilter("");
        setMovementTypeFilter("");
        setStartDateFilter("");
        setEndDateFilter("");
        setFilterErrors({});
        setPage(0);
    };

    /**
     * Cambiar cantidad de registros.
     */
    const handleSizeChange =
        event => {
            setSize(
                Number(
                    event.target.value
                )
            );

            setPage(0);
        };

    /**
     * Consultar manualmente el resumen mensual.
     */
    const handleSummarySearch =
        event => {
            event.preventDefault();

            loadMonthlySummary();
        };

    if (!canRead) {
        return (
            <MainLayout>
                <h1 className="page-title">
                    Movimientos de Inventario
                </h1>

                <div className="card">
                    <p>
                        No tiene permiso para consultar los movimientos de inventario.
                    </p>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <h1 className="page-title">
                Movimientos de Inventario
            </h1>

            <div
                className="button-row"
                style={{
                    marginBottom: "16px"
                }}
            >
                <Link
                    to="/pharmacy/inventory"
                    className="btn btn-secondary"
                >
                    Volver al inventario
                </Link>

                <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={
                        loadMovements
                    }
                    disabled={loading}
                >
                    {loading
                        ? "Actualizando..."
                        : "Actualizar bitácora"}
                </button>
            </div>

            {/*
             * Resumen mensual.
             */}
            <div className="card">
                <h2 className="section-title">
                    Resumen Mensual
                </h2>

                <form
                    onSubmit={
                        handleSummarySearch
                    }
                >
                    <div className="form-grid">
                        <div className="form-group">
                            <label>
                                Año
                            </label>

                            <input
                                type="number"
                                min="2000"
                                max={
                                    currentDate
                                        .getFullYear()
                                }
                                step="1"
                                value={
                                    summaryYear
                                }
                                onChange={
                                    event =>
                                        setSummaryYear(
                                            event
                                                .target
                                                .value
                                        )
                                }
                            />
                        </div>

                        <div className="form-group">
                            <label>
                                Mes
                            </label>

                            <select
                                value={
                                    summaryMonth
                                }
                                onChange={
                                    event =>
                                        setSummaryMonth(
                                            Number(
                                                event
                                                    .target
                                                    .value
                                            )
                                        )
                                }
                            >
                                {MONTHS.map(
                                    month => (
                                        <option
                                            key={
                                                month.value
                                            }
                                            value={
                                                month.value
                                            }
                                        >
                                            {
                                                month.label
                                            }
                                        </option>
                                    )
                                )}
                            </select>
                        </div>
                    </div>

                    <div className="button-row">
                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={
                                loadingSummary
                            }
                        >
                            {loadingSummary
                                ? "Consultando..."
                                : "Consultar resumen"}
                        </button>
                    </div>
                </form>

                {summaryError && (
                    <div
                        className="error-message"
                        style={{
                            marginTop: "12px"
                        }}
                    >
                        {summaryError}
                    </div>
                )}

                {loadingSummary ? (
                    <p>
                        Cargando resumen mensual...
                    </p>

                ) : monthlySummary && (
                    <MonthlySummary
                        summary={
                            monthlySummary
                        }
                    />
                )}
            </div>

            <br />

            {/*
             * Bitácora paginada.
             */}
            <div className="table-card">
                <h2 className="section-title">
                    Bitácora de Movimientos
                </h2>

                <form
                    onSubmit={
                        handleSearch
                    }
                >
                    <div className="form-grid">
                        <div className="form-group">
                            <label>
                                Buscar
                            </label>

                            <input
                                type="text"
                                maxLength={200}
                                placeholder="Código o nombre del medicamento"
                                value={
                                    searchFilter
                                }
                                onChange={
                                    event =>
                                        setSearchFilter(
                                            event
                                                .target
                                                .value
                                        )
                                }
                            />
                        </div>

                        <div className="form-group">
                            <label>
                                Tipo de movimiento
                            </label>

                            <select
                                value={
                                    movementTypeFilter
                                }
                                onChange={
                                    event =>
                                        setMovementTypeFilter(
                                            event
                                                .target
                                                .value
                                        )
                                }
                            >
                                <option value="">
                                    Todos
                                </option>

                                {MOVEMENT_TYPES.map(
                                    movementType => (
                                        <option
                                            key={
                                                movementType.value
                                            }
                                            value={
                                                movementType.value
                                            }
                                        >
                                            {
                                                movementType.label
                                            }
                                        </option>
                                    )
                                )}
                            </select>
                        </div>

                        <div className="form-group">
                            <label>
                                Fecha inicial
                            </label>

                            <input
                                type="date"
                                value={
                                    startDateFilter
                                }
                                onChange={
                                    event => {
                                        setStartDateFilter(
                                            event
                                                .target
                                                .value
                                        );

                                        setFilterErrors(
                                            {}
                                        );
                                    }
                                }
                            />
                        </div>

                        <div className="form-group">
                            <label>
                                Fecha final
                            </label>

                            <input
                                type="date"
                                value={
                                    endDateFilter
                                }
                                onChange={
                                    event => {
                                        setEndDateFilter(
                                            event
                                                .target
                                                .value
                                        );

                                        setFilterErrors(
                                            {}
                                        );
                                    }
                                }
                            />
                        </div>

                        <div className="form-group">
                            <label>
                                Registros por página
                            </label>

                            <select
                                value={size}
                                onChange={
                                    handleSizeChange
                                }
                            >
                                <option value={10}>
                                    10 registros
                                </option>

                                <option value={20}>
                                    20 registros
                                </option>

                                <option value={50}>
                                    50 registros
                                </option>

                                <option value={100}>
                                    100 registros
                                </option>
                            </select>
                        </div>
                    </div>

                    {filterErrors.dateRange && (
                        <div className="error-message">
                            {
                                filterErrors
                                    .dateRange
                            }
                        </div>
                    )}

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
                        Cargando movimientos...
                    </p>

                ) : movements.length === 0 ? (
                    <p
                        style={{
                            color: "#64748b"
                        }}
                    >
                        No existen movimientos que coincidan con los filtros seleccionados.
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
                                        Fecha
                                    </th>

                                    <th>
                                        Medicamento
                                    </th>

                                    <th>
                                        Tipo
                                    </th>

                                    <th>
                                        Stock anterior
                                    </th>

                                    <th>
                                        Cantidad
                                    </th>

                                    <th>
                                        Movimiento
                                    </th>

                                    <th>
                                        Stock resultante
                                    </th>

                                    <th>
                                        Motivo
                                    </th>

                                    <th>
                                        Referencia
                                    </th>

                                    <th>
                                        Usuario
                                    </th>
                                </tr>
                            </thead>

                            <tbody>
                                {movements.map(
                                    movement => (
                                        <tr
                                            key={
                                                movement
                                                    .movementId
                                            }
                                        >
                                            <td>
                                                {
                                                    formatDateTime(
                                                        movement
                                                            .createdAt
                                                    )
                                                }
                                            </td>

                                            <td>
                                                <strong>
                                                    {
                                                        movement
                                                            .medicationCode
                                                    }
                                                </strong>

                                                <div>
                                                    {
                                                        movement
                                                            .medicationName
                                                    }
                                                </div>
                                            </td>

                                            <td>
                                                <MovementTypeBadge
                                                    type={
                                                        movement
                                                            .movementType
                                                    }
                                                />
                                            </td>

                                            <td>
                                                {
                                                    formatStock(
                                                        movement
                                                            .previousStock,
                                                        movement
                                                            .medicationUnit
                                                    )
                                                }
                                            </td>

                                            <td>
                                                {
                                                    formatStock(
                                                        movement
                                                            .quantity,
                                                        movement
                                                            .medicationUnit
                                                    )
                                                }
                                            </td>

                                            <td>
                                                <SignedQuantity
                                                    value={
                                                        movement
                                                            .signedQuantity
                                                    }
                                                    unit={
                                                        movement
                                                            .medicationUnit
                                                    }
                                                />
                                            </td>

                                            <td>
                                                <strong>
                                                    {
                                                        formatStock(
                                                            movement
                                                                .resultingStock,
                                                            movement
                                                                .medicationUnit
                                                        )
                                                    }
                                                </strong>
                                            </td>

                                            <td>
                                                {
                                                    movement
                                                        .reason ||
                                                    "Sin motivo"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    formatReference(
                                                        movement
                                                    )
                                                }
                                            </td>

                                            <td>
                                                {
                                                    movement
                                                        .createdBy ||
                                                    "Sin información"
                                                }
                                            </td>
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
                        justifyContent:
                            "space-between",
                        alignItems: "center",
                        gap: "12px",
                        flexWrap: "wrap"
                    }}
                >
                    <div>
                        Total:
                        {" "}
                        <strong>
                            {totalElements}
                        </strong>
                        {" "}
                        movimiento(s)
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
 * Resumen mensual general y por medicamento.
 */
function MonthlySummary({
    summary
}) {
    const medications =
        Array.isArray(
            summary?.medications
        )
            ? summary.medications
            : [];

    return (
        <div
            style={{
                marginTop: "18px"
            }}
        >
            <div
                style={{
                    padding: "12px",
                    borderRadius: "6px",
                    backgroundColor:
                        "#f8fafc",
                    color: "#334155",
                    marginBottom: "16px"
                }}
            >
                <strong>
                    Sucursal:
                </strong>
                {" "}
                {summary.branchName ||
                    "Sin información"}

                {" | "}

                <strong>
                    Período:
                </strong>
                {" "}
                {formatDate(
                    summary.periodStart
                )}
                {" "}
                al
                {" "}
                {formatDate(
                    summary.periodEnd
                )}
            </div>

            <div
                style={{
                    display: "grid",
                    gridTemplateColumns:
                        "repeat(auto-fit, minmax(180px, 1fr))",
                    gap: "12px",
                    marginBottom: "18px"
                }}
            >
                <SummaryCard
                    title="Movimientos"
                    value={
                        summary.totalMovements ??
                        0
                    }
                    color="#1d4ed8"
                />

                <SummaryCard
                    title="Entradas"
                    value={
                        summary.totalEntries ??
                        0
                    }
                    color="#15803d"
                />

                <SummaryCard
                    title="Salidas manuales"
                    value={
                        summary.totalOutputs ??
                        0
                    }
                    color="#b45309"
                />

                <SummaryCard
                    title="Despachado"
                    value={
                        summary.totalDispensed ??
                        0
                    }
                    color="#b91c1c"
                />
            </div>

            <h3 className="section-title">
                Resumen por Medicamento
            </h3>

            {medications.length === 0 ? (
                <p>
                    No existen movimientos para el período seleccionado.
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
                                    Medicamento
                                </th>

                                <th>
                                    Stock inicial
                                </th>

                                <th>
                                    Entradas
                                </th>

                                <th>
                                    Salidas manuales
                                </th>

                                <th>
                                    Despachado
                                </th>

                                <th>
                                    Ajustes positivos
                                </th>

                                <th>
                                    Ajustes negativos
                                </th>

                                <th>
                                    Stock final
                                </th>

                                <th>
                                    Movimientos
                                </th>
                            </tr>
                        </thead>

                        <tbody>
                            {medications.map(
                                medication => (
                                    <tr
                                        key={
                                            medication
                                                .medicationId
                                        }
                                    >
                                        <td>
                                            <strong>
                                                {
                                                    medication
                                                        .medicationCode
                                                }
                                            </strong>

                                            <div>
                                                {
                                                    medication
                                                        .medicationName
                                                }
                                            </div>
                                        </td>

                                        <td>
                                            {
                                                formatStock(
                                                    medication
                                                        .initialStock,
                                                    medication
                                                        .medicationUnit
                                                )
                                            }
                                        </td>

                                        <td>
                                            <PositiveValue
                                                value={
                                                    medication
                                                        .totalEntries
                                                }
                                                unit={
                                                    medication
                                                        .medicationUnit
                                                }
                                            />
                                        </td>

                                        <td>
                                            <NegativeValue
                                                value={
                                                    medication
                                                        .totalManualOutputs
                                                }
                                                unit={
                                                    medication
                                                        .medicationUnit
                                                }
                                            />
                                        </td>

                                        <td>
                                            <NegativeValue
                                                value={
                                                    medication
                                                        .totalDispensed
                                                }
                                                unit={
                                                    medication
                                                        .medicationUnit
                                                }
                                            />
                                        </td>

                                        <td>
                                            <PositiveValue
                                                value={
                                                    medication
                                                        .totalPositiveAdjustments
                                                }
                                                unit={
                                                    medication
                                                        .medicationUnit
                                                }
                                            />
                                        </td>

                                        <td>
                                            <NegativeValue
                                                value={
                                                    medication
                                                        .totalNegativeAdjustments
                                                }
                                                unit={
                                                    medication
                                                        .medicationUnit
                                                }
                                            />
                                        </td>

                                        <td>
                                            <strong>
                                                {
                                                    formatStock(
                                                        medication
                                                            .finalStock,
                                                        medication
                                                            .medicationUnit
                                                    )
                                                }
                                            </strong>
                                        </td>

                                        <td>
                                            {
                                                medication
                                                    .movementCount ??
                                                0
                                            }
                                        </td>
                                    </tr>
                                )
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}

function SummaryCard({
    title,
    value,
    color
}) {
    return (
        <div
            style={{
                padding: "16px",
                borderRadius: "8px",
                backgroundColor:
                    "#ffffff",
                border:
                    "1px solid #e2e8f0",
                borderLeft:
                    `5px solid ${color}`
            }}
        >
            <div
                style={{
                    color: "#64748b",
                    marginBottom: "6px"
                }}
            >
                {title}
            </div>

            <div
                style={{
                    fontSize: "1.5rem",
                    fontWeight: 700,
                    color
                }}
            >
                {value}
            </div>
        </div>
    );
}

function MovementTypeBadge({
    type
}) {
    const labels = {
        STOCK_INICIAL:
            "Stock inicial",

        ENTRADA:
            "Entrada",

        SALIDA:
            "Salida",

        AJUSTE_POSITIVO:
            "Ajuste positivo",

        AJUSTE_NEGATIVO:
            "Ajuste negativo",

        DESPACHO:
            "Despacho",

        REACTIVACION:
            "Reactivación"
    };

    const positiveTypes =
        new Set([
            "STOCK_INICIAL",
            "ENTRADA",
            "AJUSTE_POSITIVO",
            "REACTIVACION"
        ]);

    const negativeTypes =
        new Set([
            "SALIDA",
            "AJUSTE_NEGATIVO",
            "DESPACHO"
        ]);

    let className = "";

    if (positiveTypes.has(type)) {
        className =
            "status-active";

    } else if (
        negativeTypes.has(type)
    ) {
        className =
            "status-inactive";
    }

    return (
        <span
            className={className}
            style={{
                whiteSpace: "nowrap"
            }}
        >
            {labels[type] ||
                type ||
                "Sin tipo"}
        </span>
    );
}

function SignedQuantity({
    value,
    unit
}) {
    const numericValue =
        Number(value ?? 0);

    const positive =
        numericValue > 0;

    const negative =
        numericValue < 0;

    return (
        <strong
            style={{
                color:
                    positive
                        ? "#15803d"
                        : (
                            negative
                                ? "#b91c1c"
                                : "#475569"
                        ),
                whiteSpace: "nowrap"
            }}
        >
            {positive
                ? "+"
                : ""}
            {numericValue}
            {" "}
            {unit || "UNIDADES"}
        </strong>
    );
}

function PositiveValue({
    value,
    unit
}) {
    const numericValue =
        Number(value ?? 0);

    return (
        <span
            style={{
                color:
                    numericValue > 0
                        ? "#15803d"
                        : "#475569",
                whiteSpace: "nowrap"
            }}
        >
            {numericValue > 0
                ? "+"
                : ""}
            {numericValue}
            {" "}
            {unit || "UNIDADES"}
        </span>
    );
}

function NegativeValue({
    value,
    unit
}) {
    const numericValue =
        Number(value ?? 0);

    return (
        <span
            style={{
                color:
                    numericValue > 0
                        ? "#b91c1c"
                        : "#475569",
                whiteSpace: "nowrap"
            }}
        >
            {numericValue > 0
                ? "-"
                : ""}
            {numericValue}
            {" "}
            {unit || "UNIDADES"}
        </span>
    );
}

function formatStock(
    value,
    unit
) {
    return `${value ?? 0
        } ${unit || "UNIDADES"
        }`;
}

function formatReference(
    movement
) {
    if (
        !movement?.referenceType &&
        movement?.referenceId == null
    ) {
        return "Sin referencia";
    }

    if (
        movement?.referenceType &&
        movement?.referenceId != null
    ) {
        return `${movement.referenceType} #${movement.referenceId}`;
    }

    return (
        movement?.referenceType ||
        `#${movement?.referenceId}`
    );
}

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

function formatDate(value) {
    if (!value) {
        return "Sin información";
    }

    const dateParts =
        String(value)
            .split("-");

    if (
        dateParts.length !== 3
    ) {
        return value;
    }

    return `${dateParts[2]}/${dateParts[1]}/${dateParts[0]}`;
}

export default InventoryMovements;