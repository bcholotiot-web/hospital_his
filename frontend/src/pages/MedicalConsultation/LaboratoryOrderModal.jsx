import {
    useEffect,
    useMemo,
    useState
} from "react";

import {
    createDoctorLaboratoryOrder,
    getDoctorLaboratoryTests
} from "../../api/medicalConsultationApi";

import "./LaboratoryOrderModal.css";

function LaboratoryOrderModal({
    appointment,
    onClose,
    onCreated
}) {
    const [tests, setTests] =
        useState([]);

    const [selectedTestIds, setSelectedTestIds] =
        useState([]);

    const [searchValue, setSearchValue] =
        useState("");

    const [externalOrder, setExternalOrder] =
        useState(false);

    const [notes, setNotes] =
        useState("");

    const [errors, setErrors] =
        useState({});

    const [generalError, setGeneralError] =
        useState("");

    const [loading, setLoading] =
        useState(true);

    const [processing, setProcessing] =
        useState(false);

    useEffect(() => {
        loadTests();
    }, []);

    const loadTests = async () => {
        try {
            setLoading(true);
            setGeneralError("");

            const response =
                await getDoctorLaboratoryTests();

            const availableTests =
                Array.isArray(response.data)
                    ? response.data
                    : [];

            setTests(availableTests);

            if (availableTests.length === 0) {
                setGeneralError(
                    "No hay exámenes de laboratorio activos disponibles."
                );
            }

        } catch (error) {
            console.error(
                "Error cargando exámenes:",
                error
            );

            setTests([]);

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible cargar el catálogo de exámenes."
                )
            );

        } finally {
            setLoading(false);
        }
    };

    const filteredTests =
        useMemo(() => {
            const query =
                searchValue
                    .trim()
                    .toLowerCase();

            if (!query) {
                return tests;
            }

            return tests.filter(test => {
                const code =
                    String(
                        test.code || ""
                    ).toLowerCase();

                const name =
                    String(
                        test.name || ""
                    ).toLowerCase();

                const description =
                    String(
                        test.description || ""
                    ).toLowerCase();

                return (
                    code.includes(query) ||
                    name.includes(query) ||
                    description.includes(query)
                );
            });
        }, [tests, searchValue]);

    const selectedTests =
        useMemo(() => {
            return tests.filter(test =>
                selectedTestIds.includes(
                    Number(test.id)
                )
            );
        }, [tests, selectedTestIds]);

    const totalAmount =
        useMemo(() => {
            return selectedTests.reduce(
                (total, test) =>
                    total +
                    Number(test.price || 0),
                0
            );
        }, [selectedTests]);

    const updateField = (
        field,
        value
    ) => {
        if (field === "externalOrder") {
            setExternalOrder(
                Boolean(value)
            );
        }

        if (field === "notes") {
            setNotes(value);
        }

        setErrors(previous => ({
            ...previous,
            [field]: ""
        }));

        setGeneralError("");
    };

    const toggleTest = (
        testId
    ) => {
        const numericTestId =
            Number(testId);

        setSelectedTestIds(previous => {
            if (
                previous.includes(
                    numericTestId
                )
            ) {
                return previous.filter(
                    id =>
                        id !== numericTestId
                );
            }

            return [
                ...previous,
                numericTestId
            ];
        });

        setErrors(previous => ({
            ...previous,
            laboratoryTestIds: ""
        }));

        setGeneralError("");
    };

    const selectAllVisible = () => {
        const visibleIds =
            filteredTests.map(test =>
                Number(test.id)
            );

        setSelectedTestIds(previous =>
            Array.from(
                new Set([
                    ...previous,
                    ...visibleIds
                ])
            )
        );

        setErrors(previous => ({
            ...previous,
            laboratoryTestIds: ""
        }));
    };

    const clearSelection = () => {
        setSelectedTestIds([]);

        setErrors(previous => ({
            ...previous,
            laboratoryTestIds: ""
        }));
    };

    const validateForm = () => {
        const newErrors = {};

        if (
            !appointment?.appointmentId
        ) {
            newErrors.appointmentId =
                "No se pudo identificar la cita.";
        }

        if (
            selectedTestIds.length === 0
        ) {
            newErrors.laboratoryTestIds =
                "Debe seleccionar al menos un examen de laboratorio.";
        }

        if (
            selectedTestIds.length > 50
        ) {
            newErrors.laboratoryTestIds =
                "No puede seleccionar más de 50 exámenes por orden.";
        }

        if (
            notes.trim().length > 1000
        ) {
            newErrors.notes =
                "Las observaciones no pueden exceder los 1000 caracteres.";
        }

        setErrors(newErrors);

        return Object.keys(
            newErrors
        ).length === 0;
    };

    const handleSubmit = async (
        event
    ) => {
        event.preventDefault();

        if (processing) {
            return;
        }

        setGeneralError("");

        if (!validateForm()) {
            return;
        }

        const request = {
            appointmentId:
                Number(
                    appointment.appointmentId
                ),

            laboratoryTestIds:
                selectedTestIds,

            externalOrder:
                Boolean(externalOrder),

            notes:
                notes.trim() || null
        };

        try {
            setProcessing(true);

            const response =
                await createDoctorLaboratoryOrder(
                    request
                );

            onCreated(response.data);

        } catch (error) {
            console.error(
                "Error creando orden:",
                error
            );

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible crear la orden de laboratorio."
                )
            );

        } finally {
            setProcessing(false);
        }
    };

    return (
        <div
            className="doctor-lab-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="doctor-lab-title"
        >
            <div className="doctor-lab-modal">
                <header className="doctor-lab-header">
                    <div>
                        <h2 id="doctor-lab-title">
                            Generar Orden de Laboratorio
                        </h2>

                        <p>
                            Cita #
                            {appointment.appointmentId}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="doctor-lab-close"
                        onClick={onClose}
                        disabled={processing}
                        aria-label="Cerrar"
                    >
                        ×
                    </button>
                </header>

                <section className="doctor-lab-context">
                    <div>
                        <span>Paciente</span>

                        <strong>
                            {appointment.patientName}
                        </strong>
                    </div>

                    <div>
                        <span>Especialidad</span>

                        <strong>
                            {appointment.specialty}
                        </strong>
                    </div>

                    <div>
                        <span>Sucursal</span>

                        <strong>
                            {appointment.branch}
                        </strong>
                    </div>

                    <div>
                        <span>Estado</span>

                        <strong>
                            {formatStatus(
                                appointment
                                    .appointmentStatus
                            )}
                        </strong>
                    </div>
                </section>

                {generalError && (
                    <div className="doctor-lab-error">
                        {generalError}
                    </div>
                )}

                {errors.appointmentId && (
                    <div className="doctor-lab-error">
                        {errors.appointmentId}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="doctor-lab-search">
                        <label htmlFor="laboratory-test-search">
                            Buscar examen
                        </label>

                        <input
                            id="laboratory-test-search"
                            type="text"
                            maxLength="100"
                            placeholder={
                                "Buscar por nombre, código o descripción"
                            }
                            value={searchValue}
                            onChange={(event) =>
                                setSearchValue(
                                    event.target.value
                                )
                            }
                        />
                    </div>

                    <div className="doctor-lab-selection-actions">
                        <button
                            type="button"
                            className="doctor-lab-selection-button"
                            onClick={selectAllVisible}
                            disabled={
                                filteredTests.length === 0
                            }
                        >
                            Seleccionar visibles
                        </button>

                        <button
                            type="button"
                            className="doctor-lab-selection-button"
                            onClick={clearSelection}
                            disabled={
                                selectedTestIds.length === 0
                            }
                        >
                            Limpiar selección
                        </button>
                    </div>

                    {errors.laboratoryTestIds && (
                        <div className="error-message">
                            {
                                errors
                                    .laboratoryTestIds
                            }
                        </div>
                    )}

                    {loading ? (
                        <div className="doctor-lab-loading">
                            Cargando catálogo de exámenes...
                        </div>

                    ) : filteredTests.length === 0 ? (
                        <div className="doctor-lab-empty">
                            No se encontraron exámenes.
                        </div>

                    ) : (
                        <section className="doctor-lab-tests">
                            {filteredTests.map(test => {
                                const selected =
                                    selectedTestIds
                                        .includes(
                                            Number(test.id)
                                        );

                                return (
                                    <label
                                        key={test.id}
                                        className={
                                            selected
                                                ? "doctor-lab-test selected"
                                                : "doctor-lab-test"
                                        }
                                    >
                                        <input
                                            type="checkbox"
                                            checked={selected}
                                            onChange={() =>
                                                toggleTest(
                                                    test.id
                                                )
                                            }
                                        />

                                        <div className="doctor-lab-test-info">
                                            <div>
                                                <strong>
                                                    {test.name}
                                                </strong>

                                                <span>
                                                    {test.code}
                                                </span>
                                            </div>

                                            <b>
                                                {formatAmount(
                                                    test.price
                                                )}
                                            </b>

                                            {test.description && (
                                                <p>
                                                    {
                                                        test
                                                            .description
                                                    }
                                                </p>
                                            )}

                                            <small>
                                                Rango de referencia:{" "}
                                                {test.referenceRange ||
                                                    "No configurado"}
                                            </small>
                                        </div>
                                    </label>
                                );
                            })}
                        </section>
                    )}

                    <section className="doctor-lab-summary">
                        <div>
                            <span>
                                Exámenes seleccionados
                            </span>

                            <strong>
                                {
                                    selectedTestIds
                                        .length
                                }
                            </strong>
                        </div>

                        <div>
                            <span>Total de la orden</span>

                            <strong>
                                {formatAmount(
                                    totalAmount
                                )}
                            </strong>
                        </div>
                    </section>

                    <label className="doctor-lab-external">
                        <input
                            type="checkbox"
                            checked={externalOrder}
                            onChange={(event) =>
                                updateField(
                                    "externalOrder",
                                    event.target.checked
                                )
                            }
                        />

                        <div>
                            <strong>
                                Orden externa
                            </strong>

                            <span>
                                Los exámenes se realizarán
                                fuera del hospital y los
                                resultados serán presentados
                                posteriormente al médico.
                            </span>
                        </div>
                    </label>

                    <div className="doctor-lab-notes">
                        <label htmlFor="laboratory-order-notes">
                            Observaciones
                        </label>

                        <textarea
                            id="laboratory-order-notes"
                            rows="4"
                            maxLength="1000"
                            value={notes}
                            className={
                                errors.notes
                                    ? "input-error"
                                    : ""
                            }
                            onChange={(event) =>
                                updateField(
                                    "notes",
                                    event.target.value
                                )
                            }
                        />

                        <small>
                            {notes.length} / 1000
                        </small>

                        {errors.notes && (
                            <div className="error-message">
                                {errors.notes}
                            </div>
                        )}
                    </div>

                    <div className="doctor-lab-actions">
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={onClose}
                            disabled={processing}
                        >
                            Cancelar
                        </button>

                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={
                                processing ||
                                loading ||
                                selectedTestIds
                                    .length === 0
                            }
                        >
                            {processing
                                ? "Generando orden..."
                                : `Generar Orden ${formatAmount(
                                    totalAmount
                                )}`}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

function formatStatus(status) {
    if (!status) {
        return "No disponible";
    }

    return status.replaceAll(
        "_",
        " "
    );
}

function formatAmount(value) {
    return `Q${Number(
        value || 0
    ).toFixed(2)}`;
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

    return data?.message || fallback;
}

export default LaboratoryOrderModal;