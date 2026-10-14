import {
    useCallback,
    useEffect,
    useState
} from "react";

import {
    Link,
    useNavigate,
    useParams
} from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import {
    changeInventoryMovementStatus,
    getInventoryMovementDetail
} from "../../api/inventoryMovementApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

function InventoryMovementDetail() {
    const {
        movementId
    } = useParams();

    const navigate =
        useNavigate();

    const {
        hasPermission
    } = useAuth();

    /**
     * Permisos.
     */
    const canRead =
        hasPermission(
            "INVENTORY_MOVEMENT_READ"
        );

    const canChangeStatus =
        hasPermission(
            "INVENTORY_MOVEMENT_STATUS_UPDATE"
        );

    /**
     * Movimiento recuperado del backend.
     */
    const [
        movement,
        setMovement
    ] = useState(null);

    const [
        loading,
        setLoading
    ] = useState(true);

    const [
        changingStatus,
        setChangingStatus
    ] = useState(false);

    const [
        errorMessage,
        setErrorMessage
    ] = useState("");

    /**
     * Carga el detalle del movimiento.
     */
    const loadMovement =
        useCallback(async () => {
            if (!canRead) {
                setLoading(false);
                return;
            }

            const numericMovementId =
                Number(movementId);

            if (
                !Number.isInteger(
                    numericMovementId
                ) ||
                numericMovementId <= 0
            ) {
                setErrorMessage(
                    "El identificador del movimiento no es válido."
                );

                setMovement(null);
                setLoading(false);

                return;
            }

            try {
                setLoading(true);
                setErrorMessage("");

                const response =
                    await getInventoryMovementDetail(
                        numericMovementId
                    );

                setMovement(
                    response.data
                );

            } catch (error) {
                console.error(
                    "Error al cargar detalle del movimiento:",
                    error
                );

                setErrorMessage(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar el detalle del movimiento."
                );

                setMovement(null);

            } finally {
                setLoading(false);
            }
        }, [
            canRead,
            movementId
        ]);

    useEffect(() => {
        loadMovement();
    }, [loadMovement]);

    /**
     * Activa o desactiva administrativamente
     * el movimiento.
     *
     * La operación no modifica el inventario.
     */
    const handleStatusChange =
        async () => {
            if (
                !canChangeStatus ||
                !movement
            ) {
                return;
            }

            const newStatus =
                !Boolean(
                    movement.active
                );

            const actionText =
                newStatus
                    ? "reactivar"
                    : "desactivar";

            const confirmed =
                window.confirm(
                    `¿Desea ${actionText} el movimiento #${movement.movementId}?\n\n`
                    + "Esta operación no modificará el stock del medicamento."
                );

            if (!confirmed) {
                return;
            }

            try {
                setChangingStatus(true);

                const response =
                    await changeInventoryMovementStatus(
                        movement.movementId,
                        newStatus,
                        movement.version
                    );

                setMovement(
                    response.data
                );

                alert(
                    response.data
                        ?.message ||
                    (
                        newStatus
                            ? "Movimiento reactivado correctamente."
                            : "Movimiento desactivado correctamente."
                    )
                );

            } catch (error) {
                console.error(
                    "Error al cambiar estado del movimiento:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cambiar el estado del movimiento."
                );

                await loadMovement();

            } finally {
                setChangingStatus(false);
            }
        };

    if (!canRead) {
        return (
            <MainLayout>
                <h1 className="page-title">
                    Detalle del Movimiento
                </h1>

                <div className="card">
                    <p>
                        No tiene permiso para consultar movimientos de inventario.
                    </p>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <h1 className="page-title">
                Detalle del Movimiento
            </h1>

            <div
                className="button-row"
                style={{
                    marginBottom: "16px"
                }}
            >
                <Link
                    to="/pharmacy/inventory-movements"
                    className="btn btn-secondary"
                >
                    Volver a la bitácora
                </Link>

                <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={
                        loadMovement
                    }
                    disabled={
                        loading ||
                        changingStatus
                    }
                >
                    {loading
                        ? "Actualizando..."
                        : "Actualizar detalle"}
                </button>
            </div>

            {errorMessage && (
                <div className="card">
                    <div className="error-message">
                        {errorMessage}
                    </div>

                    <div
                        className="button-row"
                        style={{
                            marginTop: "14px"
                        }}
                    >
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() =>
                                navigate(
                                    "/pharmacy/inventory-movements"
                                )
                            }
                        >
                            Regresar
                        </button>
                    </div>
                </div>
            )}

            {loading ? (
                <div className="card">
                    <p>
                        Cargando detalle del movimiento...
                    </p>
                </div>

            ) : movement ? (
                <>
                    <div className="card">
                        <div
                            style={{
                                display: "flex",
                                justifyContent:
                                    "space-between",
                                alignItems: "center",
                                gap: "12px",
                                flexWrap: "wrap"
                            }}
                        >
                            <div>
                                <h2 className="section-title">
                                    Movimiento #{
                                        movement.movementId
                                    }
                                </h2>

                                <MovementTypeBadge
                                    type={
                                        movement.movementType
                                    }
                                />
                            </div>

                            <span
                                className={
                                    movement.active
                                        ? "status-active"
                                        : "status-inactive"
                                }
                            >
                                {movement.active
                                    ? "Activo"
                                    : "Inactivo"}
                            </span>
                        </div>

                        <div
                            style={{
                                marginTop: "12px",
                                padding: "12px",
                                borderRadius: "6px",
                                backgroundColor:
                                    movement.operationType ===
                                        "OUTPUT"
                                        ? "#fff7ed"
                                        : "#f0fdf4",
                                border:
                                    movement.operationType ===
                                        "OUTPUT"
                                        ? "1px solid #fdba74"
                                        : "1px solid #86efac",
                                color:
                                    movement.operationType ===
                                        "OUTPUT"
                                        ? "#9a3412"
                                        : "#166534"
                            }}
                        >
                            <strong>
                                Operación:
                            </strong>

                            {" "}

                            {movement.operationType ===
                                "OUTPUT"
                                ? "Salida de inventario"
                                : "Entrada de inventario"}

                            {" | "}

                            <strong>
                                Movimiento:
                            </strong>

                            {" "}

                            <SignedQuantity
                                value={
                                    movement.signedQuantity
                                }
                                unit={
                                    movement.medicationUnit
                                }
                            />
                        </div>
                    </div>

                    <br />

                    <div className="card">
                        <h2 className="section-title">
                            Medicamento y Sucursal
                        </h2>

                        <div className="form-grid">
                            <DetailValue
                                label="Sucursal"
                                value={
                                    movement.branchName
                                }
                            />

                            <DetailValue
                                label="Código"
                                value={
                                    movement.medicationCode
                                }
                            />

                            <DetailValue
                                label="Medicamento"
                                value={
                                    movement.medicationName
                                }
                            />

                            <DetailValue
                                label="Principio activo"
                                value={
                                    movement.activeIngredient
                                }
                            />

                            <DetailValue
                                label="Concentración"
                                value={
                                    movement.concentration
                                }
                            />

                            <DetailValue
                                label="Presentación"
                                value={
                                    movement.presentation
                                }
                            />

                            <DetailValue
                                label="Unidad"
                                value={
                                    movement.medicationUnit
                                }
                            />

                            <DetailValue
                                label="ID de inventario"
                                value={
                                    movement.inventoryId
                                }
                            />
                        </div>
                    </div>

                    <br />

                    <div className="card">
                        <h2 className="section-title">
                            Efecto en Existencias
                        </h2>

                        <div
                            style={{
                                display: "grid",
                                gridTemplateColumns:
                                    "repeat(auto-fit, minmax(180px, 1fr))",
                                gap: "14px"
                            }}
                        >
                            <StockCard
                                title="Stock anterior"
                                value={
                                    movement.previousStock
                                }
                                unit={
                                    movement.medicationUnit
                                }
                                color="#475569"
                            />

                            <StockCard
                                title="Cantidad absoluta"
                                value={
                                    movement.quantity
                                }
                                unit={
                                    movement.medicationUnit
                                }
                                color="#1d4ed8"
                            />

                            <StockCard
                                title="Movimiento firmado"
                                value={
                                    movement.signedQuantity
                                }
                                unit={
                                    movement.medicationUnit
                                }
                                signed
                                color={
                                    Number(
                                        movement.signedQuantity
                                    ) < 0
                                        ? "#b91c1c"
                                        : "#15803d"
                                }
                            />

                            <StockCard
                                title="Stock resultante"
                                value={
                                    movement.resultingStock
                                }
                                unit={
                                    movement.medicationUnit
                                }
                                color="#0f172a"
                            />
                        </div>
                    </div>

                    <br />

                    <div className="card">
                        <h2 className="section-title">
                            Costos y Referencia
                        </h2>

                        <div className="form-grid">
                            <DetailValue
                                label="Costo unitario"
                                value={
                                    movement.unitCost != null
                                        ? formatCurrency(
                                            movement.unitCost
                                        )
                                        : "No aplica"
                                }
                            />

                            <DetailValue
                                label="Costo total"
                                value={
                                    movement.totalCost != null
                                        ? formatCurrency(
                                            movement.totalCost
                                        )
                                        : "No aplica"
                                }
                            />

                            <DetailValue
                                label="Tipo de referencia"
                                value={
                                    movement.referenceType ||
                                    "Sin referencia"
                                }
                            />

                            <DetailValue
                                label="ID de referencia"
                                value={
                                    movement.referenceId ??
                                    "Sin referencia"
                                }
                            />

                            <DetailValue
                                label="Número de referencia"
                                value={
                                    movement.referenceNumber ||
                                    "Sin referencia"
                                }
                            />
                        </div>

                        <div
                            style={{
                                marginTop: "14px"
                            }}
                        >
                            <DetailValue
                                label="Motivo"
                                value={
                                    movement.reason ||
                                    "Sin motivo"
                                }
                            />
                        </div>
                    </div>

                    <br />

                    <div className="card">
                        <h2 className="section-title">
                            Auditoría
                        </h2>

                        <div className="form-grid">
                            <DetailValue
                                label="Registrado por"
                                value={
                                    movement.createdBy
                                }
                            />

                            <DetailValue
                                label="Fecha de registro"
                                value={
                                    formatDateTime(
                                        movement.createdAt
                                    )
                                }
                            />

                            <DetailValue
                                label="Actualizado por"
                                value={
                                    movement.updatedBy
                                }
                            />

                            <DetailValue
                                label="Última actualización"
                                value={
                                    formatDateTime(
                                        movement.updatedAt
                                    )
                                }
                            />

                            <DetailValue
                                label="Versión"
                                value={
                                    movement.version
                                }
                            />

                            <DetailValue
                                label="Estado"
                                value={
                                    movement.active
                                        ? "Activo"
                                        : "Inactivo"
                                }
                            />
                        </div>
                    </div>

                    {canChangeStatus && (
                        <>
                            <br />

                            <div className="card">
                                <h2 className="section-title">
                                    Administración del Movimiento
                                </h2>

                                <div
                                    style={{
                                        padding: "12px",
                                        borderRadius: "6px",
                                        backgroundColor:
                                            "#fff7ed",
                                        border:
                                            "1px solid #fdba74",
                                        color:
                                            "#9a3412",
                                        marginBottom: "14px"
                                    }}
                                >
                                    Activar o desactivar este registro no modifica el stock del medicamento. Para corregir existencias se debe registrar un movimiento compensatorio.
                                </div>

                                <button
                                    type="button"
                                    className={
                                        movement.active
                                            ? "btn btn-danger-outline"
                                            : "btn btn-primary"
                                    }
                                    onClick={
                                        handleStatusChange
                                    }
                                    disabled={
                                        changingStatus
                                    }
                                >
                                    {changingStatus
                                        ? "Procesando..."
                                        : (
                                            movement.active
                                                ? "Desactivar movimiento"
                                                : "Reactivar movimiento"
                                        )}
                                </button>
                            </div>
                        </>
                    )}
                </>
            ) : null}
        </MainLayout>
    );
}

function DetailValue({
    label,
    value
}) {
    return (
        <div className="form-group">
            <label>
                {label}
            </label>

            <div
                style={{
                    minHeight: "42px",
                    padding: "10px 12px",
                    borderRadius: "6px",
                    border:
                        "1px solid #e2e8f0",
                    backgroundColor:
                        "#f8fafc",
                    color:
                        "#334155",
                    overflowWrap:
                        "anywhere"
                }}
            >
                {value === null ||
                    value === undefined ||
                    value === ""
                    ? "Sin información"
                    : value}
            </div>
        </div>
    );
}

function StockCard({
    title,
    value,
    unit,
    signed = false,
    color
}) {
    const numericValue =
        Number(
            value ?? 0
        );

    const formattedValue =
        signed &&
            numericValue > 0
            ? `+${numericValue}`
            : String(
                numericValue
            );

    return (
        <div
            style={{
                padding: "16px",
                borderRadius: "8px",
                border:
                    "1px solid #e2e8f0",
                borderLeft:
                    `5px solid ${color}`,
                backgroundColor:
                    "#ffffff"
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
                    fontSize: "1.35rem",
                    fontWeight: 700,
                    color
                }}
            >
                {formattedValue}
                {" "}
                {unit || "UNIDADES"}
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

    const outputTypes =
        new Set([
            "SALIDA",
            "VENTA",
            "RECLAMO",
            "AJUSTE_NEGATIVO",
            "DESPACHO"
        ]);

    return (
        <span
            className={
                outputTypes.has(type)
                    ? "status-inactive"
                    : "status-active"
            }
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
        Number(
            value ?? 0
        );

    return (
        <strong>
            {numericValue > 0
                ? "+"
                : ""}
            {numericValue}
            {" "}
            {unit || "UNIDADES"}
        </strong>
    );
}

function formatCurrency(
    value
) {
    const numericValue =
        Number(value);

    return new Intl.NumberFormat(
        "es-GT",
        {
            style: "currency",
            currency: "GTQ",
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }
    ).format(
        Number.isNaN(
            numericValue
        )
            ? 0
            : numericValue
    );
}

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
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit"
            }
        );
}

export default InventoryMovementDetail;