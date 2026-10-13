import {
    useCallback,
    useEffect,
    useState
} from "react";

import MainLayout from "../../layouts/MainLayout";

import {
    changeLaboratoryTestStatus,
    createLaboratoryTest,
    getLaboratoryTestsPage,
    updateLaboratoryTest
} from "../../api/laboratoryTestAdminApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

function LaboratoryTests() {
    const {
        hasPermission
    } = useAuth();

    /**
     * Permisos granulares.
     */
    const canCreate =
        hasPermission(
            "LABORATORY_TEST_CREATE"
        );

    const canUpdate =
        hasPermission(
            "LABORATORY_TEST_UPDATE"
        );

    const canChangeStatus =
        hasPermission(
            "LABORATORY_TEST_DELETE"
        );

    /**
     * Listado y carga.
     */
    const [
        laboratoryTests,
        setLaboratoryTests
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
     * Formulario.
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
        description,
        setDescription
    ] = useState("");

    const [
        referenceRange,
        setReferenceRange
    ] = useState("");

    const [
        defaultUnit,
        setDefaultUnit
    ] = useState("");

    const [
        price,
        setPrice
    ] = useState("");

    const [
        editingTest,
        setEditingTest
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
     * Carga el listado administrativo.
     */
    const loadLaboratoryTests =
        useCallback(async () => {
            try {
                setLoading(true);

                const response =
                    await getLaboratoryTestsPage({
                        search:
                            searchFilter,
                        active:
                            activeFilter,
                        page,
                        size
                    });

                const pageData =
                    response.data;

                setLaboratoryTests(
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
                    "Error al cargar exámenes:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar los exámenes de laboratorio."
                );

            } finally {
                setLoading(false);
            }
        }, [
            searchFilter,
            activeFilter,
            page,
            size
        ]);

    useEffect(() => {
        loadLaboratoryTests();
    }, [loadLaboratoryTests]);

    /**
     * Valida el formulario.
     */
    const validateForm = () => {
        const newErrors = {};

        const cleanCode =
            code.trim();

        const cleanName =
            name.trim();

        const cleanDescription =
            description.trim();

        const cleanReferenceRange =
            referenceRange.trim();

        const cleanDefaultUnit =
            defaultUnit.trim();

        const numericPrice =
            Number(price);

        if (!cleanCode) {
            newErrors.code =
                "El código del examen es obligatorio.";

        } else if (
            cleanCode.length > 30
        ) {
            newErrors.code =
                "El código no puede exceder los 30 caracteres.";

        } else if (
            !/^[A-Za-z0-9_-]+$/
                .test(cleanCode)
        ) {
            newErrors.code =
                "El código solamente puede contener letras, números, guion y guion bajo.";
        }

        if (!cleanName) {
            newErrors.name =
                "El nombre del examen es obligatorio.";

        } else if (
            cleanName.length > 200
        ) {
            newErrors.name =
                "El nombre no puede exceder los 200 caracteres.";
        }

        if (
            cleanDescription.length > 500
        ) {
            newErrors.description =
                "La descripción no puede exceder los 500 caracteres.";
        }

        if (
            cleanReferenceRange.length > 200
        ) {
            newErrors.referenceRange =
                "El rango de referencia no puede exceder los 200 caracteres.";
        }

        if (
            cleanDefaultUnit.length > 50
        ) {
            newErrors.defaultUnit =
                "La unidad no puede exceder los 50 caracteres.";
        }

        if (
            price === null ||
            price === undefined ||
            String(price).trim() === ""
        ) {
            newErrors.price =
                "El precio del examen es obligatorio.";

        } else if (
            Number.isNaN(numericPrice)
        ) {
            newErrors.price =
                "El precio debe ser un número válido.";

        } else if (
            numericPrice <= 0
        ) {
            newErrors.price =
                "El precio debe ser mayor que cero.";

        } else if (
            numericPrice > 99999999.99
        ) {
            newErrors.price =
                "El precio admite como máximo 8 dígitos enteros.";

        } else if (
            !hasMaximumTwoDecimals(
                String(price)
            )
        ) {
            newErrors.price =
                "El precio admite como máximo 2 decimales.";
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
        setDescription("");
        setReferenceRange("");
        setDefaultUnit("");
        setPrice("");
        setEditingTest(null);
        setErrors({});
    };

    /**
     * Crea o actualiza un examen.
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
                description:
                    description.trim() ||
                    null,
                referenceRange:
                    referenceRange.trim() ||
                    null,
                defaultUnit:
                    defaultUnit.trim() ||
                    null,
                price:
                    Number(price)
            };

            try {
                setSaving(true);

                if (editingTest) {
                    if (!canUpdate) {
                        alert(
                            "No tiene permiso para actualizar exámenes de laboratorio."
                        );

                        return;
                    }

                    const response =
                        await updateLaboratoryTest(
                            editingTest.id,
                            {
                                ...normalizedData,
                                version:
                                    editingTest
                                        .version
                            }
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Examen de laboratorio actualizado correctamente."
                    );

                } else {
                    if (!canCreate) {
                        alert(
                            "No tiene permiso para crear exámenes de laboratorio."
                        );

                        return;
                    }

                    const response =
                        await createLaboratoryTest(
                            normalizedData
                        );

                    alert(
                        response.data
                            ?.message ||
                        "Examen de laboratorio creado correctamente."
                    );
                }

                clearForm();

                await loadLaboratoryTests();

            } catch (error) {
                console.error(
                    "Error al guardar examen:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible guardar el examen de laboratorio."
                );

                if (
                    error.response
                        ?.status === 409
                ) {
                    clearForm();

                    await loadLaboratoryTests();
                }

            } finally {
                setSaving(false);
            }
        };

    /**
     * Carga un examen en el formulario.
     */
    const handleEdit = test => {
        if (!canUpdate) {
            return;
        }

        setEditingTest(test);

        setCode(
            test.code || ""
        );

        setName(
            test.name || ""
        );

        setDescription(
            test.description || ""
        );

        setReferenceRange(
            test.referenceRange || ""
        );

        setDefaultUnit(
            test.defaultUnit || ""
        );

        setPrice(
            test.price != null
                ? String(test.price)
                : ""
        );

        setErrors({});

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    /**
     * Activa o desactiva un examen.
     */
    const handleStatusChange =
        async test => {
            if (!canChangeStatus) {
                alert(
                    "No tiene permiso para cambiar el estado de los exámenes."
                );

                return;
            }

            const newStatus =
                !test.active;

            const actionText =
                newStatus
                    ? "reactivar"
                    : "desactivar";

            const confirmed =
                window.confirm(
                    `¿Desea ${actionText} el examen "${test.name}"?`
                );

            if (!confirmed) {
                return;
            }

            try {
                setSaving(true);

                const response =
                    await changeLaboratoryTestStatus(
                        test.id,
                        newStatus,
                        test.version
                    );

                alert(
                    response.data
                        ?.message ||
                    (
                        newStatus
                            ? "Examen reactivado correctamente."
                            : "Examen desactivado correctamente."
                    )
                );

                if (
                    editingTest?.id ===
                    test.id
                ) {
                    clearForm();
                }

                await loadLaboratoryTests();

            } catch (error) {
                console.error(
                    "Error al cambiar estado:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cambiar el estado del examen."
                );

                await loadLaboratoryTests();

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
                loadLaboratoryTests();
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
        setPage(0);
    };

    return (
        <MainLayout>
            <h1 className="page-title">
                Exámenes de Laboratorio
            </h1>

            {(canCreate ||
                (
                    editingTest &&
                    canUpdate
                )) && (
                    <div className="card">
                        <h2 className="section-title">
                            {editingTest
                                ? "Editar Examen"
                                : "Crear Examen"}
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
                                        maxLength={30}
                                        placeholder="Ejemplo: GLU-001"
                                        value={code}
                                        className={
                                            errors.code
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setCode(
                                                    event.target
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
                                        {code.length}/30
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Nombre
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={200}
                                        placeholder="Nombre del examen"
                                        value={name}
                                        className={
                                            errors.name
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setName(
                                                    event.target
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
                                        Rango de referencia
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={200}
                                        placeholder="Ejemplo: 70 - 100"
                                        value={
                                            referenceRange
                                        }
                                        className={
                                            errors.referenceRange
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setReferenceRange(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.referenceRange && (
                                        <div className="error-message">
                                            {
                                                errors.referenceRange
                                            }
                                        </div>
                                    )}

                                    <small>
                                        Campo opcional.
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Unidad predeterminada
                                    </label>

                                    <input
                                        type="text"
                                        maxLength={50}
                                        placeholder="Ejemplo: mg/dL"
                                        value={
                                            defaultUnit
                                        }
                                        className={
                                            errors.defaultUnit
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setDefaultUnit(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.defaultUnit && (
                                        <div className="error-message">
                                            {
                                                errors.defaultUnit
                                            }
                                        </div>
                                    )}

                                    <small>
                                        Campo opcional.
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Precio
                                    </label>

                                    <input
                                        type="number"
                                        min="0.01"
                                        max="99999999.99"
                                        step="0.01"
                                        placeholder="0.00"
                                        value={price}
                                        className={
                                            errors.price
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            event =>
                                                setPrice(
                                                    event.target
                                                        .value
                                                )
                                        }
                                    />

                                    {errors.price && (
                                        <div className="error-message">
                                            {errors.price}
                                        </div>
                                    )}

                                    <small>
                                        Precio en quetzales, con máximo dos decimales.
                                    </small>
                                </div>

                                <div className="form-group">
                                    <label>
                                        Descripción
                                    </label>

                                    <textarea
                                        rows={4}
                                        maxLength={500}
                                        placeholder="Descripción administrativa o clínica"
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

                            {editingTest && (
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

                                    ID {editingTest.id}

                                    {" | "}

                                    Versión {
                                        editingTest.version
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
                                            editingTest
                                                ? "Actualizar"
                                                : "Crear"
                                        )}
                                </button>

                                {editingTest && (
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
                    Listado de Exámenes
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
                                placeholder="Código o nombre del examen"
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
                        Cargando exámenes de laboratorio...
                    </p>

                ) : laboratoryTests.length === 0 ? (
                    <p
                        style={{
                            color: "#64748b"
                        }}
                    >
                        No hay exámenes que coincidan con los filtros.
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
                                        Nombre
                                    </th>

                                    <th>
                                        Rango
                                    </th>

                                    <th>
                                        Unidad
                                    </th>

                                    <th>
                                        Precio
                                    </th>

                                    <th>
                                        Estado
                                    </th>

                                    <th>
                                        Modificado por
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
                                {laboratoryTests.map(
                                    test => (
                                        <tr
                                            key={
                                                test.id
                                            }
                                        >
                                            <td>
                                                <strong>
                                                    {
                                                        test.code
                                                    }
                                                </strong>
                                            </td>

                                            <td>
                                                {
                                                    test.name
                                                }
                                            </td>

                                            <td>
                                                {
                                                    test.referenceRange ||
                                                    "No indicado"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    test.defaultUnit ||
                                                    "No indicada"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    formatCurrency(
                                                        test.price
                                                    )
                                                }
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        test.active
                                                            ? "status-active"
                                                            : "status-inactive"
                                                    }
                                                >
                                                    {test.active
                                                        ? "Activo"
                                                        : "Inactivo"}
                                                </span>
                                            </td>

                                            <td>
                                                {
                                                    test.updatedBy ||
                                                    "Sin información"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    test.version
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
                                                                            test
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
                                                                        test.active
                                                                            ? "btn btn-danger-outline"
                                                                            : "btn btn-primary"
                                                                    }
                                                                    onClick={() =>
                                                                        handleStatusChange(
                                                                            test
                                                                        )
                                                                    }
                                                                    disabled={
                                                                        saving
                                                                    }
                                                                >
                                                                    {test.active
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
 * Comprueba que el precio tenga como máximo
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
 * Formatea el precio en quetzales.
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

export default LaboratoryTests;