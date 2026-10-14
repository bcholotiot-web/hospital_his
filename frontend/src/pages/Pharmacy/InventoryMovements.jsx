import {
    useCallback,
    useEffect,
    useMemo,
    useState
} from "react";

import {
    Link
} from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import {
    getInventoryMonthlySummary,
    getInventoryMovements
} from "../../api/inventoryMovementApi";

import {
    getActiveBranches
} from "../../api/branchApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

/**
 * Tipos de movimientos almacenados
 * en la bitácora.
 *
 * Incluye movimientos manuales y automáticos.
 */
const MOVEMENT_TYPES = [
    {
        value: "STOCK_INICIAL",
        label: "Stock inicial"
    },
    {
        value: "ENTRADA",
        label: "Entrada histórica"
    },
    {
        value: "SALIDA",
        label: "Salida histórica"
    },
    {
        value: "COMPRA",
        label: "Compra"
    },
    {
        value: "DEVOLUCION",
        label: "Devolución"
    },
    {
        value: "VENTA",
        label: "Venta"
    },
    {
        value: "RECLAMO",
        label: "Reclamo"
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
 * Meses disponibles para consultar
 * el resumen mensual.
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
        user,
        hasRole,
        hasPermission
    } = useAuth();

    /**
     * ADMIN debe seleccionar una sucursal.
     *
     * FARMACIA utiliza automáticamente la
     * sucursal asignada a su cuenta.
     */
    const isAdmin =
        hasRole("ADMIN") ||
        hasRole("ADMINISTRADOR");

    const isPharmacy =
        hasRole("FARMACIA");

    /**
     * Permisos del módulo.
     */
    const canRead =
        hasPermission(
            "INVENTORY_MOVEMENT_READ"
        );

    const canCreate =
        hasPermission(
            "INVENTORY_MOVEMENT_CREATE"
        );

    /**
     * Fecha actual utilizada para inicializar
     * el resumen mensual.
     */
    const currentDate =
        useMemo(
            () => new Date(),
            []
        );

    /**
     * Sucursal seleccionada por ADMIN.
     */
    const [
        selectedBranchId,
        setSelectedBranchId
    ] = useState("");

    const [
        branches,
        setBranches
    ] = useState([]);

    const [
        loadingBranches,
        setLoadingBranches
    ] = useState(false);

    const [
        branchError,
        setBranchError
    ] = useState("");

    /**
     * Movimientos recuperados del backend.
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
     * Filtros de la bitácora.
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
        referenceNumberFilter,
        setReferenceNumberFilter
    ] = useState("");

    const [
        createdByFilter,
        setCreatedByFilter
    ] = useState("");

    const [
        activeFilter,
        setActiveFilter
    ] = useState("");

    const [
        startDateFilter,
        setStartDateFilter
    ] = useState("");

    const [
        endDateFilter,
        setEndDateFilter
    ] = useState("");

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
     * Carga las sucursales activas
     * únicamente para ADMIN.
     */
    const loadBranches =
        useCallback(async () => {
            if (
                !canRead ||
                !isAdmin
            ) {
                setBranches([]);
                setLoadingBranches(false);
                return;
            }

            try {
                setLoadingBranches(true);
                setBranchError("");

                const response =
                    await getActiveBranches();

                const branchList =
                    Array.isArray(
                        response.data
                    )
                        ? response.data
                        : [];

                setBranches(
                    branchList
                );

            } catch (error) {
                console.error(
                    "Error al cargar sucursales:",
                    error
                );

                setBranches([]);

                setBranchError(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar las sucursales activas."
                );

            } finally {
                setLoadingBranches(false);
            }
        }, [
            canRead,
            isAdmin
        ]);

    useEffect(() => {
        loadBranches();
    }, [loadBranches]);

    /**
     * Carga la bitácora paginada.
     *
     * ADMIN:
     * exige una sucursal seleccionada.
     *
     * FARMACIA:
     * no envía branchId porque Spring Boot
     * utiliza la sucursal del usuario.
     */
    const loadMovements =
        useCallback(async () => {
            if (!canRead) {
                return;
            }

            /**
             * Evitar una llamada inválida cuando
             * ADMIN todavía no seleccionó sucursal.
             */
            if (
                isAdmin &&
                !selectedBranchId
            ) {
                setMovements([]);
                setTotalElements(0);
                setTotalPages(0);
                setFirst(true);
                setLast(true);
                setLoading(false);
                return;
            }

            try {
                setLoading(true);

                const response =
                    await getInventoryMovements({
                        branchId:
                            isAdmin
                                ? selectedBranchId
                                : "",
                        search:
                            searchFilter,
                        movementType:
                            movementTypeFilter,
                        referenceNumber:
                            referenceNumberFilter,
                        createdBy:
                            createdByFilter,
                        active:
                            activeFilter,
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
                    pageData?.first ?? true
                );

                setLast(
                    pageData?.last ?? true
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
            isAdmin,
            selectedBranchId,
            searchFilter,
            movementTypeFilter,
            referenceNumberFilter,
            createdByFilter,
            activeFilter,
            startDateFilter,
            endDateFilter,
            page,
            size
        ]);

    /**
     * Cargar movimientos al cambiar sucursal,
     * página, tamaño o filtros aplicados.
     */
    useEffect(() => {
        loadMovements();
    }, [loadMovements]);

    /**
     * Carga el resumen mensual.
     */
    const loadMonthlySummary =
        useCallback(async () => {
            if (!canRead) {
                return;
            }

            if (
                isAdmin &&
                !selectedBranchId
            ) {
                setMonthlySummary(null);
                setSummaryError("");
                setLoadingSummary(false);
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
                    await getInventoryMonthlySummary({
                        branchId:
                            isAdmin
                                ? selectedBranchId
                                : "",
                        year:
                            numericYear,
                        month:
                            numericMonth
                    });

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
            isAdmin,
            selectedBranchId,
            summaryYear,
            summaryMonth
        ]);

    /**
     * Carga el resumen automáticamente después de
     * seleccionar una sucursal o cambiar el período.
     */
    useEffect(() => {
        loadMonthlySummary();
    }, [loadMonthlySummary]);

    /**
     * Valida el rango de fechas de la bitácora.
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
            Object.keys(
                newErrors
            ).length === 0
        );
    };

    /**
     * Aplica los filtros.
     */
    const handleSearch =
        event => {
            event.preventDefault();

            if (
                isAdmin &&
                !selectedBranchId
            ) {
                setBranchError(
                    "Debe seleccionar una sucursal."
                );

                return;
            }

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
     * Limpia solamente los filtros de la bitácora.
     *
     * La sucursal seleccionada se conserva.
     */
    const clearFilters = () => {
        setSearchFilter("");
        setMovementTypeFilter("");
        setReferenceNumberFilter("");
        setCreatedByFilter("");
        setActiveFilter("");
        setStartDateFilter("");
        setEndDateFilter("");
        setFilterErrors({});
        setPage(0);
    };

    /**
     * Cambia los registros mostrados por página.
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
     * Consulta manualmente el resumen.
     */
    const handleSummarySearch =
        event => {
            event.preventDefault();

            if (
                isAdmin &&
                !selectedBranchId
            ) {
                setBranchError(
                    "Debe seleccionar una sucursal."
                );

                return;
            }

            loadMonthlySummary();
        };

    /**
     * Cambia la sucursal de contexto para ADMIN.
     */
    const handleBranchChange =
        event => {
            const newBranchId =
                event.target.value;

            setSelectedBranchId(
                newBranchId
            );

            setPage(0);
            setMovements([]);
            setMonthlySummary(null);
            setSummaryError("");
            setBranchError("");
            setFilterErrors({});
        };

    /**
     * Actualiza manualmente la bitácora.
     */
    const handleRefresh = () => {
        if (
            isAdmin &&
            !selectedBranchId
        ) {
            setBranchError(
                "Debe seleccionar una sucursal."
            );

            return;
        }

        loadMovements();
    };

    /**
     * Pantalla defensiva para un usuario
     * sin permiso de lectura.
     */
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

            {/*
             * Selector de sucursal para ADMIN.
             */}
            {isAdmin && (
                <div
                    className="card"
                    style={{
                        marginBottom:
                            "16px"
                    }}
                >
                    <h2 className="section-title">
                        Sucursal de Consulta
                    </h2>

                    <div className="form-grid">
                        <div className="form-group">
                            <label>
                                Sucursal
                            </label>

                            <select
                                value={
                                    selectedBranchId
                                }
                                onChange={
                                    handleBranchChange
                                }
                                disabled={
                                    loadingBranches
                                }
                            >
                                <option value="">
                                    Seleccione una sucursal
                                </option>

                                {branches.map(
                                    branch => (
                                        <option
                                            key={
                                                branch.id
                                            }
                                            value={
                                                branch.id
                                            }
                                        >
                                            {
                                                branch.name
                                            }
                                        </option>
                                    )
                                )}
                            </select>

                            {loadingBranches && (
                                <small>
                                    Cargando sucursales...
                                </small>
                            )}

                            {branchError && (
                                <div className="error-message">
                                    {branchError}
                                </div>
                            )}
                        </div>
                    </div>

                    {!selectedBranchId &&
                        !loadingBranches &&
                        !branchError && (
                            <div
                                style={{
                                    marginTop:
                                        "10px",
                                    padding:
                                        "12px",
                                    borderRadius:
                                        "6px",
                                    backgroundColor:
                                        "#eff6ff",
                                    border:
                                        "1px solid #bfdbfe",
                                    color:
                                        "#1e3a8a"
                                }}
                            >
                                Seleccione una sucursal para consultar la bitácora y el resumen mensual.
                            </div>
                        )}
                </div>
            )}

            {/*
             * Información de sucursal para FARMACIA.
             */}
            {isPharmacy && (
                <div
                    style={{
                        marginBottom:
                            "16px",
                        padding:
                            "12px",
                        borderRadius:
                            "6px",
                        backgroundColor:
                            "#f8fafc",
                        border:
                            "1px solid #e2e8f0",
                        color:
                            "#475569"
                    }}
                >
                    <strong>
                        Sucursal:
                    </strong>

                    {" "}

                    {user?.branchName ||
                        "Sucursal asignada"}
                </div>
            )}

            {/*
             * Acciones principales.
             */}
            <div
                className="button-row"
                style={{
                    marginBottom:
                        "16px"
                }}
            >
                {isPharmacy && (
                    <Link
                        to="/pharmacy/inventory"
                        className="btn btn-secondary"
                    >
                        Volver al inventario
                    </Link>
                )}

                {canCreate && (
                    <Link
                        to="/pharmacy/inventory-movements/create"
                        className="btn btn-primary"
                    >
                        Registrar movimiento
                    </Link>
                )}

                <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={
                        handleRefresh
                    }
                    disabled={
                        loading ||
                        (
                            isAdmin &&
                            !selectedBranchId
                        )
                    }
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
                                            event.target
                                                .value
                                        )
                                }
                                disabled={
                                    loadingSummary ||
                                    (
                                        isAdmin &&
                                        !selectedBranchId
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
                                                event.target
                                                    .value
                                            )
                                        )
                                }
                                disabled={
                                    loadingSummary ||
                                    (
                                        isAdmin &&
                                        !selectedBranchId
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
                                loadingSummary ||
                                (
                                    isAdmin &&
                                    !selectedBranchId
                                )
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
                            marginTop:
                                "12px"
                        }}
                    >
                        {summaryError}
                    </div>
                )}

                {isAdmin &&
                    !selectedBranchId ? (
                    <p
                        style={{
                            color:
                                "#64748b"
                        }}
                    >
                        Seleccione una sucursal para consultar el resumen mensual.
                    </p>

                ) : loadingSummary ? (
                    <p>
                        Cargando resumen mensual...
                    </p>

                ) : monthlySummary ? (
                    <MonthlySummary
                        summary={
                            monthlySummary
                        }
                    />

                ) : (
                    <p
                        style={{
                            color:
                                "#64748b"
                        }}
                    >
                        No hay información de resumen disponible.
                    </p>
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
                                placeholder="Código, nombre, principio activo o motivo"
                                value={
                                    searchFilter
                                }
                                onChange={
                                    event =>
                                        setSearchFilter(
                                            event.target
                                                .value
                                        )
                                }
                                disabled={
                                    isAdmin &&
                                    !selectedBranchId
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
                                            event.target
                                                .value
                                        )
                                }
                                disabled={
                                    isAdmin &&
                                    !selectedBranchId
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
                                Número de referencia
                            </label>

                            <input
                                type="text"
                                maxLength={100}
                                placeholder="Ejemplo: FAC-2026-001"
                                value={
                                    referenceNumberFilter
                                }
                                onChange={
                                    event =>
                                        setReferenceNumberFilter(
                                            event.target
                                                .value
                                        )
                                }
                                disabled={
                                    isAdmin &&
                                    !selectedBranchId
                                }
                            />
                        </div>

                        <div className="form-group">
                            <label>
                                Usuario
                            </label>

                            <input
                                type="text"
                                maxLength={100}
                                placeholder="Usuario que registró el movimiento"
                                value={
                                    createdByFilter
                                }
                                onChange={
                                    event =>
                                        setCreatedByFilter(
                                            event.target
                                                .value
                                        )
                                }
                                disabled={
                                    isAdmin &&
                                    !selectedBranchId
                                }
                            />
                        </div>

                        <div className="form-group">
                            <label>
                                Estado administrativo
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
                                disabled={
                                    isAdmin &&
                                    !selectedBranchId
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
                                            event.target
                                                .value
                                        );

                                        setFilterErrors(
                                            {}
                                        );
                                    }
                                }
                                disabled={
                                    isAdmin &&
                                    !selectedBranchId
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
                                            event.target
                                                .value
                                        );

                                        setFilterErrors(
                                            {}
                                        );
                                    }
                                }
                                disabled={
                                    isAdmin &&
                                    !selectedBranchId
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
                                disabled={
                                    isAdmin &&
                                    !selectedBranchId
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
                            disabled={
                                loading ||
                                (
                                    isAdmin &&
                                    !selectedBranchId
                                )
                            }
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
                            disabled={
                                loading ||
                                (
                                    isAdmin &&
                                    !selectedBranchId
                                )
                            }
                        >
                            Limpiar filtros
                        </button>
                    </div>
                </form>

                <br />

                {isAdmin &&
                    !selectedBranchId ? (
                    <p
                        style={{
                            color:
                                "#64748b"
                        }}
                    >
                        Seleccione una sucursal para consultar los movimientos.
                    </p>

                ) : loading ? (
                    <p>
                        Cargando movimientos...
                    </p>

                ) : movements.length === 0 ? (
                    <p
                        style={{
                            color:
                                "#64748b"
                        }}
                    >
                        No existen movimientos que coincidan con los filtros seleccionados.
                    </p>

                ) : (
                    <div
                        style={{
                            overflowX:
                                "auto"
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
                                        Costo unitario
                                    </th>

                                    <th>
                                        Costo total
                                    </th>

                                    <th>
                                        Referencia
                                    </th>

                                    <th>
                                        Motivo
                                    </th>

                                    <th>
                                        Usuario
                                    </th>

                                    <th>
                                        Estado
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
                                                        .unitCost != null
                                                        ? formatCurrency(
                                                            movement
                                                                .unitCost
                                                        )
                                                        : "No aplica"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    movement
                                                        .totalCost != null
                                                        ? formatCurrency(
                                                            movement
                                                                .totalCost
                                                        )
                                                        : "No aplica"
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
                                                        .reason ||
                                                    "Sin motivo"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    movement
                                                        .createdBy ||
                                                    "Sin información"
                                                }
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        movement.active === false
                                                            ? "status-inactive"
                                                            : "status-active"
                                                    }
                                                >
                                                    {movement.active === false
                                                        ? "Inactivo"
                                                        : "Activo"}
                                                </span>
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
                        marginTop:
                            "18px",
                        display:
                            "flex",
                        justifyContent:
                            "space-between",
                        alignItems:
                            "center",
                        gap:
                            "12px",
                        flexWrap:
                            "wrap"
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
                                loading ||
                                (
                                    isAdmin &&
                                    !selectedBranchId
                                )
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
                                totalPages === 0 ||
                                (
                                    isAdmin &&
                                    !selectedBranchId
                                )
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
                marginTop:
                    "18px"
            }}
        >
            <div
                style={{
                    padding:
                        "12px",
                    borderRadius:
                        "6px",
                    backgroundColor:
                        "#f8fafc",
                    color:
                        "#334155",
                    marginBottom:
                        "16px"
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
                    display:
                        "grid",
                    gridTemplateColumns:
                        "repeat(auto-fit, minmax(180px, 1fr))",
                    gap:
                        "12px",
                    marginBottom:
                        "18px"
                }}
            >
                <SummaryCard
                    title="Movimientos"
                    value={
                        summary
                            .totalMovements ??
                        0
                    }
                    color="#1d4ed8"
                />

                <SummaryCard
                    title="Entradas"
                    value={
                        summary
                            .totalEntries ??
                        0
                    }
                    color="#15803d"
                />

                <SummaryCard
                    title="Salidas manuales"
                    value={
                        summary
                            .totalOutputs ??
                        0
                    }
                    color="#b45309"
                />

                <SummaryCard
                    title="Despachado"
                    value={
                        summary
                            .totalDispensed ??
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
                        overflowX:
                            "auto"
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

/**
 * Tarjeta del resumen general.
 */
function SummaryCard({
    title,
    value,
    color
}) {
    return (
        <div
            style={{
                padding:
                    "16px",
                borderRadius:
                    "8px",
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
                    color:
                        "#64748b",
                    marginBottom:
                        "6px"
                }}
            >
                {title}
            </div>

            <div
                style={{
                    fontSize:
                        "1.5rem",
                    fontWeight:
                        700,
                    color
                }}
            >
                {value}
            </div>
        </div>
    );
}

/**
 * Etiqueta visible del tipo de movimiento.
 */
function MovementTypeBadge({
    type
}) {
    const labels = {
        STOCK_INICIAL:
            "Stock inicial",

        ENTRADA:
            "Entrada histórica",

        SALIDA:
            "Salida histórica",

        COMPRA:
            "Compra",

        DEVOLUCION:
            "Devolución",

        VENTA:
            "Venta",

        RECLAMO:
            "Reclamo",

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
            "COMPRA",
            "DEVOLUCION",
            "AJUSTE_POSITIVO",
            "REACTIVACION"
        ]);

    const negativeTypes =
        new Set([
            "SALIDA",
            "VENTA",
            "RECLAMO",
            "AJUSTE_NEGATIVO",
            "DESPACHO"
        ]);

    let className = "";

    if (
        positiveTypes.has(
            type
        )
    ) {
        className =
            "status-active";

    } else if (
        negativeTypes.has(
            type
        )
    ) {
        className =
            "status-inactive";
    }

    return (
        <span
            className={
                className
            }
            style={{
                whiteSpace:
                    "nowrap"
            }}
        >
            {labels[type] ||
                type ||
                "Sin tipo"}
        </span>
    );
}

/**
 * Cantidad firmada del movimiento.
 */
function SignedQuantity({
    value,
    unit
}) {
    const numericValue =
        Number(
            value ?? 0
        );

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
                whiteSpace:
                    "nowrap"
            }}
        >
            {positive
                ? "+"
                : ""}

            {numericValue}

            {" "}

            {unit ||
                "UNIDADES"}
        </strong>
    );
}

function PositiveValue({
    value,
    unit
}) {
    const numericValue =
        Number(
            value ?? 0
        );

    return (
        <span
            style={{
                color:
                    numericValue > 0
                        ? "#15803d"
                        : "#475569",
                whiteSpace:
                    "nowrap"
            }}
        >
            {numericValue > 0
                ? "+"
                : ""}

            {numericValue}

            {" "}

            {unit ||
                "UNIDADES"}
        </span>
    );
}

function NegativeValue({
    value,
    unit
}) {
    const numericValue =
        Number(
            value ?? 0
        );

    return (
        <span
            style={{
                color:
                    numericValue > 0
                        ? "#b91c1c"
                        : "#475569",
                whiteSpace:
                    "nowrap"
            }}
        >
            {numericValue > 0
                ? "-"
                : ""}

            {numericValue}

            {" "}

            {unit ||
                "UNIDADES"}
        </span>
    );
}

/**
 * Formatea stock y unidad.
 */
function formatStock(
    value,
    unit
) {
    return `${value ?? 0} ${unit || "UNIDADES"
        }`;
}

/**
 * Formatea el costo en quetzales.
 */
function formatCurrency(
    value
) {
    const numericValue =
        Number(value);

    if (
        Number.isNaN(
            numericValue
        )
    ) {
        return "Q 0.00";
    }

    return new Intl.NumberFormat(
        "es-GT",
        {
            style:
                "currency",
            currency:
                "GTQ",
            minimumFractionDigits:
                2,
            maximumFractionDigits:
                2
        }
    ).format(
        numericValue
    );
}

/**
 * Construye una referencia legible.
 *
 * Prioriza referenceNumber porque es la
 * referencia funcional ingresada en CU-15.
 */
function formatReference(
    movement
) {
    if (
        movement?.referenceNumber
    ) {
        return movement
            .referenceNumber;
    }

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

/**
 * Formatea fecha y hora.
 */
function formatDateTime(
    value
) {
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
                day:
                    "2-digit",
                month:
                    "2-digit",
                year:
                    "numeric",
                hour:
                    "2-digit",
                minute:
                    "2-digit"
            }
        );
}

/**
 * Formatea una fecha ISO sin alterar el día
 * por diferencias de zona horaria.
 */
function formatDate(
    value
) {
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