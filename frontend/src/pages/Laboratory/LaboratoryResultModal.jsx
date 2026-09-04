import { useState } from "react";

import {
    saveLaboratoryResult
} from "../../api/laboratoryApi";

import "./LaboratoryResultModal.css";

function LaboratoryResultModal({
    orderId,
    item,
    onClose,
    onSaved
}) {
    const [formData, setFormData] =
        useState({
            resultValue:
                item.resultValue || "",

            unit:
                item.resultUnit || "",

            resultDate:
                toDateTimeLocal(
                    item.resultDate
                ),

            outOfRange:
                Boolean(item.outOfRange),

            notes:
                item.resultNotes || ""
        });

    const [errors, setErrors] =
        useState({});

    const [
        generalError,
        setGeneralError
    ] = useState("");

    const [processing, setProcessing] =
        useState(false);

    const updateField = (
        field,
        value
    ) => {
        setFormData(previous => ({
            ...previous,
            [field]: value
        }));

        setErrors(previous => ({
            ...previous,
            [field]: ""
        }));

        setGeneralError("");
    };

    const validateForm = () => {
        const newErrors = {};

        const resultValue =
            formData.resultValue.trim();

        const unit =
            formData.unit.trim();

        const notes =
            formData.notes.trim();

        if (!resultValue) {
            newErrors.resultValue =
                "El valor del resultado es obligatorio.";

        } else if (
            resultValue.length > 500
        ) {
            newErrors.resultValue =
                "El valor del resultado no puede exceder los 500 caracteres.";
        }

        if (unit.length > 50) {
            newErrors.unit =
                "La unidad no puede exceder los 50 caracteres.";
        }

        if (notes.length > 1000) {
            newErrors.notes =
                "Las notas no pueden exceder los 1000 caracteres.";
        }

        if (formData.resultDate) {
            const selectedDate =
                new Date(
                    formData.resultDate
                );

            if (
                Number.isNaN(
                    selectedDate.getTime()
                )
            ) {
                newErrors.resultDate =
                    "La fecha del resultado no es válida.";

            } else if (
                selectedDate.getTime() >
                Date.now() + 60000
            ) {
                newErrors.resultDate =
                    "La fecha del resultado no puede encontrarse en el futuro.";
            }
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
            resultValue:
                formData.resultValue.trim(),

            unit:
                formData.unit.trim() ||
                null,

            resultDate:
                formData.resultDate
                    ? new Date(
                        formData.resultDate
                    ).toISOString()
                    : null,

            outOfRange:
                Boolean(
                    formData.outOfRange
                ),

            notes:
                formData.notes.trim() ||
                null
        };

        try {
            setProcessing(true);

            const response =
                await saveLaboratoryResult(
                    orderId,
                    item.itemId,
                    request
                );

            onSaved(response.data);

        } catch (error) {
            console.error(
                "Error guardando resultado:",
                error
            );

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible guardar el resultado."
                )
            );

        } finally {
            setProcessing(false);
        }
    };

    return (
        <div
            className="lab-result-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="lab-result-title"
        >
            <div className="lab-result-modal">
                <header className="lab-result-header">
                    <div>
                        <h2 id="lab-result-title">
                            Registrar Resultado
                        </h2>

                        <p>
                            {item.testCode} -{" "}
                            {item.testName}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="lab-result-close"
                        onClick={onClose}
                        disabled={processing}
                        aria-label="Cerrar"
                    >
                        ×
                    </button>
                </header>

                <section className="lab-test-context">
                    <div>
                        <span>
                            Examen
                        </span>

                        <strong>
                            {item.testName}
                        </strong>
                    </div>

                    <div>
                        <span>
                            Rango de referencia
                        </span>

                        <strong>
                            {item.referenceRange ||
                                "No configurado"}
                        </strong>
                    </div>
                </section>

                {generalError && (
                    <div className="lab-result-error">
                        {generalError}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="lab-result-form-group">
                        <label htmlFor="result-value">
                            Valor del resultado *
                        </label>

                        <input
                            id="result-value"
                            type="text"
                            maxLength="500"
                            value={
                                formData.resultValue
                            }
                            className={
                                errors.resultValue
                                    ? "input-error"
                                    : ""
                            }
                            onChange={(event) =>
                                updateField(
                                    "resultValue",
                                    event.target.value
                                )
                            }
                        />

                        <small>
                            {
                                formData
                                    .resultValue
                                    .length
                            }{" "}
                            / 500
                        </small>

                        {errors.resultValue && (
                            <div className="error-message">
                                {errors.resultValue}
                            </div>
                        )}
                    </div>

                    <div className="lab-result-form-grid">
                        <div className="lab-result-form-group">
                            <label htmlFor="result-unit">
                                Unidad
                            </label>

                            <input
                                id="result-unit"
                                type="text"
                                maxLength="50"
                                placeholder="Ej. mg/dL"
                                value={
                                    formData.unit
                                }
                                className={
                                    errors.unit
                                        ? "input-error"
                                        : ""
                                }
                                onChange={(event) =>
                                    updateField(
                                        "unit",
                                        event.target.value
                                    )
                                }
                            />

                            {errors.unit && (
                                <div className="error-message">
                                    {errors.unit}
                                </div>
                            )}
                        </div>

                        <div className="lab-result-form-group">
                            <label htmlFor="result-date">
                                Fecha del resultado
                            </label>

                            <input
                                id="result-date"
                                type="datetime-local"
                                value={
                                    formData.resultDate
                                }
                                className={
                                    errors.resultDate
                                        ? "input-error"
                                        : ""
                                }
                                onChange={(event) =>
                                    updateField(
                                        "resultDate",
                                        event.target.value
                                    )
                                }
                            />

                            {errors.resultDate && (
                                <div className="error-message">
                                    {errors.resultDate}
                                </div>
                            )}
                        </div>
                    </div>

                    <label className="lab-out-range-checkbox">
                        <input
                            type="checkbox"
                            checked={
                                formData.outOfRange
                            }
                            onChange={(event) =>
                                updateField(
                                    "outOfRange",
                                    event.target.checked
                                )
                            }
                        />

                        <span>
                            Marcar resultado como
                            fuera de rango
                        </span>
                    </label>

                    {formData.outOfRange && (
                        <div className="lab-out-range-warning">
                            El resultado será mostrado
                            con una alerta de fuera de
                            rango.
                        </div>
                    )}

                    <div className="lab-result-form-group">
                        <label htmlFor="result-notes">
                            Observaciones
                        </label>

                        <textarea
                            id="result-notes"
                            rows="4"
                            maxLength="1000"
                            value={
                                formData.notes
                            }
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
                            {
                                formData.notes
                                    .length
                            }{" "}
                            / 1000
                        </small>

                        {errors.notes && (
                            <div className="error-message">
                                {errors.notes}
                            </div>
                        )}
                    </div>

                    <div className="lab-result-actions">
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
                                : "Guardar Resultado"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

function toDateTimeLocal(value) {
    if (!value) {
        return "";
    }

    const date =
        new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "";
    }

    const offset =
        date.getTimezoneOffset();

    const localDate =
        new Date(
            date.getTime() -
            offset * 60000
        );

    return localDate
        .toISOString()
        .slice(0, 16);
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

export default LaboratoryResultModal;