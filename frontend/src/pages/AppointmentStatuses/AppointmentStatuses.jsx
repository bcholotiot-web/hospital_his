import {
    useCallback,
    useEffect,
    useMemo,
    useState
} from "react";

import MainLayout from "../../layouts/MainLayout";

import {
    changeAppointmentStatusState,
    createAppointmentStatus,
    getAppointmentStatusesPage,
    getAvailableAppointmentStatusCodes,
    updateAppointmentStatus
} from "../../api/appointmentStatusAdminApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

function AppointmentStatuses() {
    const {
        hasPermission
    } = useAuth();

    /**
     * Permisos granulares.
     */
    const canCreate =
        hasPermission(
            "APPOINTMENT_STATUS_CREATE"
        );

    const canUpdate =
        hasPermission(
            "APPOINTMENT_STATUS_UPDATE"
        );

    const canChangeStatus =
        hasPermission(
            "APPOINTMENT_STATUS_DELETE"
        );

    /**
     * Listado de configuraciones.
     */
    const [
        configurations,
        setConfigurations
    ] = useState([]);

    /**
     * Códigos técnicos del enum.
     */
    const [
        availableCodes,
        setAvailableCodes
    ] = useState([]);

    /**
     * Estados de carga.
     */
    const [
        loading,
        setLoading
    ] = useState(false);

    const [
        loadingCodes,
        setLoadingCodes
    ] = useState(false);

    const [
        saving,
        setSaving
    ] = useState(false);

    /**
     * Formulario.
     */
    const [
        code,
        setCode
    ] = useState("");

    const [
        displayName,
        setDisplayName
    ] = useState("");

    const [
        description,
        setDescription
    ] = useState("");

    const [
        displayOrder,
        setDisplayOrder
    ] = useState("");

    const [
        color,
        setColor
    ] = useState("#2563EB");

    const [
        patientVisible,
        setPatientVisible
    ] = useState(true);

    const [
        staffVisible,
        setStaffVisible
    ] = useState(true);

    const [
        editingConfiguration,
        setEditingConfiguration
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
     * Carga las configuraciones paginadas.
     */
    const loadConfigurations =
        useCallback(async () => {
            try {
                setLoading(true);

                const response =
                    await getAppointmentStatusesPage({
                        search:
                            searchFilter,
                        active:
                            activeFilter,
                        page,
                        size
                    });

                const pageData =
                    response.data;

                setConfigurations(
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
                    "Error al cargar estados de cita:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar las configuraciones de estados de cita."
                );

                setConfigurations([]);
                setTotalElements(0);
                setTotalPages(0);
                setFirst(true);
                setLast(true);

            } finally {
                setLoading(false);
            }
        }, [
            searchFilter,
            activeFilter,
            page,
            size
        ]);

    /**
     * Carga todos los códigos del enum.
     */
    const loadAvailableCodes =
        useCallback(async () => {
            try {
                setLoadingCodes(true);

                const response =
                    await getAvailableAppointmentStatusCodes();

                setAvailableCodes(
                    Array.isArray(
                        response.data
                    )
                        ? response.data
                        : []
                );

            } catch (error) {
                console.error(
                    "Error al cargar códigos técnicos:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar los códigos técnicos de estados."
                );

                setAvailableCodes([]);

            } finally {
                setLoadingCodes(false);
            }
        }, []);

    useEffect(() => {
        loadConfigurations();
    }, [loadConfigurations]);

    useEffect(() => {
        loadAvailableCodes();
    }, [loadAvailableCodes]);

    /**
     * Códigos que pueden seleccionarse al crear.
     *
     * Incluye:
     *
     * - Códigos no configurados.
     * - Códigos configurados pero inactivos, porque
     *   POST puede reactivarlos.
     */
    const selectableCodes =
        useMemo(() => {
            return availableCodes.filter(
                statusCode =>
                    !statusCode.configured ||
                    statusCode.active === false
            );
        }, [availableCodes]);

    /**
     * Código seleccionado.
     */
    const selectedCode =
        useMemo(() => {
            return availableCodes.find(
                statusCode =>
                    statusCode.code === code
            ) || null;
        }, [
            availableCodes,
            code
        ]);

    /**
     * Valida el formulario.
     */
    const validateForm = () => {
        const newErrors = {};

        const cleanDisplayName =
            displayName.trim();

        const cleanDescription =
            description.trim();

        const numericDisplayOrder =
            Number(displayOrder);

        const normalizedColor =
            color.trim();

        if (
            !editingConfiguration &&
            !code
        ) {
            newErrors.code =
                "Debe seleccionar un código técnico.";
        }

        if (!cleanDisplayName) {
            newErrors.displayName =
                "El nombre visible es obligatorio.";

        } else if (
            cleanDisplayName.length > 100
        ) {
            newErrors.displayName =
                "El nombre visible no puede exceder los 100 caracteres.";
        }

        if (!cleanDescription) {
            newErrors.description =
                "La descripción es obligatoria.";

        } else if (
            cleanDescription.length > 500
        ) {
            newErrors.description =
                "La descripción no puede exceder los 500 caracteres.";
        }

        if (
            displayOrder === "" ||
            displayOrder === null ||
            displayOrder === undefined
        ) {
            newErrors.displayOrder =
                "El orden visual es obligatorio.";

        } else if (
            !Number.isInteger(
                numericDisplayOrder
            )
        ) {
            newErrors.displayOrder =
                "El orden visual debe ser un número entero.";

        } else if (
            numericDisplayOrder < 0 ||
            numericDisplayOrder > 1000
        ) {
            newErrors.displayOrder =
                "El orden visual debe estar entre 0 y 1000.";
        }

        if (!normalizedColor) {
            newErrors.color =
                "El color es obligatorio.";

        } else if (
            !/^#[0-9A-Fa-f]{6}$/
                .test(normalizedColor)
        ) {
            newErrors.color =
                "El color debe utilizar el formato hexadecimal #RRGGBB.";
        }

        if (
            patientVisible !== true &&
            patientVisible !== false
        ) {
            newErrors.patientVisible =
                "Debe indicar la visibilidad para pacientes.";
        }

        if (
            staffVisible !== true &&
            staffVisible !== false
        ) {
            newErrors.staffVisible =
                "Debe indicar la visibilidad para el personal.";
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
        setDisplayName("");
        setDescription("");
        setDisplayOrder("");
        setColor("#2563EB");
        setPatientVisible(true);
        setStaffVisible(true);
        setEditingConfiguration(null);
        setErrors({});
    };

    /**
     * Al seleccionar un código, utilizar el
     * nombre sugerido como valor inicial.
     */
    const handleCodeChange =
        event => {
            const selectedValue =
                event.target.value;

            setCode(selectedValue);

            const matchingCode =
                availableCodes.find(
                    statusCode =>
                        statusCode.code ===
                        selectedValue
                );

            if (
                matchingCode &&
                !displayName.trim()
            ) {
                setDisplayName(
                    matchingCode
                        .suggestedDisplayName ||
                    formatTechnicalCode(
                        matchingCode.code
                    )
                );
            }

            setErrors(
                currentErrors => ({
                    ...currentErrors,
                    code: null
                })
            );
        };

    /**
     * Crea, reactiva o actualiza.
     */
    const handleSubmit =
        async event => {
            event.preventDefault();

            if (!validateForm()) {
                return;
            }

            const normalizedData = {
                displayName:
                    displayName.trim(),
                description:
                    description.trim(),
                displayOrder:
                    Number(displayOrder),
                color:
                    color.trim()
                        .toUpperCase(),
                patientVisible,
                staffVisible
            };

            try {
                setSaving(true);

                if (editingConfiguration) {
                    if (!canUpdate) {
                        alert(
                            "No tiene permiso para actualizar estados de cita."
                        );

                        return;
                    }

                    const response =
                        await updateAppointmentStatus(
                            editingConfiguration.id,
                            {
                                ...normalizedData,
                                version:
                                    editingConfiguration
                                        .version
                            }
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Configuración actualizada correctamente."
                    );

                } else {
                    if (!canCreate) {
                        alert(
                            "No tiene permiso para crear configuraciones de estados."
                        );

                        return;
                    }

                    const response =
                        await createAppointmentStatus({
                            code,
                            ...normalizedData
                        });

                    alert(
                        response.data
                            ?.message ||
                        (
                            selectedCode
                                ?.configured
                                ? "Configuración reactivada correctamente."
                                : "Configuración creada correctamente."
                        )
                    );
                }

                clearForm();

                await Promise.all([
                    loadConfigurations(),
                    loadAvailableCodes()
                ]);

            } catch (error) {
                console.error(
                    "Error al guardar estado de cita:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible guardar la configuración del estado."
                );

                if (
                    error.response
                        ?.status === 409
                ) {
                    clearForm();

                    await Promise.all([
                        loadConfigurations(),
                        loadAvailableCodes()
                    ]);
                }

            } finally {
                setSaving(false);
            }
        };

    /**
     * Carga una configuración en edición.
     */
    const handleEdit =
        configuration => {
            if (!canUpdate) {
                return;
            }

            setEditingConfiguration(
                configuration
            );

            setCode(
                configuration.code ||
                ""
            );

            setDisplayName(
                configuration.displayName ||
                ""
            );

            setDescription(
                configuration.description ||
                ""
            );

            setDisplayOrder(
                configuration.displayOrder ??
                ""
            );

            setColor(
                configuration.color ||
                "#2563EB"
            );

            setPatientVisible(
                Boolean(
                    configuration
                        .patientVisible
                )
            );

            setStaffVisible(
                Boolean(
                    configuration
                        .staffVisible
                )
            );

            setErrors({});

            window.scrollTo({
                top: 0,
                behavior: "smooth"
            });
        };

    /**
     * Cambia el estado lógico de la configuración.
     */
    const handleStatusChange =
        async configuration => {
            if (!canChangeStatus) {
                alert(
                    "No tiene permiso para cambiar el estado de las configuraciones."
                );

                return;
            }

            const newStatus =
                !configuration.active;

            const actionText =
                newStatus
                    ? "reactivar"
                    : "desactivar";

            const confirmed =
                window.confirm(
                    `¿Desea ${actionText} la configuración "${configuration.displayName}"?`
                );

            if (!confirmed) {
                return;
            }

            try {
                setSaving(true);

                const response =
                    await changeAppointmentStatusState(
                        configuration.id,
                        newStatus,
                        configuration.version
                    );

                alert(
                    response.data
                        ?.message ||
                    (
                        newStatus
                            ? "Configuración reactivada correctamente."
                            : "Configuración desactivada correctamente."
                    )
                );

                if (
                    editingConfiguration
                        ?.id ===
                    configuration.id
                ) {
                    clearForm();
                }

                await Promise.all([
                    loadConfigurations(),
                    loadAvailableCodes()
                ]);

            } catch (error) {
                console.error(
                    "Error al cambiar estado:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cambiar el estado de la configuración."
                );

                await Promise.all([
                    loadConfigurations(),
                    loadAvailableCodes()
                ]);

            } finally {
                setSaving(false);
            }
        };

    /**
     * Aplica filtros desde la página cero.
     */
    const handleSearch =
        event => {
            event.preventDefault();

            if (page === 0) {
                loadConfigurations();

            } else {
                setPage(0);
            }
        };

    /**
     * Limpia filtros.
     */
    const clearFilters = () => {
        setSearchFilter("");
        setActiveFilter("");
        setPage(0);
    };

    return (
        <MainLayout>
            <h1 className="page-title">
                Estados de Cita
            </h1>

            <div
                style={{
                    marginBottom: "16px",
                    padding: "14px",
                    borderRadius: "8px",
                    border:
                        "1px solid #bfdbfe",
                    backgroundColor:
                        "#eff6ff",
                    color: "#1e3a8a"
                }}
            >
                Esta pantalla configura exclusivamente el nombre, descripción, color, orden y visibilidad de los estados. No modifica el flujo clínico ni el estado real de las citas.
            </div>

            {(canCreate ||
                (
                    editingConfiguration &&
                    canUpdate
                )) && (
                    <div className="card">
                        <h2 className="section-title">
                            {editingConfiguration
                                ? "Editar Configuración"
                                : "Crear Configuración"}
                        </h2>

                        <form
                            onSubmit={
                                handleSubmit
                            }
                        >
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>
                                        Código técnico
                                    </label>

                                    <select
                                        value={code}
                                        disabled={
                                            Boolean(
                                                editingConfiguration
                                            ) ||
                                            loadingCodes
                                        }
                                        className={
                                            errors.code
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            handleCodeChange
                                        }
                                    >
                                        <option value="">
                                            Seleccione un código
                                        </option>

                                        {selectableCodes.map(
                                            statusCode => (
                                                <option
                                                    key={
                                                        statusCode.code
                                                    }
                                                    value={
                                                        statusCode.code
                                                    }
                                                >
                                                    {
                                                        statusCode.code
                                                    }
                                                    {statusCode.configured &&
                                                        statusCode.active === false
                                                        ? " | Inactivo, disponible para reactivar"
                                                        : ""}
                                                </option>
                                            )
                                        )}

                                        {editingConfiguration && (
                                            <option
                                                value={
                                                    editingConfiguration
                                                        .code
                                                }
                                            >
                                                {
                                                    editingConfiguration
                                                        .code
                                                }
                                            </option>
                                        )}
                                    </select>

                                    {errors.code && (
                                        <div className="error-message">
                                            {errors.code}
                                        </div>
                                    )}

                                    <small>
                                        El código pertenece al enum técnico y no puede modificarse después de crear la configuración.
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Nombre visible
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={100}
                                        placeholder="Ejemplo: Pendiente de pago"
                                        value={
                                            displayName
                                        }
                                        className={
                                            errors.displayName
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setDisplayName(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.displayName && (
                                        <div className="error-message">
                                            {
                                                errors.displayName
                                            }
                                        </div>
                                    )}

                                    <small>
                                        {displayName.length}/100
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Orden visual
                                    </label>

                                    <input
                                        type="number"
                                        min="0"
                                        max="1000"
                                        step="1"
                                        placeholder="Ejemplo: 10"
                                        value={
                                            displayOrder
                                        }
                                        className={
                                            errors.displayOrder
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setDisplayOrder(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.displayOrder && (
                                        <div className="error-message">
                                            {
                                                errors.displayOrder
                                            }
                                        </div>
                                    )}
                                </div>

                                <div className="form-group">
                                    <label>
                                        Color
                                    </label>

                                    <div
                                        style={{
                                            display: "flex",
                                            gap: "10px",
                                            alignItems:
                                                "center"
                                        }}
                                    >
                                        <input
                                            type="color"
                                            value={
                                                normalizeColorInput(
                                                    color
                                                )
                                            }
                                            onChange={
                                                event =>
                                                    setColor(
                                                        event.target
                                                            .value
                                                            .toUpperCase()
                                                    )
                                            }
                                            style={{
                                                width: "60px",
                                                height: "42px",
                                                padding: "3px"
                                            }}
                                        />

                                        <input
                                            type="text"
                                            maxLength={7}
                                            placeholder="#2563EB"
                                            value={color}
                                            className={
                                                errors.color
                                                    ? "input-error"
                                                    : ""
                                            }
                                            onChange={
                                                event =>
                                                    setColor(
                                                        event.target
                                                            .value
                                                            .toUpperCase()
                                                    )
                                            }
                                        />
                                    </div>

                                    {errors.color && (
                                        <div className="error-message">
                                            {errors.color}
                                        </div>
                                    )}

                                    <small>
                                        Formato hexadecimal #RRGGBB.
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Visible para paciente
                                    </label>

                                    <select
                                        value={
                                            String(
                                                patientVisible
                                            )
                                        }
                                        onChange={
                                            event =>
                                                setPatientVisible(
                                                    event.target
                                                        .value ===
                                                    "true"
                                                )
                                        }
                                    >
                                        <option value="true">
                                            Sí
                                        </option>

                                        <option value="false">
                                            No
                                        </option>
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Visible para personal
                                    </label>

                                    <select
                                        value={
                                            String(
                                                staffVisible
                                            )
                                        }
                                        onChange={
                                            event =>
                                                setStaffVisible(
                                                    event.target
                                                        .value ===
                                                    "true"
                                                )
                                        }
                                    >
                                        <option value="true">
                                            Sí
                                        </option>

                                        <option value="false">
                                            No
                                        </option>
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Descripción
                                    </label>

                                    <textarea
                                        rows={4}
                                        maxLength={500}
                                        placeholder="Descripción funcional del estado"
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
                                                    event.target
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

                            {editingConfiguration && (
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
                                        Configuración en edición:
                                    </strong>
                                    {" "}
                                    {
                                        editingConfiguration
                                            .code
                                    }
                                    {" | "}
                                    ID:
                                    {" "}
                                    {
                                        editingConfiguration
                                            .id
                                    }
                                    {" | "}
                                    Versión:
                                    {" "}
                                    {
                                        editingConfiguration
                                            .version
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
                                            editingConfiguration
                                                ? "Actualizar"
                                                : (
                                                    selectedCode
                                                        ?.configured
                                                        ? "Reactivar"
                                                        : "Crear"
                                                )
                                        )}
                                </button>

                                {editingConfiguration && (
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
                    Configuraciones Registradas
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
                                maxLength={100}
                                placeholder="Código o nombre visible"
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
                                Registros por página
                            </label>

                            <select
                                value={size}
                                onChange={
                                    event => {
                                        setSize(
                                            Number(
                                                event.target
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
                        Cargando configuraciones...
                    </p>

                ) : configurations.length === 0 ? (
                    <p
                        style={{
                            color: "#64748b"
                        }}
                    >
                        No hay configuraciones que coincidan con los filtros.
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
                                        Orden
                                    </th>

                                    <th>
                                        Código técnico
                                    </th>

                                    <th>
                                        Nombre visible
                                    </th>

                                    <th>
                                        Color
                                    </th>

                                    <th>
                                        Paciente
                                    </th>

                                    <th>
                                        Personal
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
                                {configurations.map(
                                    configuration => (
                                        <tr
                                            key={
                                                configuration.id
                                            }
                                        >
                                            <td>
                                                {
                                                    configuration
                                                        .displayOrder
                                                }
                                            </td>

                                            <td>
                                                <code>
                                                    {
                                                        configuration
                                                            .code
                                                    }
                                                </code>
                                            </td>

                                            <td>
                                                <div
                                                    style={{
                                                        display:
                                                            "flex",
                                                        alignItems:
                                                            "center",
                                                        gap:
                                                            "8px"
                                                    }}
                                                >
                                                    <span
                                                        style={{
                                                            width:
                                                                "14px",
                                                            height:
                                                                "14px",
                                                            borderRadius:
                                                                "50%",
                                                            backgroundColor:
                                                                configuration
                                                                    .color,
                                                            border:
                                                                "1px solid #cbd5e1",
                                                            flexShrink:
                                                                0
                                                        }}
                                                    />

                                                    <strong>
                                                        {
                                                            configuration
                                                                .displayName
                                                        }
                                                    </strong>
                                                </div>
                                            </td>

                                            <td>
                                                <code>
                                                    {
                                                        configuration
                                                            .color
                                                    }
                                                </code>
                                            </td>

                                            <td>
                                                {
                                                    configuration
                                                        .patientVisible
                                                        ? "Sí"
                                                        : "No"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    configuration
                                                        .staffVisible
                                                        ? "Sí"
                                                        : "No"
                                                }
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        configuration
                                                            .active
                                                            ? "status-active"
                                                            : "status-inactive"
                                                    }
                                                >
                                                    {configuration
                                                        .active
                                                        ? "Activo"
                                                        : "Inactivo"}
                                                </span>
                                            </td>

                                            <td>
                                                {
                                                    configuration
                                                        .version
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
                                                                            configuration
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
                                                                        configuration
                                                                            .active
                                                                            ? "btn btn-danger-outline"
                                                                            : "btn btn-primary"
                                                                    }
                                                                    onClick={() =>
                                                                        handleStatusChange(
                                                                            configuration
                                                                        )
                                                                    }
                                                                    disabled={
                                                                        saving
                                                                    }
                                                                >
                                                                    {configuration
                                                                        .active
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
                        configuración(es)
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
 * Convierte un código técnico en un texto visible.
 *
 * PENDIENTE_DE_PAGO
 * ->
 * Pendiente de pago
 */
function formatTechnicalCode(value) {
    if (!value) {
        return "";
    }

    const normalizedValue =
        String(value)
            .toLowerCase()
            .replace(
                /_/g,
                " "
            );

    return (
        normalizedValue
            .charAt(0)
            .toUpperCase() +
        normalizedValue.slice(1)
    );
}

/**
 * El input type=color siempre necesita
 * un valor hexadecimal válido.
 */
function normalizeColorInput(value) {
    if (
        /^#[0-9A-Fa-f]{6}$/
            .test(value || "")
    ) {
        return value;
    }

    return "#2563EB";
}

export default AppointmentStatuses;