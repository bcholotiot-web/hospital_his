import {
    useCallback,
    useEffect,
    useMemo,
    useState
} from "react";

import {
    Link,
    useNavigate
} from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import {
    getActiveBranches
} from "../../api/branchApi";

import {
    getActiveMedications
} from "../../api/medicationAdminApi";

import {
    getPharmacyMedications
} from "../../api/pharmacyInventoryApi";

import {
    createInventoryMovement,
    previewInventoryMovement
} from "../../api/inventoryMovementApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

/**
 * Tipos que puede registrar manualmente
 * el rol FARMACIA.
 */
const PHARMACY_MOVEMENT_TYPES = [
    {
        value: "COMPRA",
        label: "Compra",
        operation: "Entrada"
    },
    {
        value: "DEVOLUCION",
        label: "Devolución",
        operation: "Entrada"
    },
    {
        value: "VENTA",
        label: "Venta",
        operation: "Salida"
    },
    {
        value: "RECLAMO",
        label: "Reclamo",
        operation: "Salida"
    },
    {
        value: "AJUSTE_POSITIVO",
        label: "Ajuste positivo",
        operation: "Entrada"
    },
    {
        value: "AJUSTE_NEGATIVO",
        label: "Ajuste negativo",
        operation: "Salida"
    }
];

/**
 * Para ADMIN se limita inicialmente
 * a movimientos de ajuste.
 */
const ADMIN_MOVEMENT_TYPES = [
    {
        value: "AJUSTE_POSITIVO",
        label: "Ajuste positivo",
        operation: "Entrada"
    },
    {
        value: "AJUSTE_NEGATIVO",
        label: "Ajuste negativo",
        operation: "Salida"
    }
];

function CreateInventoryMovement() {
    const navigate =
        useNavigate();

    const {
        user,
        hasRole,
        hasPermission
    } = useAuth();

    const isAdmin =
        hasRole("ADMIN") ||
        hasRole("ADMINISTRADOR");

    const isPharmacy =
        hasRole("FARMACIA");

    const canCreate =
        hasPermission(
            "INVENTORY_MOVEMENT_CREATE"
        );

    /**
     * Catálogos.
     */
    const [
        branches,
        setBranches
    ] = useState([]);

    const [
        medications,
        setMedications
    ] = useState([]);

    const [
        loadingCatalogs,
        setLoadingCatalogs
    ] = useState(false);

    /**
     * Formulario.
     */
    const [
        branchId,
        setBranchId
    ] = useState("");

    const [
        medicationId,
        setMedicationId
    ] = useState("");

    const [
        movementType,
        setMovementType
    ] = useState("");

    const [
        quantity,
        setQuantity
    ] = useState("");

    const [
        unitCost,
        setUnitCost
    ] = useState("");

    const [
        referenceNumber,
        setReferenceNumber
    ] = useState("");

    const [
        reason,
        setReason
    ] = useState("");

    const [
        errors,
        setErrors
    ] = useState({});

    /**
     * Vista previa calculada por Spring Boot.
     */
    const [
        preview,
        setPreview
    ] = useState(null);

    const [
        previewing,
        setPreviewing
    ] = useState(false);

    const [
        saving,
        setSaving
    ] = useState(false);

    /**
     * Tipos permitidos según rol.
     */
    const movementTypes =
        useMemo(() => {
            return isAdmin
                ? ADMIN_MOVEMENT_TYPES
                : PHARMACY_MOVEMENT_TYPES;
        }, [isAdmin]);

    /**
     * Configuración dinámica por tipo.
     */
    const movementConfiguration =
        useMemo(() => {
            return getMovementConfiguration(
                movementType
            );
        }, [movementType]);

    /**
     * Medicamento seleccionado.
     */
    const selectedMedication =
        useMemo(() => {
            return medications.find(
                medication =>
                    Number(
                        getMedicationId(
                            medication
                        )
                    ) ===
                    Number(
                        medicationId
                    )
            ) || null;
        }, [
            medications,
            medicationId
        ]);

    /**
     * Cargar catálogos.
     *
     * ADMIN utiliza el catálogo administrativo.
     * FARMACIA utiliza el catálogo operativo.
     */
    const loadCatalogs =
        useCallback(async () => {
            try {
                setLoadingCatalogs(
                    true
                );

                if (isAdmin) {
                    const [
                        branchesResponse,
                        medicationsResponse
                    ] = await Promise.all([
                        getActiveBranches(),
                        getActiveMedications()
                    ]);

                    setBranches(
                        Array.isArray(
                            branchesResponse.data
                        )
                            ? branchesResponse.data
                            : []
                    );

                    setMedications(
                        Array.isArray(
                            medicationsResponse.data
                        )
                            ? medicationsResponse.data
                            : []
                    );

                } else if (isPharmacy) {
                    const medicationsResponse =
                        await getPharmacyMedications();

                    setBranches([]);
                    setMedications(
                        Array.isArray(
                            medicationsResponse.data
                        )
                            ? medicationsResponse.data
                            : []
                    );
                }

            } catch (error) {
                console.error(
                    "Error al cargar catálogos:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar los catálogos requeridos."
                );

            } finally {
                setLoadingCatalogs(
                    false
                );
            }
        }, [
            isAdmin,
            isPharmacy
        ]);

    useEffect(() => {
        if (canCreate) {
            loadCatalogs();
        }
    }, [
        canCreate,
        loadCatalogs
    ]);

    /**
     * Limpiar la vista previa cuando cambien
     * datos que afectan el cálculo.
     */
    useEffect(() => {
        setPreview(null);
    }, [
        branchId,
        medicationId,
        movementType,
        quantity,
        unitCost
    ]);

    /**
     * Al cambiar tipo, limpiar los campos
     * que dejan de aplicar.
     */
    const handleMovementTypeChange =
        event => {
            const newType =
                event.target.value;

            setMovementType(
                newType
            );

            const configuration =
                getMovementConfiguration(
                    newType
                );

            if (
                !configuration
                    .showUnitCost
            ) {
                setUnitCost("");
            }

            if (
                !configuration
                    .showReason
            ) {
                setReason("");
            }

            setReferenceNumber("");
            setErrors({});
            setPreview(null);
        };

    /**
     * Validar campos básicos antes de
     * solicitar la vista previa.
     */
    const validatePreviewFields = () => {
        const newErrors = {};

        if (
            isAdmin &&
            !branchId
        ) {
            newErrors.branchId =
                "Debe seleccionar una sucursal.";
        }

        if (!medicationId) {
            newErrors.medicationId =
                "Debe seleccionar un medicamento.";
        }

        if (!movementType) {
            newErrors.movementType =
                "Debe seleccionar el tipo de movimiento.";
        }

        const numericQuantity =
            Number(quantity);

        if (!quantity) {
            newErrors.quantity =
                "La cantidad es obligatoria.";

        } else if (
            !Number.isInteger(
                numericQuantity
            ) ||
            numericQuantity <= 0
        ) {
            newErrors.quantity =
                "La cantidad debe ser un número entero positivo.";
        }

        if (
            movementConfiguration
                .showUnitCost
        ) {
            const numericCost =
                Number(unitCost);

            if (!unitCost) {
                newErrors.unitCost =
                    "El costo unitario es obligatorio para compras.";

            } else if (
                Number.isNaN(
                    numericCost
                ) ||
                numericCost <= 0
            ) {
                newErrors.unitCost =
                    "El costo unitario debe ser mayor a 0.";

            } else if (
                !hasMaximumTwoDecimals(
                    String(unitCost)
                )
            ) {
                newErrors.unitCost =
                    "El costo unitario debe tener máximo 2 decimales.";
            }
        }

        setErrors(newErrors);

        return (
            Object.keys(
                newErrors
            ).length === 0
        );
    };

    /**
     * Validación completa antes de guardar.
     */
    const validateCompleteForm = () => {
        if (!validatePreviewFields()) {
            return false;
        }

        const newErrors = {};

        const cleanReason =
            reason.trim();

        if (
            movementConfiguration
                .reasonRequired
        ) {
            if (!cleanReason) {
                newErrors.reason =
                    "El motivo es obligatorio para el tipo seleccionado.";

            } else if (
                cleanReason.length < 10
            ) {
                newErrors.reason =
                    "El motivo debe contener al menos 10 caracteres.";
            }
        }

        if (
            cleanReason.length > 500
        ) {
            newErrors.reason =
                "El motivo no puede exceder los 500 caracteres.";
        }

        if (
            referenceNumber
                .trim()
                .length > 100
        ) {
            newErrors.referenceNumber =
                "La referencia no puede exceder los 100 caracteres.";
        }

        setErrors(
            currentErrors => ({
                ...currentErrors,
                ...newErrors
            })
        );

        return (
            Object.keys(
                newErrors
            ).length === 0
        );
    };

    /**
     * Solicitar cálculo proyectado.
     */
    const handlePreview =
        async event => {
            event.preventDefault();

            if (
                !validatePreviewFields()
            ) {
                return;
            }

            try {
                setPreviewing(true);

                const response =
                    await previewInventoryMovement({
                        branchId:
                            isAdmin
                                ? branchId
                                : "",
                        medicationId,
                        movementType,
                        quantity,
                        unitCost:
                            movementConfiguration
                                .showUnitCost
                                ? unitCost
                                : ""
                    });

                setPreview(
                    response.data
                );

                setErrors({});

            } catch (error) {
                console.error(
                    "Error al calcular vista previa:",
                    error
                );

                setPreview(null);

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible calcular el stock proyectado."
                );

            } finally {
                setPreviewing(false);
            }
        };

    /**
     * Guardar el movimiento.
     */
    const handleSubmit =
        async event => {
            event.preventDefault();

            if (
                !validateCompleteForm()
            ) {
                return;
            }

            if (!preview) {
                alert(
                    "Debe calcular la vista previa antes de registrar el movimiento."
                );

                return;
            }

            const confirmed =
                window.confirm(
                    buildConfirmationMessage(
                        preview
                    )
                );

            if (!confirmed) {
                return;
            }

            try {
                setSaving(true);

                const response =
                    await createInventoryMovement({
                        branchId:
                            isAdmin
                                ? branchId
                                : "",
                        medicationId,
                        movementType,
                        quantity,
                        unitCost:
                            movementConfiguration
                                .showUnitCost
                                ? unitCost
                                : "",
                        referenceNumber,
                        reason,
                        inventoryVersion:
                            preview.inventoryVersion
                    });

                alert(
                    response.data
                        ?.message ||
                    "Movimiento registrado correctamente."
                );

                navigate(
                    `/pharmacy/inventory-movements/${response.data.movementId}`
                );

            } catch (error) {
                console.error(
                    "Error al registrar movimiento:",
                    error
                );

                setPreview(null);

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible registrar el movimiento. Calcule nuevamente la vista previa."
                );

            } finally {
                setSaving(false);
            }
        };

    if (!canCreate) {
        return (
            <MainLayout>
                <h1 className="page-title">
                    Registrar Movimiento
                </h1>

                <div className="card">
                    <p>
                        No tiene permiso para registrar movimientos de inventario.
                    </p>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <h1 className="page-title">
                Registrar Movimiento de Inventario
            </h1>

            <div
                className="button-row"
                style={{
                    marginBottom:
                        "16px"
                }}
            >
                <Link
                    to="/pharmacy/inventory-movements"
                    className="btn btn-secondary"
                >
                    Volver a la bitácora
                </Link>
            </div>

            <div className="card">
                <h2 className="section-title">
                    Datos del movimiento
                </h2>

                <form
                    onSubmit={
                        handleSubmit
                    }
                >
                    <div className="form-grid">
                        {isAdmin && (
                            <div className="form-group">
                                <label>
                                    Sucursal
                                </label>

                                <select
                                    value={
                                        branchId
                                    }
                                    disabled={
                                        loadingCatalogs ||
                                        saving
                                    }
                                    className={
                                        errors.branchId
                                            ? "input-error"
                                            : ""
                                    }
                                    onChange={
                                        event =>
                                            setBranchId(
                                                event.target
                                                    .value
                                            )
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

                                {errors.branchId && (
                                    <div className="error-message">
                                        {
                                            errors.branchId
                                        }
                                    </div>
                                )}
                            </div>
                        )}

                        {!isAdmin && (
                            <div className="form-group">
                                <label>
                                    Sucursal
                                </label>

                                <input
                                    type="text"
                                    value={
                                        user?.branchName ||
                                        "Sucursal asignada"
                                    }
                                    disabled
                                />
                            </div>
                        )}

                        <div className="form-group">
                            <label>
                                Medicamento
                            </label>

                            <select
                                value={
                                    medicationId
                                }
                                disabled={
                                    loadingCatalogs ||
                                    saving
                                }
                                className={
                                    errors.medicationId
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setMedicationId(
                                            event.target
                                                .value
                                        )
                                }
                            >
                                <option value="">
                                    Seleccione un medicamento
                                </option>

                                {medications.map(
                                    medication => {
                                        const id =
                                            getMedicationId(
                                                medication
                                            );

                                        return (
                                            <option
                                                key={id}
                                                value={id}
                                            >
                                                {
                                                    medication.code
                                                }
                                                {" | "}
                                                {
                                                    medication.name
                                                }
                                                {
                                                    medication.concentration
                                                        ? ` | ${medication.concentration}`
                                                        : ""
                                                }
                                            </option>
                                        );
                                    }
                                )}
                            </select>

                            {errors.medicationId && (
                                <div className="error-message">
                                    {
                                        errors.medicationId
                                    }
                                </div>
                            )}
                        </div>

                        <div className="form-group">
                            <label>
                                Tipo de movimiento
                            </label>

                            <select
                                value={
                                    movementType
                                }
                                disabled={saving}
                                className={
                                    errors.movementType
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    handleMovementTypeChange
                                }
                            >
                                <option value="">
                                    Seleccione un tipo
                                </option>

                                {movementTypes.map(
                                    type => (
                                        <option
                                            key={
                                                type.value
                                            }
                                            value={
                                                type.value
                                            }
                                        >
                                            {type.label}
                                            {" | "}
                                            {type.operation}
                                        </option>
                                    )
                                )}
                            </select>

                            {errors.movementType && (
                                <div className="error-message">
                                    {
                                        errors.movementType
                                    }
                                </div>
                            )}
                        </div>

                        <div className="form-group">
                            <label>
                                Cantidad
                            </label>

                            <input
                                type="number"
                                min="1"
                                step="1"
                                placeholder="Ejemplo: 20"
                                value={quantity}
                                disabled={saving}
                                className={
                                    errors.quantity
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setQuantity(
                                            event.target
                                                .value
                                        )
                                }
                            />

                            {errors.quantity && (
                                <div className="error-message">
                                    {errors.quantity}
                                </div>
                            )}
                        </div>

                        {movementConfiguration
                            .showUnitCost && (
                                <div className="form-group">
                                    <label>
                                        Costo unitario
                                    </label>

                                    <input
                                        type="number"
                                        min="0.01"
                                        step="0.01"
                                        placeholder="0.00"
                                        value={unitCost}
                                        disabled={saving}
                                        className={
                                            errors.unitCost
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setUnitCost(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.unitCost && (
                                        <div className="error-message">
                                            {errors.unitCost}
                                        </div>
                                    )}
                                </div>
                            )}

                        {movementConfiguration
                            .showReference && (
                                <div className="form-group">
                                    <label>
                                        {
                                            movementConfiguration
                                                .referenceLabel
                                        }
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={100}
                                        placeholder={
                                            movementConfiguration
                                                .referencePlaceholder
                                        }
                                        value={
                                            referenceNumber
                                        }
                                        disabled={saving}
                                        className={
                                            errors.referenceNumber
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setReferenceNumber(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.referenceNumber && (
                                        <div className="error-message">
                                            {
                                                errors.referenceNumber
                                            }
                                        </div>
                                    )}
                                </div>
                            )}

                        {movementConfiguration
                            .showReason && (
                                <div className="form-group">
                                    <label>
                                        Motivo
                                        {movementConfiguration
                                            .reasonRequired
                                            ? " *"
                                            : ""}
                                    </label>

                                    <textarea
                                        rows={4}
                                        minLength={
                                            movementConfiguration
                                                .reasonRequired
                                                ? 10
                                                : undefined
                                        }
                                        maxLength={500}
                                        placeholder={
                                            movementConfiguration
                                                .reasonPlaceholder
                                        }
                                        value={reason}
                                        disabled={saving}
                                        className={
                                            errors.reason
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setReason(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.reason && (
                                        <div className="error-message">
                                            {errors.reason}
                                        </div>
                                    )}

                                    <small>
                                        {reason.length}/500
                                    </small>
                                </div>
                            )}
                    </div>

                    {selectedMedication && (
                        <div
                            style={{
                                marginTop:
                                    "14px",
                                padding:
                                    "12px",
                                borderRadius:
                                    "7px",
                                backgroundColor:
                                    "#f8fafc",
                                color:
                                    "#475569"
                            }}
                        >
                            <strong>
                                Medicamento:
                            </strong>
                            {" "}
                            {
                                selectedMedication
                                    .code
                            }
                            {" | "}
                            {
                                selectedMedication
                                    .name
                            }

                            {selectedMedication
                                .controlled && (
                                    <>
                                        {" | "}
                                        <strong
                                            style={{
                                                color:
                                                    "#b91c1c"
                                            }}
                                        >
                                            CONTROLADO
                                        </strong>
                                    </>
                                )}
                        </div>
                    )}

                    <div className="button-row">
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={
                                handlePreview
                            }
                            disabled={
                                previewing ||
                                saving
                            }
                        >
                            {previewing
                                ? "Calculando..."
                                : "Calcular vista previa"}
                        </button>
                    </div>

                    {preview && (
                        <MovementPreview
                            preview={preview}
                        />
                    )}

                    <div className="button-row">
                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={
                                saving ||
                                !preview
                            }
                        >
                            {saving
                                ? "Registrando..."
                                : "Registrar movimiento"}
                        </button>
                    </div>
                </form>
            </div>
        </MainLayout>
    );
}

/**
 * Vista previa del stock.
 */
function MovementPreview({
    preview
}) {
    const isOutput =
        preview.operationType ===
        "OUTPUT";

    return (
        <div
            style={{
                marginTop:
                    "18px",
                padding:
                    "16px",
                borderRadius:
                    "8px",
                backgroundColor:
                    isOutput
                        ? "#fff7ed"
                        : "#f0fdf4",
                border:
                    isOutput
                        ? "1px solid #fdba74"
                        : "1px solid #86efac"
            }}
        >
            <h3 className="section-title">
                Vista previa
            </h3>

            <div
                style={{
                    display:
                        "grid",
                    gridTemplateColumns:
                        "repeat(auto-fit, minmax(180px, 1fr))",
                    gap:
                        "12px"
                }}
            >
                <PreviewValue
                    label="Stock actual"
                    value={
                        formatStock(
                            preview.currentStock,
                            preview.medicationUnit
                        )
                    }
                />

                <PreviewValue
                    label="Movimiento"
                    value={
                        formatSignedQuantity(
                            preview.signedQuantity,
                            preview.medicationUnit
                        )
                    }
                />

                <PreviewValue
                    label="Stock proyectado"
                    value={
                        formatStock(
                            preview.projectedStock,
                            preview.medicationUnit
                        )
                    }
                />

                <PreviewValue
                    label="Versión"
                    value={
                        preview.inventoryVersion
                    }
                />

                {preview.totalCost != null && (
                    <PreviewValue
                        label="Costo total"
                        value={
                            formatCurrency(
                                preview.totalCost
                            )
                        }
                    />
                )}
            </div>

            {preview.outOfStockWarning && (
                <div
                    className="error-message"
                    style={{
                        marginTop:
                            "12px"
                    }}
                >
                    El movimiento dejará el medicamento agotado.
                </div>
            )}

            {!preview.outOfStockWarning &&
                preview.lowStockWarning && (
                    <div
                        style={{
                            marginTop:
                                "12px",
                            color:
                                "#b45309",
                            fontWeight:
                                700
                        }}
                    >
                        El stock proyectado quedará en nivel bajo.
                    </div>
                )}
        </div>
    );
}

function PreviewValue({
    label,
    value
}) {
    return (
        <div>
            <div
                style={{
                    color:
                        "#64748b",
                    fontSize:
                        "0.85rem"
                }}
            >
                {label}
            </div>

            <strong>
                {value}
            </strong>
        </div>
    );
}

/**
 * Configuración visual y funcional por tipo.
 */
function getMovementConfiguration(
    movementType
) {
    const configurations = {
        COMPRA: {
            showUnitCost: true,
            showReference: true,
            referenceLabel:
                "Número de factura",
            referencePlaceholder:
                "Ejemplo: FAC-2026-001",
            showReason: true,
            reasonRequired: false,
            reasonPlaceholder:
                "Motivo opcional de la compra"
        },

        DEVOLUCION: {
            showUnitCost: false,
            showReference: true,
            referenceLabel:
                "Número de devolución",
            referencePlaceholder:
                "Ejemplo: DEV-2026-001",
            showReason: true,
            reasonRequired: true,
            reasonPlaceholder:
                "Explique el motivo de la devolución"
        },

        VENTA: {
            showUnitCost: false,
            showReference: true,
            referenceLabel:
                "Número de venta",
            referencePlaceholder:
                "Ejemplo: VENTA-000125",
            showReason: false,
            reasonRequired: false,
            reasonPlaceholder: ""
        },

        RECLAMO: {
            showUnitCost: false,
            showReference: true,
            referenceLabel:
                "Número de reclamo",
            referencePlaceholder:
                "Ejemplo: REC-2026-010",
            showReason: true,
            reasonRequired: true,
            reasonPlaceholder:
                "Explique el motivo del reclamo"
        },

        AJUSTE_POSITIVO: {
            showUnitCost: false,
            showReference: false,
            referenceLabel: "",
            referencePlaceholder: "",
            showReason: true,
            reasonRequired: true,
            reasonPlaceholder:
                "Explique el motivo del ajuste positivo"
        },

        AJUSTE_NEGATIVO: {
            showUnitCost: false,
            showReference: false,
            referenceLabel: "",
            referencePlaceholder: "",
            showReason: true,
            reasonRequired: true,
            reasonPlaceholder:
                "Explique el motivo del ajuste negativo"
        }
    };

    return configurations[
        movementType
    ] || {
        showUnitCost: false,
        showReference: false,
        referenceLabel: "",
        referencePlaceholder: "",
        showReason: false,
        reasonRequired: false,
        reasonPlaceholder: ""
    };
}

function getMedicationId(
    medication
) {
    return (
        medication?.medicationId ??
        medication?.id ??
        ""
    );
}

function hasMaximumTwoDecimals(
    value
) {
    return /^\d+(\.\d{1,2})?$/
        .test(
            String(value).trim()
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
        Number.isNaN(numericValue)
            ? 0
            : numericValue
    );
}

function formatStock(
    value,
    unit
) {
    return `${value ?? 0} ${unit || "UNIDADES"
        }`;
}

function formatSignedQuantity(
    value,
    unit
) {
    const numericValue =
        Number(value ?? 0);

    return `${numericValue > 0
        ? "+"
        : ""
        }${numericValue} ${unit || "UNIDADES"
        }`;
}

function buildConfirmationMessage(
    preview
) {
    return (
        `¿Desea registrar el movimiento ${preview.movementType}?\n\n`
        + `Medicamento: ${preview.medicationCode} | ${preview.medicationName}\n`
        + `Stock actual: ${preview.currentStock}\n`
        + `Movimiento: ${preview.signedQuantity > 0 ? "+" : ""}${preview.signedQuantity}\n`
        + `Stock proyectado: ${preview.projectedStock}`
    );
}

export default CreateInventoryMovement;