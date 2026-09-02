import {
    useEffect,
    useState
} from "react";

import {
    getMedicalConsultation,
    saveMedicalConsultation,
    searchIcd10Codes
} from "../../api/medicalConsultationApi";

import "./MedicalConsultationModal.css";

function MedicalConsultationModal({
    appointmentId,
    onClose,
    onSaved
}) {
    const [consultation, setConsultation] =
        useState(null);

    const [formData, setFormData] =
        useState({
            visitReason: "",
            clinicalFindings: "",
            icd10Code: "",
            icd10Description: "",
            diagnosis: "",
            treatmentPlan: "",
            additionalNotes: "",
            status: "EN_CURSO"
        });

    const [icd10Query, setIcd10Query] =
        useState("");

    const [icd10Results, setIcd10Results] =
        useState([]);

    const [showIcd10Results, setShowIcd10Results] =
        useState(false);

    const [loading, setLoading] =
        useState(true);

    const [searchingIcd10, setSearchingIcd10] =
        useState(false);

    const [processing, setProcessing] =
        useState(false);

    const [errors, setErrors] =
        useState({});

    const [generalError, setGeneralError] =
        useState("");

    useEffect(() => {
        loadConsultation();
    }, [appointmentId]);

    useEffect(() => {
        const cleanQuery =
            icd10Query.trim();

        if (cleanQuery.length < 2) {
            setIcd10Results([]);
            setShowIcd10Results(false);
            return;
        }

        const timeoutId = setTimeout(() => {
            loadIcd10Codes(cleanQuery);
        }, 350);

        return () => {
            clearTimeout(timeoutId);
        };
    }, [icd10Query]);

    const loadConsultation = async () => {
        try {
            setLoading(true);
            setGeneralError("");

            const response =
                await getMedicalConsultation(
                    appointmentId
                );

            const data =
                response.data;

            setConsultation(data);

            setFormData({
                visitReason:
                    data.visitReason || "",

                clinicalFindings:
                    data.clinicalFindings || "",

                icd10Code:
                    data.icd10Code || "",

                icd10Description:
                    data.icd10Description || "",

                diagnosis:
                    data.diagnosis || "",

                treatmentPlan:
                    data.treatmentPlan || "",

                additionalNotes:
                    data.additionalNotes || "",

                status:
                    data.consultationStatus ||
                    "EN_CURSO"
            });

            if (data.icd10Code) {
                setIcd10Query(
                    `${data.icd10Code} - ${data.icd10Description || ""}`
                );
            }

        } catch (error) {
            console.error(
                "Error cargando consulta:",
                error
            );

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible cargar la consulta médica."
                )
            );

        } finally {
            setLoading(false);
        }
    };

    const loadIcd10Codes = async (
        query
    ) => {
        try {
            setSearchingIcd10(true);

            const response =
                await searchIcd10Codes(
                    query
                );

            const results =
                Array.isArray(response.data)
                    ? response.data
                    : [];

            setIcd10Results(results);
            setShowIcd10Results(true);

        } catch (error) {
            console.error(
                "Error buscando CIE-10:",
                error
            );

            setIcd10Results([]);
            setShowIcd10Results(false);

        } finally {
            setSearchingIcd10(false);
        }
    };

    const updateField = (field, newValue) => {
        setFormData((previous) => ({
            ...previous,
            [field]: newValue
        }));

        setErrors((previous) => ({
            ...previous,
            [field]: ""
        }));

        setGeneralError("");
    };

    const selectIcd10Code = (
        item
    ) => {
        setFormData(previous => ({
            ...previous,
            icd10Code: item.code,
            icd10Description:
                item.description
        }));

        setIcd10Query(
            `${item.code} - ${item.description}`
        );

        setIcd10Results([]);
        setShowIcd10Results(false);

        setErrors(previous => ({
            ...previous,
            icd10Code: ""
        }));
    };

    const clearIcd10Selection = () => {
        setIcd10Query("");

        setFormData(previous => ({
            ...previous,
            icd10Code: "",
            icd10Description: ""
        }));

        setIcd10Results([]);
        setShowIcd10Results(false);
    };

    const validateForm = () => {
        const newErrors = {};

        const cleanVisitReason =
            formData.visitReason.trim();

        const cleanDiagnosis =
            formData.diagnosis.trim();

        if (!cleanVisitReason) {
            newErrors.visitReason =
                "El motivo de visita es obligatorio.";

        } else if (
            cleanVisitReason.length > 1000
        ) {
            newErrors.visitReason =
                "El motivo de visita no puede exceder los 1000 caracteres.";
        }

        if (
            formData.status === "FINALIZADA" &&
            !cleanDiagnosis
        ) {
            newErrors.diagnosis =
                "No es posible finalizar la consulta sin registrar un diagnóstico.";
        }

        const fieldsWithLimit = [
            {
                name: "clinicalFindings",
                label: "Los hallazgos clínicos"
            },
            {
                name: "diagnosis",
                label: "El diagnóstico"
            },
            {
                name: "treatmentPlan",
                label: "El plan de tratamiento"
            },
            {
                name: "additionalNotes",
                label: "Las notas adicionales"
            }
        ];

        fieldsWithLimit.forEach(
            field => {
                if (
                    formData[field.name]
                        .trim()
                        .length > 4000
                ) {
                    newErrors[field.name] =
                        `${field.label} no pueden exceder los 4000 caracteres.`;
                }
            }
        );

        if (
            icd10Query.trim() &&
            !formData.icd10Code
        ) {
            newErrors.icd10Code =
                "Debe seleccionar un código CIE-10 de la lista.";
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
            visitReason:
                formData.visitReason.trim(),

            clinicalFindings:
                formData.clinicalFindings
                    .trim() || null,

            icd10Code:
                formData.icd10Code || null,

            icd10Description:
                formData.icd10Description ||
                null,

            diagnosis:
                formData.diagnosis
                    .trim() || null,

            treatmentPlan:
                formData.treatmentPlan
                    .trim() || null,

            additionalNotes:
                formData.additionalNotes
                    .trim() || null,

            status:
                formData.status
        };

        try {
            setProcessing(true);

            const response =
                await saveMedicalConsultation(
                    appointmentId,
                    request
                );

            onSaved(response.data);

        } catch (error) {
            console.error(
                "Error guardando consulta:",
                error
            );

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible guardar la consulta médica."
                )
            );

        } finally {
            setProcessing(false);
        }
    };

    if (loading) {
        return (
            <div className="medical-modal-overlay">
                <div className="medical-modal">
                    <p>
                        Cargando consulta médica...
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div
            className="medical-modal-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="medical-modal-title"
        >
            <div className="medical-modal">
                <header className="medical-modal-header">
                    <div>
                        <h2 id="medical-modal-title">
                            Consulta Médica
                        </h2>

                        <p>
                            Cita #{appointmentId}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="medical-close-button"
                        onClick={onClose}
                        disabled={processing}
                        aria-label="Cerrar"
                    >
                        ×
                    </button>
                </header>

                {generalError && (
                    <div className="medical-error">
                        {generalError}
                    </div>
                )}

                {consultation && (
                    <>
                        <section className="medical-context">
                            <div>
                                <span>Paciente</span>

                                <strong>
                                    {
                                        consultation
                                            .patientName
                                    }
                                </strong>
                            </div>

                            <div>
                                <span>Médico</span>

                                <strong>
                                    {
                                        consultation
                                            .doctorName
                                    }
                                </strong>
                            </div>

                            <div>
                                <span>Especialidad</span>

                                <strong>
                                    {
                                        consultation
                                            .specialty
                                    }
                                </strong>
                            </div>

                            <div>
                                <span>Prioridad</span>

                                <strong
                                    className={
                                        consultation.priority
                                            === "EMERGENCIA"
                                            ? "medical-emergency"
                                            : ""
                                    }
                                >
                                    {
                                        consultation
                                            .priority
                                    }
                                </strong>
                            </div>
                        </section>

                        <VitalSignsSummary
                            vitalSigns={
                                consultation.vitalSigns
                            }
                        />
                    </>
                )}

                <form
                    className="medical-form"
                    onSubmit={handleSubmit}
                >
                    <ConsultationTextArea
                        id="visit-reason"
                        label="Motivo de visita"
                        required
                        maxLength={1000}
                        value={
                            formData.visitReason
                        }
                        error={
                            errors.visitReason
                        }
                        onChange={(value) =>
                            updateField(
                                "visitReason",
                                value
                            )
                        }
                    />

                    <ConsultationTextArea
                        id="clinical-findings"
                        label="Hallazgos clínicos"
                        maxLength={4000}
                        value={
                            formData
                                .clinicalFindings
                        }
                        error={
                            errors
                                .clinicalFindings
                        }
                        onChange={(value) =>
                            updateField(
                                "clinicalFindings",
                                value
                            )
                        }
                    />

                    <div className="medical-form-group">
                        <label htmlFor="icd10-search">
                            Código CIE-10
                        </label>

                        <div className="icd10-input-row">
                            <input
                                id="icd10-search"
                                type="text"
                                autoComplete="off"
                                placeholder={
                                    "Busque por código o descripción"
                                }
                                value={icd10Query}
                                className={
                                    errors.icd10Code
                                        ? "input-error"
                                        : ""
                                }
                                onChange={(event) => {
                                    setIcd10Query(
                                        event.target.value
                                    );

                                    setFormData(
                                        previous => ({
                                            ...previous,
                                            icd10Code: "",
                                            icd10Description:
                                                ""
                                        })
                                    );

                                    setErrors(
                                        previous => ({
                                            ...previous,
                                            icd10Code: ""
                                        })
                                    );
                                }}
                                onFocus={() => {
                                    if (
                                        icd10Results.length >
                                        0
                                    ) {
                                        setShowIcd10Results(
                                            true
                                        );
                                    }
                                }}
                            />

                            {icd10Query && (
                                <button
                                    type="button"
                                    className="icd10-clear-button"
                                    onClick={
                                        clearIcd10Selection
                                    }
                                >
                                    Limpiar
                                </button>
                            )}
                        </div>

                        {searchingIcd10 && (
                            <small>
                                Buscando códigos...
                            </small>
                        )}

                        {showIcd10Results && (
                            <div className="icd10-results">
                                {icd10Results.length > 0 ? (
                                    icd10Results.map(
                                        item => (
                                            <button
                                                key={item.id}
                                                type="button"
                                                className={
                                                    "icd10-result"
                                                }
                                                onClick={() =>
                                                    selectIcd10Code(
                                                        item
                                                    )
                                                }
                                            >
                                                <strong>
                                                    {item.code}
                                                </strong>

                                                <span>
                                                    {
                                                        item
                                                            .description
                                                    }
                                                </span>
                                            </button>
                                        )
                                    )
                                ) : (
                                    <div className="icd10-empty">
                                        No se encontraron
                                        coincidencias.
                                    </div>
                                )}
                            </div>
                        )}

                        {errors.icd10Code && (
                            <div className="error-message">
                                {errors.icd10Code}
                            </div>
                        )}

                        {formData.icd10Code && (
                            <div className="icd10-selected">
                                Seleccionado:{" "}
                                <strong>
                                    {
                                        formData
                                            .icd10Code
                                    }
                                </strong>
                                {" - "}
                                {
                                    formData
                                        .icd10Description
                                }
                            </div>
                        )}
                    </div>

                    <ConsultationTextArea
                        id="diagnosis"
                        label="Diagnóstico"
                        required={
                            formData.status ===
                            "FINALIZADA"
                        }
                        maxLength={4000}
                        value={
                            formData.diagnosis
                        }
                        error={
                            errors.diagnosis
                        }
                        onChange={(value) =>
                            updateField(
                                "diagnosis",
                                value
                            )
                        }
                    />

                    <ConsultationTextArea
                        id="treatment-plan"
                        label="Plan de tratamiento"
                        maxLength={4000}
                        value={
                            formData.treatmentPlan
                        }
                        error={
                            errors.treatmentPlan
                        }
                        onChange={(value) =>
                            updateField(
                                "treatmentPlan",
                                value
                            )
                        }
                    />

                    <ConsultationTextArea
                        id="additional-notes"
                        label="Notas adicionales"
                        maxLength={4000}
                        value={
                            formData.additionalNotes
                        }
                        error={
                            errors.additionalNotes
                        }
                        onChange={(value) =>
                            updateField(
                                "additionalNotes",
                                value
                            )
                        }
                    />

                    <div className="medical-form-group">
                        <label htmlFor="consultation-status">
                            Estado de consulta
                        </label>

                        <select
                            id="consultation-status"
                            value={formData.status}
                            onChange={(event) =>
                                updateField(
                                    "status",
                                    event.target.value
                                )
                            }
                        >
                            <option value="EN_CURSO">
                                En curso
                            </option>

                            <option value="FINALIZADA">
                                Finalizada
                            </option>
                        </select>

                        {formData.status ===
                            "FINALIZADA" && (
                                <small>
                                    El diagnóstico será
                                    obligatorio para finalizar.
                                </small>
                            )}
                    </div>

                    <div className="medical-modal-actions">
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
                            disabled={processing}
                        >
                            {processing
                                ? "Guardando..."
                                : formData.status ===
                                    "FINALIZADA"
                                    ? "Finalizar Consulta"
                                    : "Guardar en Curso"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

function VitalSignsSummary({
    vitalSigns
}) {
    if (!vitalSigns) {
        return (
            <section className="medical-vitals missing">
                No hay signos vitales disponibles.
            </section>
        );
    }

    return (
        <section className="medical-vitals">
            <h3>Signos vitales</h3>

            <div className="medical-vitals-grid">
                <p>
                    <span>Presión arterial</span>
                    <strong>
                        {vitalSigns.systolicPressure}/
                        {vitalSigns.diastolicPressure}
                        {" "}mmHg
                    </strong>
                </p>

                <p>
                    <span>Temperatura</span>
                    <strong>
                        {vitalSigns.temperature} °C
                    </strong>
                </p>

                <p>
                    <span>Peso</span>
                    <strong>
                        {vitalSigns.weight} kg
                    </strong>
                </p>

                <p>
                    <span>Talla</span>
                    <strong>
                        {vitalSigns.height} cm
                    </strong>
                </p>

                <p>
                    <span>Frecuencia cardíaca</span>
                    <strong>
                        {vitalSigns.heartRate} lpm
                    </strong>
                </p>

                <p>
                    <span>Personal de enfermería</span>
                    <strong>
                        {vitalSigns.nurseName}
                    </strong>
                </p>
            </div>

            {vitalSigns.clinicalAlerts?.length >
                0 && (
                    <div className="medical-clinical-alerts">
                        <strong>
                            Alertas clínicas registradas
                        </strong>

                        <ul>
                            {vitalSigns
                                .clinicalAlerts
                                .map(
                                    (alert, index) => (
                                        <li key={index}>
                                            {alert}
                                        </li>
                                    )
                                )}
                        </ul>
                    </div>
                )}
        </section>
    );
}

function ConsultationTextArea({
    id,
    label,
    required = false,
    maxLength,
    value,
    error,
    onChange
}) {
    return (
        <div className="medical-form-group">
            <label htmlFor={id}>
                {label}
                {required ? " *" : ""}
            </label>

            <textarea
                id={id}
                rows="4"
                maxLength={maxLength}
                value={value}
                className={
                    error
                        ? "input-error"
                        : ""
                }
                onChange={(event) =>
                    onChange(event.target.value)
                }
            />

            <small className="medical-counter">
                {value.length} / {maxLength}
            </small>

            {error && (
                <div className="error-message">
                    {error}
                </div>
            )}
        </div>
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

    return data?.message || fallback;
}

export default MedicalConsultationModal;