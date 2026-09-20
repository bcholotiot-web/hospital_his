import {
    useEffect,
    useMemo,
    useState
} from "react";

import {
    createDoctorPrescription,
    getDoctorMedicationCatalog
} from "../../api/medicalConsultationApi";

import "./PrescriptionModal.css";

/**
 * Permite crear una receta médica desde una
 * consulta finalizada.
 *
 * El frontend envía únicamente:
 *
 * - ID de la cita.
 * - Medicamentos seleccionados.
 * - Dosis.
 * - Frecuencia.
 * - Duración.
 * - Cantidad.
 * - Indicaciones.
 *
 * El backend obtiene y valida:
 *
 * - Consulta.
 * - Médico.
 * - Paciente.
 * - Estado.
 * - Fecha.
 * - Medicamentos activos.
 * - Una sola receta activa por consulta.
 */
function PrescriptionModal({
    appointment,
    onClose,
    onCreated
}) {
    /*
     * Catálogo activo obtenido desde PostgreSQL
     * mediante Spring Boot.
     */
    const [medications, setMedications] =
        useState([]);

    /*
     * Texto utilizado para filtrar visualmente
     * el catálogo cargado.
     */
    const [searchValue, setSearchValue] =
        useState("");

    /*
     * IDs de medicamentos agregados a la receta.
     */
    const [selectedMedicationIds, setSelectedMedicationIds] =
        useState([]);

    /*
     * Datos específicos de cada medicamento:
     *
     * dosage
     * frequency
     * duration
     * prescribedQuantity
     * specialInstructions
     */
    const [itemForms, setItemForms] =
        useState({});

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

    /*
     * Cargar el catálogo al abrir el modal.
     */
    useEffect(() => {
        loadMedicationCatalog();
    }, []);

    /**
     * Obtiene los medicamentos activos desde el backend.
     *
     * React no contiene un catálogo estático.
     */
    const loadMedicationCatalog = async () => {
        try {
            setLoading(true);
            setGeneralError("");

            const response =
                await getDoctorMedicationCatalog();

            const catalog =
                Array.isArray(response.data)
                    ? response.data
                    : [];

            setMedications(catalog);

            if (catalog.length === 0) {
                setGeneralError(
                    "No existen medicamentos activos disponibles."
                );
            }

        } catch (error) {
            console.error(
                "Error cargando medicamentos:",
                error
            );

            setMedications([]);

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible cargar el catálogo de medicamentos."
                )
            );
        } finally {
            setLoading(false);
        }
    };

    /**
     * Filtra el catálogo por:
     *
     * - Código.
     * - Nombre.
     * - Principio activo.
     * - Concentración.
     * - Presentación.
     */
    const filteredMedications =
        useMemo(() => {
            const query =
                searchValue
                    .trim()
                    .toLowerCase();

            if (!query) {
                return medications;
            }

            return medications.filter(
                medication => {
                    const searchableText = [
                        medication.code,
                        medication.name,
                        medication.activeIngredient,
                        medication.concentration,
                        medication.presentation,
                        medication.description
                    ]
                        .map(value =>
                            String(value || "")
                                .toLowerCase()
                        )
                        .join(" ");

                    return searchableText.includes(
                        query
                    );
                }
            );
        }, [
            medications,
            searchValue
        ]);

    /**
     * Devuelve los medicamentos actualmente
     * agregados a la receta.
     */
    const selectedMedications = useMemo(() => {
        return medications.filter(
            medication =>
                selectedMedicationIds.includes(
                    Number(medication.id)
                )
        );
    }, [
        medications,
        selectedMedicationIds
    ]);

    /**
     * Agrega o elimina un medicamento de la receta.
     *
     * Al agregarlo, crea su formulario clínico vacío.
     */
    const toggleMedication = (
        medicationId
    ) => {
        const numericId =
            Number(medicationId);

        setSelectedMedicationIds(
            previous => {
                if (
                    previous.includes(
                        numericId
                    )
                ) {
                    /*
                     * Eliminar también los datos del formulario.
                     */
                    setItemForms(
                        previousForms => {
                            const nextForms = {
                                ...previousForms
                            };

                            delete nextForms[
                                numericId
                            ];

                            return nextForms;
                        }
                    );

                    return previous.filter(
                        id => id !== numericId
                    );
                }

                /*
                 * Crear valores iniciales del medicamento.
                 */
                setItemForms(
                    previousForms => ({
                        ...previousForms,
                        [numericId]: {
                            dosage: "",
                            frequency: "",
                            duration: "",
                            prescribedQuantity: "",
                            specialInstructions: ""
                        }
                    })
                );

                return [
                    ...previous,
                    numericId
                ];
            }
        );

        setErrors(previous => ({
            ...previous,
            items: "",
            [`medication-${numericId}`]: ""
        }));

        setGeneralError("");
    };

    /**
     * Actualiza un campo clínico del medicamento.
     */
    const updateMedicationField = (medicationId, field, value) => {
        setItemForms(previous => ({
            ...previous,
            [medicationId]: {
                ...previous[medicationId],
                [field]: value
            }
        }));

        setErrors(previous => ({
            ...previous,
            [`medication-${medicationId}`]: ""
        }));

        setGeneralError("");
    };

    /**
     * Selecciona todos los medicamentos visibles.
     *
     * Respeta el máximo de 30 medicamentos definido
     * por el backend.
     */
    const selectAllVisible = () => {
        const visibleIds =
            filteredMedications
                .map(medication =>
                    Number(medication.id)
                );

        const mergedIds =
            Array.from(
                new Set([
                    ...selectedMedicationIds,
                    ...visibleIds
                ])
            )
                .slice(0, 30);

        const nextForms = {
            ...itemForms
        };

        mergedIds.forEach(id => {
            if (!nextForms[id]) {
                nextForms[id] = {
                    dosage: "",
                    frequency: "",
                    duration: "",
                    prescribedQuantity: "",
                    specialInstructions: ""
                };
            }
        });

        setSelectedMedicationIds(
            mergedIds
        );

        setItemForms(
            nextForms
        );

        setErrors(previous => ({
            ...previous,
            items: ""
        }));
    };

    /**
     * Elimina toda la selección actual.
     */
    const clearSelection = () => {
        setSelectedMedicationIds([]);
        setItemForms({});

        setErrors(previous => ({
            ...previous,
            items: ""
        }));
    };

    /**
     * Valida el formulario antes de enviarlo.
     *
     * Spring Boot repetirá todas las validaciones.
     */
    const validateForm = () => {
        const newErrors = {};

        if (
            !appointment?.appointmentId
        ) {
            newErrors.appointmentId =
                "No se pudo identificar la cita médica.";
        }

        if (
            selectedMedicationIds.length === 0
        ) {
            newErrors.items =
                "Debe agregar al menos un medicamento.";
        }

        if (
            selectedMedicationIds.length > 30
        ) {
            newErrors.items =
                "No puede agregar más de 30 medicamentos.";
        }

        selectedMedicationIds.forEach(
            medicationId => {
                const form =
                    itemForms[medicationId];

                const medication =
                    medications.find(
                        item =>
                            Number(item.id) ===
                            Number(medicationId)
                    );

                if (!form) {
                    newErrors[
                        `medication-${medicationId}`
                    ] =
                        "Los datos del medicamento son obligatorios.";

                    return;
                }

                if (!form.dosage.trim()) {
                    newErrors[
                        `medication-${medicationId}`
                    ] =
                        `Debe indicar la dosis de ${medication?.name ||
                        "este medicamento"
                        }.`;

                } else if (
                    form.dosage.trim().length > 100
                ) {
                    newErrors[
                        `medication-${medicationId}`
                    ] =
                        "La dosis no puede exceder 100 caracteres.";

                } else if (
                    !form.frequency.trim()
                ) {
                    newErrors[
                        `medication-${medicationId}`
                    ] =
                        `Debe indicar la frecuencia de ${medication?.name ||
                        "este medicamento"
                        }.`;

                } else if (
                    form.frequency.trim().length > 150
                ) {
                    newErrors[
                        `medication-${medicationId}`
                    ] =
                        "La frecuencia no puede exceder 150 caracteres.";

                } else if (
                    !form.duration.trim()
                ) {
                    newErrors[
                        `medication-${medicationId}`
                    ] =
                        `Debe indicar la duración para ${medication?.name ||
                        "este medicamento"
                        }.`;

                } else if (
                    form.duration.trim().length > 100
                ) {
                    newErrors[
                        `medication-${medicationId}`
                    ] =
                        "La duración no puede exceder 100 caracteres.";

                } else {
                    const quantity =
                        Number(
                            form.prescribedQuantity
                        );

                    if (
                        !Number.isInteger(quantity) ||
                        quantity <= 0
                    ) {
                        newErrors[
                            `medication-${medicationId}`
                        ] =
                            "La cantidad debe ser un número entero mayor que cero.";

                    } else if (
                        quantity > 10000
                    ) {
                        newErrors[
                            `medication-${medicationId}`
                        ] =
                            "La cantidad no puede exceder 10000 unidades.";

                    } else if (
                        form.specialInstructions
                            .trim()
                            .length > 1000
                    ) {
                        newErrors[
                            `medication-${medicationId}`
                        ] =
                            "Las indicaciones no pueden exceder 1000 caracteres.";
                    }
                }
            }
        );

        if (
            notes.trim().length > 1000
        ) {
            newErrors.notes =
                "Las observaciones generales no pueden exceder 1000 caracteres.";
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors).length === 0
        );
    };

    /**
     * Construye y envía la receta.
     *
     * No envía:
     *
     * - Médico.
     * - Paciente.
     * - Consulta.
     * - Estado.
     * - Fecha de emisión.
     * - Precios.
     */
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

            items:
                selectedMedicationIds.map(
                    medicationId => {
                        const form =
                            itemForms[
                            medicationId
                            ];

                        return {
                            medicationId:
                                Number(
                                    medicationId
                                ),

                            dosage:
                                form.dosage.trim(),

                            frequency:
                                form.frequency.trim(),

                            duration:
                                form.duration.trim(),

                            prescribedQuantity:
                                Number(
                                    form
                                        .prescribedQuantity
                                ),

                            specialInstructions:
                                form
                                    .specialInstructions
                                    .trim() ||
                                null
                        };
                    }
                ),

            notes:
                notes.trim() || null
        };

        try {
            setProcessing(true);

            const response =
                await createDoctorPrescription(
                    request
                );

            if (onCreated) {
                onCreated(
                    response.data
                );
            }

        } catch (error) {
            console.error(
                "Error creando receta:",
                error
            );

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible crear la receta médica."
                )
            );
        } finally {
            setProcessing(false);
        }
    };

    return (
        <div
            className="prescription-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="prescription-modal-title"
        >
            <div className="prescription-modal">
                <header className="prescription-header">
                    <div>
                        <h2 id="prescription-modal-title">
                            Generar Receta Médica
                        </h2>

                        <p>
                            Cita #{appointment
                                ?.appointmentId}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="prescription-close"
                        onClick={onClose}
                        disabled={processing}
                        aria-label="Cerrar"
                    >
                        ×
                    </button>
                </header>

                <section className="prescription-context">
                    <div>
                        <span>Paciente</span>

                        <strong>
                            {appointment
                                ?.patientName ||
                                "No disponible"}
                        </strong>
                    </div>

                    <div>
                        <span>Especialidad</span>

                        <strong>
                            {appointment
                                ?.specialty ||
                                "No disponible"}
                        </strong>
                    </div>

                    <div>
                        <span>Sucursal</span>

                        <strong>
                            {appointment
                                ?.branch ||
                                "No disponible"}
                        </strong>
                    </div>

                    <div>
                        <span>Estado</span>

                        <strong>
                            {formatStatus(
                                appointment
                                    ?.appointmentStatus
                            )}
                        </strong>
                    </div>
                </section>

                {generalError && (
                    <div
                        className="prescription-error"
                        role="alert"
                    >
                        {generalError}
                    </div>
                )}

                {errors.appointmentId && (
                    <div className="prescription-error">
                        {errors.appointmentId}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="prescription-search">
                        <label htmlFor="medication-search">
                            Buscar medicamento
                        </label>

                        <input
                            id="medication-search"
                            type="text"
                            maxLength={100}
                            placeholder="Buscar por código, nombre, principio activo o presentación"
                            value={searchValue}
                            onChange={(event) => {
                                setSearchValue(
                                    event.target.value
                                );

                                setGeneralError("");
                            }}
                            disabled={
                                processing ||
                                loading
                            }
                        />
                    </div>

                    <div className="prescription-selection-actions">
                        <button
                            type="button"
                            onClick={selectAllVisible}
                            disabled={
                                processing ||
                                loading ||
                                filteredMedications
                                    .length === 0
                            }
                        >
                            Seleccionar visibles
                        </button>

                        <button
                            type="button"
                            onClick={clearSelection}
                            disabled={
                                processing ||
                                selectedMedicationIds
                                    .length === 0
                            }
                        >
                            Limpiar selección
                        </button>
                    </div>

                    {errors.items && (
                        <div className="prescription-field-error">
                            {errors.items}
                        </div>
                    )}

                    {loading ? (
                        <div className="prescription-empty">
                            Cargando catálogo de medicamentos...
                        </div>
                    ) : filteredMedications
                        .length === 0 ? (
                        <div className="prescription-empty">
                            No se encontraron medicamentos.
                        </div>
                    ) : (
                        <section className="prescription-catalog">
                            {filteredMedications.map(
                                medication => {
                                    const selected =
                                        selectedMedicationIds
                                            .includes(
                                                Number(
                                                    medication.id
                                                )
                                            );

                                    return (
                                        <label
                                            key={
                                                medication.id
                                            }
                                            className={
                                                selected
                                                    ? "prescription-catalog-item selected"
                                                    : "prescription-catalog-item"
                                            }
                                        >
                                            <input
                                                type="checkbox"
                                                checked={
                                                    selected
                                                }
                                                onChange={() =>
                                                    toggleMedication(
                                                        medication.id
                                                    )
                                                }
                                                disabled={
                                                    processing
                                                }
                                            />

                                            <div>
                                                <div className="prescription-catalog-heading">
                                                    <strong>
                                                        {
                                                            medication
                                                                .name
                                                        }
                                                    </strong>

                                                    {medication
                                                        .controlled && (
                                                            <span>
                                                                Controlado
                                                            </span>
                                                        )}
                                                </div>

                                                <p>
                                                    {
                                                        medication
                                                            .code
                                                    }
                                                    {" · "}
                                                    {
                                                        medication
                                                            .activeIngredient
                                                    }
                                                </p>

                                                <small>
                                                    {
                                                        medication
                                                            .concentration
                                                    }
                                                    {" · "}
                                                    {
                                                        medication
                                                            .presentation
                                                    }
                                                </small>

                                                <b>
                                                    {formatAmount(
                                                        medication
                                                            .unitPrice
                                                    )}
                                                </b>
                                            </div>
                                        </label>
                                    );
                                }
                            )}
                        </section>
                    )}

                    {selectedMedications.length > 0 && (
                        <section className="prescription-selected-section">
                            <h3>
                                Indicaciones de medicamentos
                            </h3>

                            {selectedMedications.map(
                                medication => {
                                    const form =
                                        itemForms[
                                        medication.id
                                        ] || {};

                                    return (
                                        <article
                                            key={
                                                medication.id
                                            }
                                            className="prescription-selected-item"
                                        >
                                            <header>
                                                <div>
                                                    <strong>
                                                        {
                                                            medication
                                                                .name
                                                        }
                                                    </strong>

                                                    <span>
                                                        {
                                                            medication
                                                                .concentration
                                                        }
                                                        {" · "}
                                                        {
                                                            medication
                                                                .presentation
                                                        }
                                                    </span>
                                                </div>

                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        toggleMedication(
                                                            medication.id
                                                        )
                                                    }
                                                    disabled={
                                                        processing
                                                    }
                                                >
                                                    Quitar
                                                </button>
                                            </header>

                                            <div className="prescription-item-grid">
                                                <PrescriptionField
                                                    label="Dosis"
                                                    placeholder="Ejemplo: 500 mg"
                                                    value={
                                                        form.dosage ||
                                                        ""
                                                    }
                                                    maxLength={100}
                                                    disabled={
                                                        processing
                                                    }
                                                    onChange={value =>
                                                        updateMedicationField(
                                                            medication.id,
                                                            "dosage",
                                                            value
                                                        )
                                                    }
                                                />

                                                <PrescriptionField
                                                    label="Frecuencia"
                                                    placeholder="Ejemplo: Cada 8 horas"
                                                    value={
                                                        form.frequency ||
                                                        ""
                                                    }
                                                    maxLength={150}
                                                    disabled={
                                                        processing
                                                    }
                                                    onChange={value =>
                                                        updateMedicationField(
                                                            medication.id,
                                                            "frequency",
                                                            value
                                                        )
                                                    }
                                                />

                                                <PrescriptionField
                                                    label="Duración"
                                                    placeholder="Ejemplo: 5 días"
                                                    value={
                                                        form.duration ||
                                                        ""
                                                    }
                                                    maxLength={100}
                                                    disabled={
                                                        processing
                                                    }
                                                    onChange={value =>
                                                        updateMedicationField(
                                                            medication.id,
                                                            "duration",
                                                            value
                                                        )
                                                    }
                                                />

                                                <div className="prescription-form-group">
                                                    <label>
                                                        Cantidad total
                                                    </label>

                                                    <input
                                                        type="number"
                                                        min="1"
                                                        max="10000"
                                                        step="1"
                                                        value={
                                                            form
                                                                .prescribedQuantity ||
                                                            ""
                                                        }
                                                        onChange={event =>
                                                            updateMedicationField(
                                                                medication.id,
                                                                "prescribedQuantity",
                                                                event
                                                                    .target
                                                                    .value
                                                            )
                                                        }
                                                        disabled={
                                                            processing
                                                        }
                                                    />
                                                </div>
                                            </div>

                                            <div className="prescription-form-group">
                                                <label>
                                                    Indicaciones especiales
                                                </label>

                                                <textarea
                                                    rows={3}
                                                    maxLength={1000}
                                                    value={
                                                        form
                                                            .specialInstructions ||
                                                        ""
                                                    }
                                                    onChange={event =>
                                                        updateMedicationField(
                                                            medication.id,
                                                            "specialInstructions",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    disabled={
                                                        processing
                                                    }
                                                />

                                                <small>
                                                    {String(
                                                        form
                                                            .specialInstructions ||
                                                        ""
                                                    ).length}
                                                    /1000
                                                </small>
                                            </div>

                                            {errors[
                                                `medication-${medication.id}`
                                            ] && (
                                                    <div className="prescription-field-error">
                                                        {
                                                            errors[
                                                            `medication-${medication.id}`
                                                            ]
                                                        }
                                                    </div>
                                                )}
                                        </article>
                                    );
                                }
                            )}
                        </section>
                    )}

                    <section className="prescription-notes">
                        <label htmlFor="prescription-notes">
                            Observaciones generales
                        </label>

                        <textarea
                            id="prescription-notes"
                            rows={4}
                            maxLength={1000}
                            value={notes}
                            onChange={(event) => {
                                setNotes(
                                    event.target.value
                                );

                                setErrors(previous => ({
                                    ...previous,
                                    notes: ""
                                }));
                            }}
                            disabled={processing}
                        />

                        <small>
                            {notes.length}/1000
                        </small>

                        {errors.notes && (
                            <div className="prescription-field-error">
                                {errors.notes}
                            </div>
                        )}
                    </section>

                    <section className="prescription-summary">
                        <span>
                            Medicamentos seleccionados
                        </span>

                        <strong>
                            {selectedMedicationIds.length}
                        </strong>
                    </section>

                    <div className="prescription-actions">
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
                                selectedMedicationIds
                                    .length === 0
                            }
                        >
                            {processing
                                ? "Generando receta..."
                                : "Generar receta médica"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/**
 * Campo reutilizable para dosis, frecuencia
 * y duración.
 */
function PrescriptionField({
    label,
    placeholder,
    value,
    maxLength,
    disabled,
    onChange
}) {
    return (
        <div className="prescription-form-group">
            <label>
                {label}
            </label>

            <input
                type="text"
                placeholder={placeholder}
                value={value}
                maxLength={maxLength}
                onChange={event =>
                    onChange(
                        event.target.value
                    )
                }
                disabled={disabled}
            />
        </div>
    );
}

function formatStatus(value) {
    if (!value) {
        return "No disponible";
    }

    return String(value)
        .replaceAll("_", " ");
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

export default PrescriptionModal;