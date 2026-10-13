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
    changeInventoryStatus,
    createInventory,
    getInventoryAdministration,
    getPharmacyMedications,
    updateInventory
} from "../../api/pharmacyInventoryApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

function PharmacyInventory() {
    const {
        hasPermission
    } = useAuth();

    /**
     * Permisos del módulo.
     */
    const canCreate =
        hasPermission(
            "INVENTORY_CREATE"
        );

    const canUpdate =
        hasPermission(
            "INVENTORY_UPDATE"
        );

    const canChangeStatus =
        hasPermission(
            "INVENTORY_DELETE"
        );

    const canReadMovements =
        hasPermission(
            "INVENTORY_MOVEMENT_READ"
        );

    /**
     * Listados.
     */
    const [
        inventoryItems,
        setInventoryItems
    ] = useState([]);

    const [
        medications,
        setMedications
    ] = useState([]);

    /**
     * Estados de carga.
     */
    const [
        loading,
        setLoading
    ] = useState(false);

    const [
        loadingMedications,
        setLoadingMedications
    ] = useState(false);

    const [
        saving,
        setSaving
    ] = useState(false);

    /**
     * Formulario.
     */
    const [
        medicationId,
        setMedicationId
    ] = useState("");

    const [
        currentStock,
        setCurrentStock
    ] = useState("");

    const [
        minimumStock,
        setMinimumStock
    ] = useState("");

    const [
        editingInventory,
        setEditingInventory
    ] = useState(null);

    /**
     * Si contiene un inventario inactivo,
     * el formulario realizará una reactivación
     * mediante POST.
     */
    const [
        reactivatingInventory,
        setReactivatingInventory
    ] = useState(null);

    const [
        errors,
        setErrors
    ] = useState({});

    /**
     * Filtros locales.
     */
    const [
        searchFilter,
        setSearchFilter
    ] = useState("");

    const [
        activeFilter,
        setActiveFilter
    ] = useState("");

    const [
        stockFilter,
        setStockFilter
    ] = useState("");

    /**
     * Paginación local.
     *
     * El backend devuelve actualmente una lista,
     * no una respuesta paginada.
     */
    const [
        page,
        setPage
    ] = useState(0);

    const [
        size,
        setSize
    ] = useState(10);

    /**
     * Carga inventarios activos e inactivos.
     */
    const loadInventory =
        useCallback(async () => {
            try {
                setLoading(true);

                const response =
                    await getInventoryAdministration();

                setInventoryItems(
                    Array.isArray(
                        response.data
                    )
                        ? response.data
                        : []
                );

            } catch (error) {
                console.error(
                    "Error al cargar inventario:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar el inventario."
                );

            } finally {
                setLoading(false);
            }
        }, []);

    /**
     * Carga medicamentos activos para el selector.
     */
    const loadMedications =
        useCallback(async () => {
            if (!canCreate) {
                setMedications([]);
                return;
            }

            try {
                setLoadingMedications(
                    true
                );

                const response =
                    await getPharmacyMedications();

                setMedications(
                    Array.isArray(
                        response.data
                    )
                        ? response.data
                        : []
                );

            } catch (error) {
                console.error(
                    "Error al cargar medicamentos:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar el catálogo de medicamentos."
                );

            } finally {
                setLoadingMedications(
                    false
                );
            }
        }, [canCreate]);

    useEffect(() => {
        loadInventory();
    }, [loadInventory]);

    useEffect(() => {
        loadMedications();
    }, [loadMedications]);

    /**
     * Normaliza el ID porque algunos DTO pueden
     * devolver id y otros medicationId.
     */
    const getMedicationId = medication => {
        return (
            medication?.medicationId ??
            medication?.id ??
            ""
        );
    };

    /**
     * Lista filtrada.
     */
    const filteredInventory =
        useMemo(() => {
            const search =
                normalizeSearch(
                    searchFilter
                );

            return inventoryItems.filter(
                item => {
                    const matchesSearch =
                        !search ||
                        normalizeSearch(
                            item.medicationCode
                        ).includes(search) ||
                        normalizeSearch(
                            item.medicationName
                        ).includes(search) ||
                        normalizeSearch(
                            item.activeIngredient
                        ).includes(search);

                    const matchesActive =
                        activeFilter === "" ||
                        String(
                            Boolean(item.active)
                        ) === activeFilter;

                    const matchesStock =
                        stockFilter === "" ||
                        (
                            stockFilter ===
                            "LOW" &&
                            Boolean(
                                item.lowStock
                            )
                        ) ||
                        (
                            stockFilter ===
                            "OUT" &&
                            Boolean(
                                item.outOfStock
                            )
                        ) ||
                        (
                            stockFilter ===
                            "AVAILABLE" &&
                            Boolean(
                                item.available
                            ) &&
                            !Boolean(
                                item.lowStock
                            )
                        );

                    return (
                        matchesSearch &&
                        matchesActive &&
                        matchesStock
                    );
                }
            );
        }, [
            inventoryItems,
            searchFilter,
            activeFilter,
            stockFilter
        ]);

    /**
     * Regresar a página cero si cambian filtros.
     */
    useEffect(() => {
        setPage(0);
    }, [
        searchFilter,
        activeFilter,
        stockFilter,
        size
    ]);

    const totalElements =
        filteredInventory.length;

    const totalPages =
        Math.ceil(
            totalElements / size
        );

    const paginatedInventory =
        useMemo(() => {
            const start =
                page * size;

            return filteredInventory.slice(
                start,
                start + size
            );
        }, [
            filteredInventory,
            page,
            size
        ]);

    const first =
        page <= 0;

    const last =
        totalPages === 0 ||
        page >= totalPages - 1;

    /**
     * Inventario seleccionado para mostrar
     * información del medicamento.
     */
    const selectedMedication =
        useMemo(() => {
            if (!medicationId) {
                return null;
            }

            return medications.find(
                medication =>
                    Number(
                        getMedicationId(
                            medication
                        )
                    ) ===
                    Number(medicationId)
            ) || null;
        }, [
            medicationId,
            medications
        ]);

    /**
     * Valida el formulario.
     */
    const validateForm = () => {
        const newErrors = {};

        const parsedMedicationId =
            Number(medicationId);

        const parsedCurrentStock =
            Number(currentStock);

        const parsedMinimumStock =
            Number(minimumStock);

        if (
            !editingInventory &&
            (
                !medicationId ||
                Number.isNaN(
                    parsedMedicationId
                ) ||
                parsedMedicationId <= 0
            )
        ) {
            newErrors.medicationId =
                "Debe seleccionar un medicamento.";
        }

        if (
            currentStock === "" ||
            currentStock === null ||
            currentStock === undefined
        ) {
            newErrors.currentStock =
                "El stock actual es obligatorio.";

        } else if (
            !Number.isInteger(
                parsedCurrentStock
            )
        ) {
            newErrors.currentStock =
                "El stock actual debe ser un número entero.";

        } else if (
            parsedCurrentStock < 0
        ) {
            newErrors.currentStock =
                "El stock actual no puede ser negativo.";
        }

        if (
            minimumStock === "" ||
            minimumStock === null ||
            minimumStock === undefined
        ) {
            newErrors.minimumStock =
                "El stock mínimo es obligatorio.";

        } else if (
            !Number.isInteger(
                parsedMinimumStock
            )
        ) {
            newErrors.minimumStock =
                "El stock mínimo debe ser un número entero.";

        } else if (
            parsedMinimumStock < 0
        ) {
            newErrors.minimumStock =
                "El stock mínimo no puede ser negativo.";
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors)
                .length === 0
        );
    };

    /**
     * Limpia formulario.
     */
    const clearForm = () => {
        setMedicationId("");
        setCurrentStock("");
        setMinimumStock("");
        setEditingInventory(null);
        setReactivatingInventory(null);
        setErrors({});
    };

    /**
     * Crea, reactiva o actualiza inventario.
     */
    const handleSubmit =
        async event => {
            event.preventDefault();

            if (!validateForm()) {
                return;
            }

            try {
                setSaving(true);

                if (editingInventory) {
                    if (!canUpdate) {
                        alert(
                            "No tiene permiso para actualizar inventario."
                        );

                        return;
                    }

                    const response =
                        await updateInventory(
                            editingInventory
                                .inventoryId,
                            {
                                currentStock:
                                    Number(
                                        currentStock
                                    ),
                                minimumStock:
                                    Number(
                                        minimumStock
                                    ),
                                version:
                                    editingInventory
                                        .version
                            }
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Inventario actualizado correctamente."
                    );

                } else {
                    if (!canCreate) {
                        alert(
                            "No tiene permiso para crear o reactivar inventario."
                        );

                        return;
                    }

                    const response =
                        await createInventory({
                            medicationId:
                                Number(
                                    medicationId
                                ),
                            currentStock:
                                Number(
                                    currentStock
                                ),
                            minimumStock:
                                Number(
                                    minimumStock
                                )
                        });

                    alert(
                        response.data
                            ?.message ||
                        (
                            reactivatingInventory
                                ? "Inventario reactivado correctamente."
                                : "Inventario creado correctamente."
                        )
                    );
                }

                clearForm();

                await loadInventory();

            } catch (error) {
                console.error(
                    "Error al guardar inventario:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible guardar el inventario."
                );

                if (
                    error.response
                        ?.status === 409
                ) {
                    clearForm();
                    await loadInventory();
                }

            } finally {
                setSaving(false);
            }
        };

    /**
     * Edita un inventario activo.
     */
    const handleEdit = item => {
        if (!canUpdate) {
            return;
        }

        setEditingInventory(item);
        setReactivatingInventory(null);

        setMedicationId(
            String(
                item.medicationId
            )
        );

        setCurrentStock(
            String(
                item.currentStock ?? 0
            )
        );

        setMinimumStock(
            String(
                item.minimumStock ?? 0
            )
        );

        setErrors({});

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    /**
     * Prepara la reactivación mediante POST.
     */
    const handleReactivate = item => {
        if (!canCreate) {
            return;
        }

        setEditingInventory(null);
        setReactivatingInventory(
            item
        );

        setMedicationId(
            String(
                item.medicationId
            )
        );

        setCurrentStock(
            String(
                item.currentStock ?? 0
            )
        );

        setMinimumStock(
            String(
                item.minimumStock ?? 0
            )
        );

        setErrors({});

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    /**
     * Desactiva inventario.
     */
    const handleDeactivate =
        async item => {
            if (!canChangeStatus) {
                return;
            }

            const confirmed =
                window.confirm(
                    `¿Desea desactivar el inventario de "${item.medicationName}"?`
                );

            if (!confirmed) {
                return;
            }

            try {
                setSaving(true);

                const response =
                    await changeInventoryStatus(
                        item.inventoryId,
                        false
                    );

                alert(
                    response.data
                        ?.message ||
                    "Inventario desactivado correctamente."
                );

                if (
                    editingInventory
                        ?.inventoryId ===
                    item.inventoryId
                ) {
                    clearForm();
                }

                await loadInventory();

            } catch (error) {
                console.error(
                    "Error al desactivar inventario:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible desactivar el inventario."
                );

                await loadInventory();

            } finally {
                setSaving(false);
            }
        };

    const clearFilters = () => {
        setSearchFilter("");
        setActiveFilter("");
        setStockFilter("");
        setPage(0);
    };

    return (
        <MainLayout>
            <h1 className="page-title">
                Inventario de Farmacia
            </h1>

            {canReadMovements && (
                <div
                    className="button-row"
                    style={{
                        marginBottom: "16px"
                    }}
                >
                    <a
                        href="/pharmacy/inventory-movements"
                        className="btn btn-secondary"
                    >
                        Ver movimientos de inventario
                    </a>
                </div>
            )
            }

            {
                (canCreate ||
                    (
                        editingInventory &&
                        canUpdate
                    )) && (
                    <div className="card">
                        <h2 className="section-title">
                            {editingInventory
                                ? "Ajustar Inventario"
                                : (
                                    reactivatingInventory
                                        ? "Reactivar Inventario"
                                        : "Agregar Medicamento al Inventario"
                                )}
                        </h2>

                        <form
                            onSubmit={
                                handleSubmit
                            }
                        >
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>
                                        Medicamento
                                    </label>

                                    <select
                                        value={
                                            medicationId
                                        }
                                        disabled={
                                            Boolean(
                                                editingInventory ||
                                                reactivatingInventory
                                            ) ||
                                            loadingMedications
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
                                                        {
                                                            medication.unit
                                                                ? ` | ${medication.unit}`
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

                                    {loadingMedications && (
                                        <small>
                                            Cargando medicamentos...
                                        </small>
                                    )}
                                </div>

                                <div className="form-group">
                                    <label>
                                        Stock actual
                                    </label>

                                    <input
                                        type="number"
                                        min="0"
                                        step="1"
                                        placeholder="0"
                                        value={
                                            currentStock
                                        }
                                        className={
                                            errors.currentStock
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setCurrentStock(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.currentStock && (
                                        <div className="error-message">
                                            {
                                                errors.currentStock
                                            }
                                        </div>
                                    )}
                                </div>

                                <div className="form-group">
                                    <label>
                                        Stock mínimo
                                    </label>

                                    <input
                                        type="number"
                                        min="0"
                                        step="1"
                                        placeholder="0"
                                        value={
                                            minimumStock
                                        }
                                        className={
                                            errors.minimumStock
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setMinimumStock(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.minimumStock && (
                                        <div className="error-message">
                                            {
                                                errors.minimumStock
                                            }
                                        </div>
                                    )}
                                </div>
                            </div>

                            {selectedMedication && (
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
                                        Medicamento:
                                    </strong>
                                    {" "}
                                    {selectedMedication.code}
                                    {" | "}
                                    {selectedMedication.name}

                                    {selectedMedication.controlled && (
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

                            {editingInventory && (
                                <div
                                    style={{
                                        marginTop: "12px",
                                        padding: "12px",
                                        borderRadius: "6px",
                                        backgroundColor:
                                            "#eff6ff",
                                        color: "#1e3a8a"
                                    }}
                                >
                                    Al guardar un cambio de existencias, el backend registrará automáticamente un
                                    {" "}
                                    <strong>
                                        AJUSTE_POSITIVO
                                    </strong>
                                    {" "}
                                    o
                                    {" "}
                                    <strong>
                                        AJUSTE_NEGATIVO
                                    </strong>
                                    .
                                    <br />
                                    ID:
                                    {" "}
                                    {editingInventory.inventoryId}
                                    {" | "}
                                    Versión:
                                    {" "}
                                    {editingInventory.version}
                                </div>
                            )}

                            {reactivatingInventory && (
                                <div
                                    style={{
                                        marginTop: "12px",
                                        padding: "12px",
                                        borderRadius: "6px",
                                        backgroundColor:
                                            "#f0fdf4",
                                        color: "#166534"
                                    }}
                                >
                                    Se reactivará el registro existente y se generará un movimiento
                                    {" "}
                                    <strong>
                                        REACTIVACION
                                    </strong>
                                    .
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
                                            editingInventory
                                                ? "Guardar ajuste"
                                                : (
                                                    reactivatingInventory
                                                        ? "Reactivar inventario"
                                                        : "Crear inventario"
                                                )
                                        )}
                                </button>

                                {(editingInventory ||
                                    reactivatingInventory) && (
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
                )
            }

            <br />

            <div className="table-card">
                <h2 className="section-title">
                    Existencias de la Sucursal
                </h2>

                <div className="form-grid">
                    <div className="form-group">
                        <label>
                            Buscar medicamento
                        </label>

                        <input
                            type="text"
                            placeholder="Código, nombre o principio activo"
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
                            Existencias
                        </label>

                        <select
                            value={
                                stockFilter
                            }
                            onChange={
                                event =>
                                    setStockFilter(
                                        event.target
                                            .value
                                    )
                            }
                        >
                            <option value="">
                                Todas
                            </option>

                            <option value="LOW">
                                Stock bajo
                            </option>

                            <option value="OUT">
                                Agotados
                            </option>

                            <option value="AVAILABLE">
                                Disponibles sin alerta
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
                                event =>
                                    setSize(
                                        Number(
                                            event.target
                                                .value
                                        )
                                    )
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
                        type="button"
                        className="btn btn-secondary"
                        onClick={
                            clearFilters
                        }
                        disabled={loading}
                    >
                        Limpiar filtros
                    </button>

                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={
                            loadInventory
                        }
                        disabled={loading}
                    >
                        {loading
                            ? "Actualizando..."
                            : "Actualizar listado"}
                    </button>
                </div>

                <br />

                {loading ? (
                    <p>
                        Cargando inventario...
                    </p>

                ) : paginatedInventory
                    .length === 0 ? (
                    <p>
                        No hay registros que coincidan con los filtros.
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
                                        Presentación
                                    </th>

                                    <th>
                                        Stock actual
                                    </th>

                                    <th>
                                        Stock mínimo
                                    </th>

                                    <th>
                                        Disponibilidad
                                    </th>

                                    <th>
                                        Controlado
                                    </th>

                                    <th>
                                        Estado
                                    </th>

                                    <th>
                                        Actualizado por
                                    </th>

                                    <th>
                                        Versión
                                    </th>

                                    {(canUpdate ||
                                        canChangeStatus ||
                                        canCreate) && (
                                            <th>
                                                Acciones
                                            </th>
                                        )}
                                </tr>
                            </thead>

                            <tbody>
                                {paginatedInventory.map(
                                    item => (
                                        <tr
                                            key={
                                                item.inventoryId
                                            }
                                        >
                                            <td>
                                                <strong>
                                                    {
                                                        item.medicationCode
                                                    }
                                                </strong>

                                                <div>
                                                    {
                                                        item.medicationName
                                                    }
                                                </div>

                                                <small>
                                                    {
                                                        item.activeIngredient
                                                    }
                                                    {
                                                        item.concentration
                                                            ? ` | ${item.concentration}`
                                                            : ""
                                                    }
                                                </small>
                                            </td>

                                            <td>
                                                {
                                                    item.presentation ||
                                                    "No indicada"
                                                }
                                            </td>

                                            <td>
                                                <strong>
                                                    {
                                                        item.currentStock
                                                    }
                                                </strong>
                                                {" "}
                                                {
                                                    item.medicationUnit ||
                                                    "UNIDADES"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    item.minimumStock
                                                }
                                                {" "}
                                                {
                                                    item.medicationUnit ||
                                                    "UNIDADES"
                                                }
                                            </td>

                                            <td>
                                                <StockStatus
                                                    item={item}
                                                />
                                            </td>

                                            <td>
                                                {
                                                    item.controlled
                                                        ? "Sí"
                                                        : "No"
                                                }
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        item.active
                                                            ? "status-active"
                                                            : "status-inactive"
                                                    }
                                                >
                                                    {item.active
                                                        ? "Activo"
                                                        : "Inactivo"}
                                                </span>
                                            </td>

                                            <td>
                                                {
                                                    item.updatedBy ||
                                                    "Sin información"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    item.version
                                                }
                                            </td>

                                            {(canUpdate ||
                                                canChangeStatus ||
                                                canCreate) && (
                                                    <td>
                                                        <div className="button-row">
                                                            {item.active &&
                                                                canUpdate && (
                                                                    <button
                                                                        type="button"
                                                                        className="btn btn-secondary"
                                                                        onClick={() =>
                                                                            handleEdit(
                                                                                item
                                                                            )
                                                                        }
                                                                        disabled={saving}
                                                                    >
                                                                        Ajustar
                                                                    </button>
                                                                )}

                                                            {item.active &&
                                                                canChangeStatus && (
                                                                    <button
                                                                        type="button"
                                                                        className="btn btn-danger-outline"
                                                                        onClick={() =>
                                                                            handleDeactivate(
                                                                                item
                                                                            )
                                                                        }
                                                                        disabled={saving}
                                                                    >
                                                                        Desactivar
                                                                    </button>
                                                                )}

                                                            {!item.active &&
                                                                canCreate && (
                                                                    <button
                                                                        type="button"
                                                                        className="btn btn-primary"
                                                                        onClick={() =>
                                                                            handleReactivate(
                                                                                item
                                                                            )
                                                                        }
                                                                        disabled={saving}
                                                                    >
                                                                        Reactivar
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
                                    current =>
                                        Math.max(
                                            current - 1,
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
                            {totalPages === 0
                                ? 0
                                : page + 1}
                            {" "}
                            de
                            {" "}
                            {totalPages}
                        </span>

                        <button
                            type="button"
                            className="btn btn-secondary"
                            disabled={
                                last ||
                                loading
                            }
                            onClick={() =>
                                setPage(
                                    current =>
                                        current + 1
                                )
                            }
                        >
                            Siguiente
                        </button>
                    </div>
                </div>
            </div>
        </MainLayout >
    );
}

function StockStatus({ item }) {
    if (item.outOfStock) {
        return (
            <span className="status-inactive">
                Agotado
            </span>
        );
    }

    if (item.lowStock) {
        return (
            <span
                style={{
                    color: "#b45309",
                    fontWeight: 700
                }}
            >
                Stock bajo
            </span>
        );
    }

    return (
        <span className="status-active">
            Disponible
        </span>
    );
}

function normalizeSearch(value) {
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
        .toLowerCase();
}

export default PharmacyInventory;