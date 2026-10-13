import {
    useCallback,
    useEffect,
    useState
} from "react";

import MainLayout from "../../layouts/MainLayout";

import {
    changeMedicationStatus,
    createMedication,
    getMedicationsPage,
    updateMedication
} from "../../api/medicationAdminApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

function Medications() {
    const {
        hasPermission
    } = useAuth();

    /**
     * Permisos granulares del módulo.
     */
    const canCreate =
        hasPermission(
            "MEDICATION_CREATE"
        );

    const canUpdate =
        hasPermission(
            "MEDICATION_UPDATE"
        );

    const canChangeStatus =
        hasPermission(
            "MEDICATION_DELETE"
        );

    /**
     * Listado y carga.
     */
    const [
        medications,
        setMedications
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
        code,
        setCode
    ] = useState("");

    const [
        name,
        setName
    ] = useState("");

    const [
        activeIngredient,
        setActiveIngredient
    ] = useState("");

    const [
        concentration,
        setConcentration
    ] = useState("");

    const [
        presentation,
        setPresentation
    ] = useState("");

    const [
        unit,
        setUnit
    ] = useState("");

    const [
        description,
        setDescription
    ] = useState("");

    const [
        unitPrice,
        setUnitPrice
    ] = useState("");

    const [
        controlled,
        setControlled
    ] = useState(false);

    const [
        editingMedication,
        setEditingMedication
    ] = useState(null);

    const [
        errors,
        setErrors
    ] = useState({});

    /**
     * Filtros.
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
        controlledFilter,
        setControlledFilter
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
     * Consulta el catálogo administrativo.
     */
    const loadMedications =
        useCallback(async () => {
            try {
                setLoading(true);

                const response =
                    await getMedicationsPage({
                        search:
                            searchFilter,
                        active:
                            activeFilter,
                        controlled:
                            controlledFilter,
                        page,
                        size
                    });

                const pageData =
                    response.data;

                setMedications(
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
                    "Error al cargar medicamentos:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar los medicamentos."
                );

            } finally {
                setLoading(false);
            }
        }, [
            searchFilter,
            activeFilter,
            controlledFilter,
            page,
            size
        ]);

    useEffect(() => {
        loadMedications();
    }, [loadMedications]);

    /**
     * Valida la información del formulario.
     */
    const validateForm = () => {
        const newErrors = {};

        const cleanCode =
            code.trim();

        const cleanName =
            name.trim();

        const cleanActiveIngredient =
            activeIngredient.trim();

        const cleanConcentration =
            concentration.trim();

        const cleanPresentation =
            presentation.trim();

        const cleanUnit =
            unit.trim();

        const cleanDescription =
            description.trim();

        const numericUnitPrice =
            Number(unitPrice);

        if (!cleanCode) {
            newErrors.code =
                "El código del medicamento es obligatorio.";

        } else if (
            cleanCode.length > 40
        ) {
            newErrors.code =
                "El código no puede exceder los 40 caracteres.";

        } else if (
            !/^[A-Za-z0-9_-]+$/
                .test(cleanCode)
        ) {
            newErrors.code =
                "El código solamente puede contener letras, números, guion y guion bajo.";
        }

        if (!cleanName) {
            newErrors.name =
                "El nombre del medicamento es obligatorio.";

        } else if (
            cleanName.length > 200
        ) {
            newErrors.name =
                "El nombre no puede exceder los 200 caracteres.";
        }

        if (!cleanActiveIngredient) {
            newErrors.activeIngredient =
                "El principio activo es obligatorio.";

        } else if (
            cleanActiveIngredient.length > 200
        ) {
            newErrors.activeIngredient =
                "El principio activo no puede exceder los 200 caracteres.";
        }

        if (!cleanConcentration) {
            newErrors.concentration =
                "La concentración es obligatoria.";

        } else if (
            cleanConcentration.length > 100
        ) {
            newErrors.concentration =
                "La concentración no puede exceder los 100 caracteres.";
        }

        if (!cleanPresentation) {
            newErrors.presentation =
                "La presentación es obligatoria.";

        } else if (
            cleanPresentation.length > 100
        ) {
            newErrors.presentation =
                "La presentación no puede exceder los 100 caracteres.";
        }

        if (!cleanUnit) {
            newErrors.unit =
                "La unidad del medicamento es obligatoria.";

        } else if (
            cleanUnit.length > 50
        ) {
            newErrors.unit =
                "La unidad no puede exceder los 50 caracteres.";
        }

        if (!cleanDescription) {
            newErrors.description =
                "La descripción del medicamento es obligatoria.";

        } else if (
            cleanDescription.length > 1000
        ) {
            newErrors.description =
                "La descripción no puede exceder los 1000 caracteres.";
        }

        if (
            unitPrice === null ||
            unitPrice === undefined ||
            String(unitPrice).trim() === ""
        ) {
            newErrors.unitPrice =
                "El precio unitario es obligatorio.";

        } else if (
            Number.isNaN(
                numericUnitPrice
            )
        ) {
            newErrors.unitPrice =
                "El precio debe ser un número válido.";

        } else if (
            numericUnitPrice <= 0
        ) {
            newErrors.unitPrice =
                "El precio unitario debe ser mayor que cero.";

        } else if (
            numericUnitPrice >
            99999999.99
        ) {
            newErrors.unitPrice =
                "El precio admite como máximo 8 dígitos enteros.";

        } else if (
            !hasMaximumTwoDecimals(
                String(unitPrice)
            )
        ) {
            newErrors.unitPrice =
                "El precio admite como máximo 2 decimales.";
        }

        if (
            controlled !== true &&
            controlled !== false
        ) {
            newErrors.controlled =
                "Debe indicar si el medicamento es controlado.";
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
        setCode("");
        setName("");
        setActiveIngredient("");
        setConcentration("");
        setPresentation("");
        setUnit("");
        setDescription("");
        setUnitPrice("");
        setControlled(false);
        setEditingMedication(null);
        setErrors({});
    };

    /**
     * Crea o actualiza un medicamento.
     */
    const handleSubmit =
        async event => {
            event.preventDefault();

            if (!validateForm()) {
                return;
            }

            const normalizedData = {
                code:
                    code.trim(),
                name:
                    name.trim(),
                activeIngredient:
                    activeIngredient.trim(),
                concentration:
                    concentration.trim(),
                presentation:
                    presentation.trim(),
                unit:
                    unit.trim(),
                description:
                    description.trim(),
                unitPrice:
                    Number(unitPrice),
                controlled
            };

            try {
                setSaving(true);

                if (editingMedication) {
                    if (!canUpdate) {
                        alert(
                            "No tiene permiso para actualizar medicamentos."
                        );

                        return;
                    }

                    const response =
                        await updateMedication(
                            editingMedication.id,
                            {
                                ...normalizedData,
                                version:
                                    editingMedication
                                        .version
                            }
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Medicamento actualizado correctamente."
                    );

                } else {
                    if (!canCreate) {
                        alert(
                            "No tiene permiso para crear medicamentos."
                        );

                        return;
                    }

                    const response =
                        await createMedication(
                            normalizedData
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Medicamento creado correctamente."
                    );
                }

                clearForm();

                await loadMedications();

            } catch (error) {
                console.error(
                    "Error al guardar medicamento:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible guardar el medicamento."
                );

                if (
                    error.response
                        ?.status === 409
                ) {
                    clearForm();

                    await loadMedications();
                }

            } finally {
                setSaving(false);
            }
        };

    /**
     * Carga un medicamento dentro
     * del formulario.
     */
    const handleEdit = medication => {
        if (!canUpdate) {
            return;
        }

        setEditingMedication(
            medication
        );

        setCode(
            medication.code || ""
        );

        setName(
            medication.name || ""
        );

        setActiveIngredient(
            medication.activeIngredient ||
            ""
        );

        setConcentration(
            medication.concentration ||
            ""
        );

        setPresentation(
            medication.presentation ||
            ""
        );

        setUnit(
            medication.unit || ""
        );

        setDescription(
            medication.description ||
            ""
        );

        setUnitPrice(
            medication.unitPrice != null
                ? String(
                    medication.unitPrice
                )
                : ""
        );

        setControlled(
            Boolean(
                medication.controlled
            )
        );

        setErrors({});

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    /**
     * Activa o desactiva un medicamento.
     */
    const handleStatusChange =
        async medication => {
            if (!canChangeStatus) {
                alert(
                    "No tiene permiso para cambiar el estado de los medicamentos."
                );

                return;
            }

            const newStatus =
                !medication.active;

            const actionText =
                newStatus
                    ? "reactivar"
                    : "desactivar";

            const confirmed =
                window.confirm(
                    `¿Desea ${actionText} el medicamento "${medication.name}"?`
                );

            if (!confirmed) {
                return;
            }

            try {
                setSaving(true);

                const response =
                    await changeMedicationStatus(
                        medication.id,
                        newStatus,
                        medication.version
                    );

                alert(
                    response.data
                        ?.message ||
                    (
                        newStatus
                            ? "Medicamento reactivado correctamente."
                            : "Medicamento desactivado correctamente."
                    )
                );

                if (
                    editingMedication?.id ===
                    medication.id
                ) {
                    clearForm();
                }

                await loadMedications();

            } catch (error) {
                console.error(
                    "Error al cambiar estado:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cambiar el estado del medicamento."
                );

                await loadMedications();

            } finally {
                setSaving(false);
            }
        };

    /**
     * Aplica los filtros.
     */
    const handleSearch =
        event => {
            event.preventDefault();

            if (page === 0) {
                loadMedications();
            } else {
                setPage(0);
            }
        };

    /**
     * Limpia los filtros.
     */
    const clearFilters = () => {
        setSearchFilter("");
        setActiveFilter("");
        setControlledFilter("");
        setPage(0);
    };

    return (
        <MainLayout>
            <h1 className="page-title">
                Medicamentos
            </h1>

            {/*
             * Formulario de creación y edición.
             */}
            {(canCreate ||
                (
                    editingMedication &&
                    canUpdate
                )) && (
                    <div className="card">
                        <h2 className="section-title">
                            {editingMedication
                                ? "Editar Medicamento"
                                : "Crear Medicamento"}
                        </h2>

                        <form
                            onSubmit={
                                handleSubmit
                            }
                        >
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>
                                        Código
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={40}
                                        placeholder="Ejemplo: PAR-500-TAB"
                                        value={code}
                                        className={
                                            errors.code
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setCode(
                                                    event
                                                        .target
                                                        .value
                                                        .toUpperCase()
                                                )
                                        }
                                    />

                                    {errors.code && (
                                        <div className="error-message">
                                            {errors.code}
                                        </div>
                                    )}

                                    <small>
                                        {code.length}/40
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Nombre comercial
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={200}
                                        placeholder="Ejemplo: Paracetamol"
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
                                        {name.length}/200
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Principio activo
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={200}
                                        placeholder="Ejemplo: Paracetamol"
                                        value={
                                            activeIngredient
                                        }
                                        className={
                                            errors.activeIngredient
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setActiveIngredient(
                                                    event
                                                        .target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.activeIngredient && (
                                        <div className="error-message">
                                            {
                                                errors.activeIngredient
                                            }
                                        </div>
                                    )}

                                    <small>
                                        {activeIngredient.length}/200
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Concentración
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={100}
                                        placeholder="Ejemplo: 500 mg"
                                        value={
                                            concentration
                                        }
                                        className={
                                            errors.concentration
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setConcentration(
                                                    event
                                                        .target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.concentration && (
                                        <div className="error-message">
                                            {
                                                errors.concentration
                                            }
                                        </div>
                                    )}

                                    <small>
                                        {concentration.length}/100
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Presentación
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={100}
                                        placeholder="Ejemplo: Caja de 20 tabletas"
                                        value={
                                            presentation
                                        }
                                        className={
                                            errors.presentation
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setPresentation(
                                                    event
                                                        .target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.presentation && (
                                        <div className="error-message">
                                            {
                                                errors.presentation
                                            }
                                        </div>
                                    )}

                                    <small>
                                        {presentation.length}/100
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Unidad de inventario
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={50}
                                        placeholder="Ejemplo: TABLETA"
                                        value={unit}
                                        className={
                                            errors.unit
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setUnit(
                                                    event
                                                        .target
                                                        .value
                                                        .toUpperCase()
                                                )
                                        }
                                    />

                                    {errors.unit && (
                                        <div className="error-message">
                                            {errors.unit}
                                        </div>
                                    )}

                                    <small>
                                        Ejemplos: TABLETA, CÁPSULA, FRASCO, AMPOLLA.
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Precio unitario
                                    </label>

                                    <input
                                        type="number"
                                        min="0.01"
                                        max="99999999.99"
                                        step="0.01"
                                        placeholder="0.00"
                                        value={unitPrice}
                                        className={
                                            errors.unitPrice
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setUnitPrice(
                                                    event
                                                        .target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.unitPrice && (
                                        <div className="error-message">
                                            {
                                                errors.unitPrice
                                            }
                                        </div>
                                    )}

                                    <small>
                                        Precio por unidad de inventario.
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Medicamento controlado
                                    </label>

                                    <select
                                        value={
                                            String(
                                                controlled
                                            )
                                        }
                                        className={
                                            errors.controlled
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setControlled(
                                                    event
                                                        .target
                                                        .value ===
                                                    "true"
                                                )
                                        }
                                    >
                                        <option value="false">
                                            No
                                        </option>

                                        <option value="true">
                                            Sí
                                        </option>
                                    </select>

                                    {errors.controlled && (
                                        <div className="error-message">
                                            {
                                                errors.controlled
                                            }
                                        </div>
                                    )}

                                    <small>
                                        Indica si requiere control especial.
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Descripción
                                    </label>

                                    <textarea
                                        rows={4}
                                        maxLength={1000}
                                        placeholder="Descripción del medicamento"
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
                                        {description.length}/1000
                                    </small>
                                </div>
                            </div>

                            {editingMedication && (
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

                                    ID {
                                        editingMedication.id
                                    }

                                    {" | "}

                                    Versión {
                                        editingMedication.version
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
                                            editingMedication
                                                ? "Actualizar"
                                                : "Crear"
                                        )}
                                </button>

                                {editingMedication && (
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
                    Listado de Medicamentos
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
                                placeholder="Código, nombre o principio activo"
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
                                Tipo de medicamento
                            </label>

                            <select
                                value={
                                    controlledFilter
                                }
                                onChange={
                                    event =>
                                        setControlledFilter(
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
                                    Controlados
                                </option>

                                <option value="false">
                                    No controlados
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

                {loading ? (
                    <p>
                        Cargando medicamentos...
                    </p>

                ) : medications.length === 0 ? (
                    <p
                        style={{
                            color: "#64748b"
                        }}
                    >
                        No hay medicamentos que coincidan con los filtros.
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
                                        Código
                                    </th>

                                    <th>
                                        Medicamento
                                    </th>

                                    <th>
                                        Principio activo
                                    </th>

                                    <th>
                                        Concentración
                                    </th>

                                    <th>
                                        Presentación
                                    </th>

                                    <th>
                                        Unidad
                                    </th>

                                    <th>
                                        Precio
                                    </th>

                                    <th>
                                        Controlado
                                    </th>

                                    <th>
                                        Estado
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
                                {medications.map(
                                    medication => (
                                        <tr
                                            key={
                                                medication.id
                                            }
                                        >
                                            <td>
                                                <strong>
                                                    {
                                                        medication.code
                                                    }
                                                </strong>
                                            </td>

                                            <td>
                                                {
                                                    medication.name
                                                }
                                            </td>

                                            <td>
                                                {
                                                    medication
                                                        .activeIngredient
                                                }
                                            </td>

                                            <td>
                                                {
                                                    medication
                                                        .concentration
                                                }
                                            </td>

                                            <td>
                                                {
                                                    medication
                                                        .presentation
                                                }
                                            </td>

                                            <td>
                                                {
                                                    medication.unit
                                                }
                                            </td>

                                            <td>
                                                {
                                                    formatCurrency(
                                                        medication
                                                            .unitPrice
                                                    )
                                                }
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        medication.controlled
                                                            ? "status-inactive"
                                                            : "status-active"
                                                    }
                                                >
                                                    {medication.controlled
                                                        ? "Sí"
                                                        : "No"}
                                                </span>
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        medication.active
                                                            ? "status-active"
                                                            : "status-inactive"
                                                    }
                                                >
                                                    {medication.active
                                                        ? "Activo"
                                                        : "Inactivo"}
                                                </span>
                                            </td>

                                            <td>
                                                {
                                                    medication.version
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
                                                                            medication
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
                                                                        medication.active
                                                                            ? "btn btn-danger-outline"
                                                                            : "btn btn-primary"
                                                                    }
                                                                    onClick={() =>
                                                                        handleStatusChange(
                                                                            medication
                                                                        )
                                                                    }
                                                                    disabled={
                                                                        saving
                                                                    }
                                                                >
                                                                    {medication.active
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
 * Comprueba que el precio tenga máximo
 * dos decimales.
 */
function hasMaximumTwoDecimals(value) {
    if (!value) {
        return false;
    }

    return /^\d+(\.\d{1,2})?$/
        .test(
            String(value).trim()
        );
}

/**
 * Formatea un precio en quetzales.
 */
function formatCurrency(value) {
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
            style: "currency",
            currency: "GTQ",
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }
    ).format(
        numericValue
    );
}

export default Medications;