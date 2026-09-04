import { useMemo, useState } from "react";

import {
    registerVitalSigns
} from "../../api/nursingApi";

import "./VitalSignsModal.css";

function VitalSignsModal({
    patient,
    onClose,
    onRegistered
}) {
    const [formData, setFormData] = useState({
        systolicPressure: "",
        diastolicPressure: "",
        temperature: "",
        weight: "",
        height: "",
        heartRate: "",
        emergency: Boolean(patient?.emergency)
    });

    const [errors, setErrors] = useState({});

    const [generalError, setGeneralError] =
        useState("");

    const [processing, setProcessing] =
        useState(false);

    /*
     * Actualiza dinámicamente el campo indicado.
     *
     * Ejemplo:
     * updateField("temperature", "36.7")
     *
     * Actualiza formData.temperature.
     */

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


    // Continúa aquí el resto de tu lógica

    /*
     * Genera alertas clínicas en tiempo real.
     * Estas alertas no sustituyen la evaluación médica.
     */
    const clinicalAlerts = useMemo(() => {
        const alerts = [];

        const hasSystolic =
            formData.systolicPressure !== "";

        const hasDiastolic =
            formData.diastolicPressure !== "";

        const hasTemperature =
            formData.temperature !== "";

        const hasHeartRate =
            formData.heartRate !== "";

        const systolic =
            Number(formData.systolicPressure);

        const diastolic =
            Number(formData.diastolicPressure);

        const temperature =
            Number(formData.temperature);

        const heartRate = Number(formData.heartRate);

        if (
            hasSystolic &&
            hasDiastolic &&
            (
                systolic < 90 ||
                diastolic < 60
            )
        ) {
            alerts.push({
                field: "Presión arterial",
                value: `${systolic}/${diastolic} mmHg`,
                message:
                    "Presión arterial por debajo del rango clínico configurado."
            });
        }

        if (
            hasSystolic &&
            hasDiastolic &&
            (
                systolic > 120 ||
                diastolic > 80
            )
        ) {
            alerts.push({
                field: "Presión arterial",
                value: `${systolic}/${diastolic} mmHg`,
                message:
                    "Presión arterial por encima del rango clínico configurado."
            });
        }

        if (
            hasTemperature &&
            temperature < 36.5
        ) {
            alerts.push({
                field: "Temperatura",
                value: `${temperature} °C`,
                message:
                    "Temperatura por debajo del rango clínico configurado."
            });
        }

        if (
            hasTemperature &&
            temperature > 37.3
        ) {
            alerts.push({
                field: "Temperatura",
                value: `${temperature} °C`,
                message:
                    "Temperatura por encima del rango clínico configurado."
            });
        }

        if (
            hasHeartRate &&
            heartRate < 60
        ) {
            alerts.push({
                field: "Frecuencia cardíaca",
                value: `${heartRate} lpm`,
                message:
                    "Frecuencia cardíaca por debajo del rango clínico configurado."
            });
        }

        if (
            hasHeartRate &&
            heartRate > 100
        ) {
            alerts.push({
                field: "Frecuencia cardíaca",
                value: `${heartRate} lpm`,
                message:
                    "Frecuencia cardíaca por encima del rango clínico configurado."
            });
        }

        return alerts;
    }, [formData]);

    /*
     * Valida un valor numérico según su rango
     * permitido de captura.
     */
    const validateNumber = (
        value,
        minimum,
        maximum,
        label,
        unit
    ) => {
        if (
            value === "" ||
            value === null ||
            value === undefined
        ) {
            return `${label} es obligatorio.`;
        }

        const numericValue = Number(value);

        if (!Number.isFinite(numericValue)) {
            return `${label} debe ser un valor numérico.`;
        }

        if (
            numericValue < minimum ||
            numericValue > maximum
        ) {
            return `${label} debe encontrarse entre ${minimum} y ${maximum} ${unit}.`;
        }

        return "";
    };

    /*
     * Valida todos los campos antes de enviar
     * la información al backend.
     */
    const validateForm = () => {
        const newErrors = {};

        const systolicError = validateNumber(
            formData.systolicPressure,
            60,
            250,
            "La presión sistólica",
            "mmHg"
        );

        const diastolicError = validateNumber(
            formData.diastolicPressure,
            40,
            150,
            "La presión diastólica",
            "mmHg"
        );

        const temperatureError = validateNumber(
            formData.temperature,
            34,
            42,
            "La temperatura",
            "°C"
        );

        const weightError = validateNumber(
            formData.weight,
            0.5,
            300,
            "El peso",
            "kg"
        );

        const heightError = validateNumber(
            formData.height,
            30,
            250,
            "La talla",
            "cm"
        );

        const heartRateError = validateNumber(
            formData.heartRate,
            30,
            220,
            "La frecuencia cardíaca",
            "lpm"
        );

        if (systolicError) {
            newErrors.systolicPressure =
                systolicError;
        }

        if (diastolicError) {
            newErrors.diastolicPressure =
                diastolicError;
        }

        if (temperatureError) {
            newErrors.temperature =
                temperatureError;
        }

        if (weightError) {
            newErrors.weight =
                weightError;
        }

        if (heightError) {
            newErrors.height =
                heightError;
        }

        if (heartRateError) {
            newErrors.heartRate =
                heartRateError;
        }

        /*
         * Esta validación se ejecuta únicamente
         * cuando ambas presiones son válidas.
         */
        if (
            !systolicError &&
            !diastolicError &&
            Number(formData.systolicPressure) <=
            Number(formData.diastolicPressure)
        ) {
            newErrors.systolicPressure =
                "La presión sistólica debe ser mayor que la presión diastólica.";
        }

        setErrors(newErrors);

        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (processing) {
            return;
        }

        setGeneralError("");

        if (!validateForm()) {
            return;
        }

        const isAppointment =
            patient.sourceType === "APPOINTMENT";

        const isEmergencyReception =
            patient.sourceType ===
            "EMERGENCY_RECEPTION";

        if (
            !isAppointment &&
            !isEmergencyReception
        ) {
            setGeneralError(
                "El origen del paciente no es válido."
            );

            return;
        }

        const request = {
            sourceType: patient.sourceType,

            appointmentId:
                isAppointment
                    ? patient.appointmentId
                    : null,

            emergencyReceptionId:
                isEmergencyReception
                    ? patient.emergencyReceptionId
                    : null,

            systolicPressure:
                Number(
                    formData.systolicPressure
                ),

            diastolicPressure:
                Number(
                    formData.diastolicPressure
                ),

            temperature:
                Number(formData.temperature),

            weight:
                Number(formData.weight),

            height:
                Number(formData.height),

            heartRate:
                Number(formData.heartRate),

            emergency:
                Boolean(formData.emergency)
        };

        try {
            setProcessing(true);

            const response =
                await registerVitalSigns(
                    request
                );

            onRegistered(response.data);

        } catch (error) {
            console.error(
                "Error registrando signos vitales:",
                error
            );

            const backendData =
                error.response?.data;

            const backendMessage =
                typeof backendData === "string"
                    ? backendData
                    : backendData?.message;

            setGeneralError(
                backendMessage ||
                "No fue posible registrar los signos vitales."
            );

        } finally {
            setProcessing(false);
        }
    };

    const maskDpi = (dpi) => {
        if (!dpi) {
            return "No registrado";
        }

        const value =
            String(dpi).trim();

        if (value.length <= 4) {
            return value;
        }

        return `${"*".repeat(
            value.length - 4
        )}${value.slice(-4)}`;
    };

    return (
        <div
            className="vital-modal-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="vital-modal-title"
        >
            <div className="vital-modal">
                <header className="vital-modal-header">
                    <div>
                        <h2 id="vital-modal-title">
                            Registrar Signos Vitales
                        </h2>

                        <p>
                            {patient.sourceType ===
                                "APPOINTMENT"
                                ? `Cita #${patient.appointmentId}`
                                : `Emergencia #${patient.emergencyReceptionId}`}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="vital-close-button"
                        onClick={onClose}
                        disabled={processing}
                        aria-label="Cerrar"
                    >
                        ×
                    </button>
                </header>

                <section className="vital-patient-context">
                    <div>
                        <span>Paciente</span>

                        <strong>
                            {patient.patientName}
                        </strong>
                    </div>

                    <div>
                        <span>DPI</span>

                        <strong>
                            {maskDpi(
                                patient.patientDpi
                            )}
                        </strong>
                    </div>

                    <div>
                        <span>Sucursal</span>

                        <strong>
                            {patient.branch}
                        </strong>
                    </div>

                    <div>
                        <span>Prioridad</span>

                        <strong
                            className={
                                patient.emergency
                                    ? "emergency-context"
                                    : ""
                            }
                        >
                            {patient.priority}
                        </strong>
                    </div>
                </section>

                {generalError && (
                    <div className="vital-general-error">
                        {generalError}
                    </div>
                )}

                {clinicalAlerts.length > 0 && (
                    <section className="vital-alerts">
                        <h3>
                            Alertas clínicas
                        </h3>

                        <p>
                            Los valores están dentro del
                            rango de captura, pero fuera del
                            rango clínico configurado.
                        </p>

                        <ul>
                            {clinicalAlerts.map(
                                (alert, index) => (
                                    <li
                                        key={
                                            `${alert.field}-${index}`
                                        }
                                    >
                                        <strong>
                                            {alert.field}:
                                        </strong>{" "}
                                        {alert.value}.{" "}
                                        {alert.message}
                                    </li>
                                )
                            )}
                        </ul>
                    </section>
                )}

                <form
                    className="vital-form"
                    onSubmit={handleSubmit}
                >
                    <fieldset className="vital-fieldset">
                        <legend>
                            Presión arterial
                        </legend>

                        <div className="vital-form-grid">
                            <VitalField
                                id="systolic-pressure"
                                label="Presión sistólica"
                                unit="mmHg"
                                value={
                                    formData
                                        .systolicPressure
                                }
                                error={
                                    errors
                                        .systolicPressure
                                }
                                min="60"
                                max="250"
                                step="1"
                                onChange={(value) =>
                                    updateField(
                                        "systolicPressure",
                                        value
                                    )
                                }
                            />

                            <VitalField
                                id="diastolic-pressure"
                                label="Presión diastólica"
                                unit="mmHg"
                                value={
                                    formData
                                        .diastolicPressure
                                }
                                error={
                                    errors
                                        .diastolicPressure
                                }
                                min="40"
                                max="150"
                                step="1"
                                onChange={(value) =>
                                    updateField(
                                        "diastolicPressure",
                                        value
                                    )
                                }
                            />
                        </div>
                    </fieldset>

                    <div className="vital-form-grid">
                        <VitalField
                            id="temperature"
                            label="Temperatura"
                            unit="°C"
                            value={
                                formData.temperature
                            }
                            error={
                                errors.temperature
                            }
                            min="34"
                            max="42"
                            step="0.1"
                            onChange={(value) =>
                                updateField(
                                    "temperature",
                                    value
                                )
                            }
                        />

                        <VitalField
                            id="weight"
                            label="Peso"
                            unit="kg"
                            value={
                                formData.weight
                            }
                            error={
                                errors.weight
                            }
                            min="0.5"
                            max="300"
                            step="0.1"
                            onChange={(value) =>
                                updateField(
                                    "weight",
                                    value
                                )
                            }
                        />

                        <VitalField
                            id="height"
                            label="Talla"
                            unit="cm"
                            value={
                                formData.height
                            }
                            error={
                                errors.height
                            }
                            min="30"
                            max="250"
                            step="0.1"
                            onChange={(value) =>
                                updateField(
                                    "height",
                                    value
                                )
                            }
                        />

                        <VitalField
                            id="heart-rate"
                            label="Frecuencia cardíaca"
                            unit="lpm"
                            value={
                                formData.heartRate
                            }
                            error={
                                errors.heartRate
                            }
                            min="30"
                            max="220"
                            step="1"
                            onChange={(value) =>
                                updateField(
                                    "heartRate",
                                    value
                                )
                            }
                        />
                    </div>

                    <label className="emergency-checkbox">
                        <input
                            type="checkbox"
                            checked={
                                formData.emergency
                            }
                            onChange={(event) =>
                                updateField(
                                    "emergency",
                                    event.target.checked
                                )
                            }
                        />

                        <span>
                            Es Emergencia
                        </span>
                    </label>

                    <div className="vital-modal-actions">
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
                                ? "Registrando..."
                                : "Registrar Signos Vitales"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
function VitalField({
    id,
    label,
    unit,
    value,
    error,
    min,
    max,
    step,
    onChange
}) {
    return (
        <div className="vital-form-group">
            <label htmlFor={id}>
                {label}
            </label>

            <div className="vital-input-wrapper">
                <input
                    id={id}
                    type="number"
                    inputMode="decimal"
                    min={min}
                    max={max}
                    step={step}
                    value={value}
                    className={
                        error
                            ? "input-error"
                            : ""
                    }
                    onChange={(event) => {
                        onChange(
                            event.target.value
                        );
                    }}
                />

                <span>
                    {unit}
                </span>
            </div>

            <small>
                Rango de captura:{" "}
                {min} a {max} {unit}
            </small>

            {error && (
                <div className="error-message">
                    {error}
                </div>
            )}
        </div>
    );
}

export default VitalSignsModal;