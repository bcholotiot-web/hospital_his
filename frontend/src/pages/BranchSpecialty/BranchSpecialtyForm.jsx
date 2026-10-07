import {
    useEffect,
    useState
} from "react";

import {
    Link,
    useLocation,
    useNavigate
} from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import {
    createBranchSpecialtyAssignment,
    getActiveBranchOptions,
    getActiveSpecialtyOptions
} from "../../api/branchSpecialtyApi";

import "./BranchSpecialtyForm.css";

function BranchSpecialtyForm() {
    const navigate =
        useNavigate();

    const location =
        useLocation();

    const [branches, setBranches] =
        useState([]);

    const [specialties, setSpecialties] =
        useState([]);

    /*
     * Cuando se abre desde Reactivar, los IDs
     * se reciben mediante location.state.
     */
    const [branchId, setBranchId] =
        useState(
            location.state?.branchId
                ? String(
                    location.state.branchId
                )
                : ""
        );

    const [specialtyId, setSpecialtyId] =
        useState(
            location.state?.specialtyId
                ? String(
                    location.state.specialtyId
                )
                : ""
        );

    const [errors, setErrors] =
        useState({});

    const [generalError, setGeneralError] =
        useState("");

    const [loading, setLoading] =
        useState(true);

    const [processing, setProcessing] =
        useState(false);

    useEffect(() => {
        loadOptions();
    }, []);

    /**
     * Carga sedes y especialidades activas.
     *
     * Los dos endpoints se ejecutan en paralelo.
     */
    const loadOptions = async () => {
        try {
            setLoading(true);
            setGeneralError("");

            const [
                branchesResponse,
                specialtiesResponse
            ] = await Promise.all([
                getActiveBranchOptions(),
                getActiveSpecialtyOptions()
            ]);

            setBranches(
                Array.isArray(
                    branchesResponse.data
                )
                    ? branchesResponse.data
                    : []
            );

            setSpecialties(
                Array.isArray(
                    specialtiesResponse.data
                )
                    ? specialtiesResponse.data
                    : []
            );

        } catch (error) {
            setBranches([]);
            setSpecialties([]);

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible cargar los catálogos activos."
                )
            );
        } finally {
            setLoading(false);
        }
    };

    /**
     * Valida visualmente los campos.
     *
     * Spring Boot repetirá estas validaciones
     * antes de guardar.
     */
    const validateForm = () => {
        const newErrors = {};

        if (!branchId) {
            newErrors.branchId =
                "Debe seleccionar una sede.";
        }

        if (!specialtyId) {
            newErrors.specialtyId =
                "Debe seleccionar una especialidad.";
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors).length === 0
        );
    };

    /**
     * Envía únicamente los identificadores.
     *
     * El backend controla:
     *
     * - Catálogos activos.
     * - Duplicidad.
     * - Reactivación.
     * - Auditoría.
     * - Fechas.
     * - Usuario.
     */
    const handleSubmit = async (event) => {
        event.preventDefault();

        if (processing) {
            return;
        }

        setGeneralError("");

        if (!validateForm()) {
            return;
        }

        try {
            setProcessing(true);

            const response =
                await createBranchSpecialtyAssignment(
                    Number(branchId),
                    Number(specialtyId)
                );

            navigate(
                "/branch-specialties",
                {
                    replace: true,

                    state: {
                        successMessage:
                            response.data.message
                    }
                }
            );

        } catch (error) {
            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible guardar la asignación."
                )
            );
        } finally {
            setProcessing(false);
        }
    };

    return (
        <MainLayout>
            <div className="branch-specialty-form-page">
                <header className="branch-specialty-form-header">
                    <div>
                        <h1>
                            Asignar Especialidad
                        </h1>

                        <p>
                            Seleccione una sede y una
                            especialidad activa.
                        </p>
                    </div>

                    <Link
                        to="/branch-specialties"
                        className="branch-specialty-form-back"
                    >
                        Volver al listado
                    </Link>
                </header>

                {generalError && (
                    <div
                        className="branch-specialty-form-message error"
                        role="alert"
                    >
                        {generalError}
                    </div>
                )}

                <form
                    className="branch-specialty-form-card"
                    onSubmit={handleSubmit}
                >
                    <div className="branch-specialty-form-group">
                        <label htmlFor="branch-id">
                            Sede
                        </label>

                        <select
                            id="branch-id"
                            value={branchId}
                            onChange={(event) => {
                                setBranchId(
                                    event.target.value
                                );

                                setErrors(previous => ({
                                    ...previous,
                                    branchId: ""
                                }));

                                setGeneralError("");
                            }}
                            disabled={
                                loading ||
                                processing
                            }
                        >
                            <option value="">
                                Seleccione una sede
                            </option>

                            {branches.map(branch => (
                                <option
                                    key={branch.id}
                                    value={branch.id}
                                >
                                    {branch.name}
                                    {branch.description
                                        ? ` · ${branch.description}`
                                        : ""}
                                </option>
                            ))}
                        </select>

                        {errors.branchId && (
                            <div className="branch-specialty-form-field-error">
                                {errors.branchId}
                            </div>
                        )}
                    </div>

                    <div className="branch-specialty-form-group">
                        <label htmlFor="specialty-id">
                            Especialidad
                        </label>

                        <select
                            id="specialty-id"
                            value={specialtyId}
                            onChange={(event) => {
                                setSpecialtyId(
                                    event.target.value
                                );

                                setErrors(previous => ({
                                    ...previous,
                                    specialtyId: ""
                                }));

                                setGeneralError("");
                            }}
                            disabled={
                                loading ||
                                processing
                            }
                        >
                            <option value="">
                                Seleccione una especialidad
                            </option>

                            {specialties.map(
                                specialty => (
                                    <option
                                        key={specialty.id}
                                        value={specialty.id}
                                    >
                                        {
                                            specialty.name
                                        }
                                        {specialty.description
                                            ? ` · ${specialty.description}`
                                            : ""}
                                    </option>
                                ))}
                        </select>

                        {errors.specialtyId && (
                            <div className="branch-specialty-form-field-error">
                                {
                                    errors.specialtyId
                                }
                            </div>
                        )}
                    </div>

                    {/* <div className="branch-specialty-form-notice">
                        La asignación será creada como
                        activa. Si la combinación existió
                        anteriormente y está inactiva,
                        Spring Boot reactivará el mismo
                        registro. 
                    </div>*/}

                    <div className="branch-specialty-form-actions">
                        <Link
                            to="/branch-specialties"
                            className="secondary"
                        >
                            Cancelar
                        </Link>

                        <button
                            type="submit"
                            className="primary"
                            disabled={
                                loading ||
                                processing ||
                                branches.length === 0 ||
                                specialties.length === 0
                            }
                        >
                            {processing
                                ? "Guardando..."
                                : "Asignar especialidad"}
                        </button>
                    </div>
                </form>
            </div>
        </MainLayout>
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

    const validationErrors =
        data?.validationErrors;

    if (validationErrors) {
        const messages =
            Object.values(
                validationErrors
            );

        if (messages.length > 0) {
            return messages[0];
        }
    }

    return data?.message || fallback;
}

export default BranchSpecialtyForm;