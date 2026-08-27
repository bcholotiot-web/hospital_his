import { useEffect, useState } from "react";

import {
    getReceptionBranches,
    registerEmergencyReception
} from "../../api/receptionApi";

import "./EmergencyReceptionModal.css";

import "./EmergencyReceptionModal.css";

function EmergencyReceptionModal({
    onClose,
    onRegistered
}) {
    const [patientName, setPatientName] =
        useState("");

    const [patientDpi, setPatientDpi] =
        useState("");

    const [branchId, setBranchId] =
        useState("");

    const [emergencyNote, setEmergencyNote] =
        useState("");

    const [branches, setBranches] =
        useState([]);

    const [errors, setErrors] =
        useState({});

    const [generalError, setGeneralError] =
        useState("");

    const [loadingBranches, setLoadingBranches] =
        useState(true);

    const [processing, setProcessing] =
        useState(false);

    useEffect(() => {
        loadBranches();
    }, []);

    const loadBranches = async () => {
        try {
            setLoadingBranches(true);
            setGeneralError("");

            const response =
                await getReceptionBranches();

            const activeBranches =
                Array.isArray(response.data)
                    ? response.data.filter(
                        branch => branch.active
                    )
                    : [];

            setBranches(activeBranches);

            if (activeBranches.length === 0) {
                setGeneralError(
                    "No hay sucursales activas disponibles."
                );
            }

        } catch (error) {
            console.error(
                "Error al cargar sucursales:",
                error
            );

            setBranches([]);

            setGeneralError(
                error.response?.data?.message ||
                "No fue posible cargar las sucursales activas."
            );

        } finally {
            setLoadingBranches(false);
        }
    };

    const validateForm = () => {
        const newErrors = {};

        const cleanName =
            patientName.trim();

        const cleanDpi =
            patientDpi.trim();

        const cleanNote =
            emergencyNote.trim();

        if (!cleanName) {
            newErrors.patientName =
                "El nombre del paciente es obligatorio.";

        } else if (
            cleanName.length < 5 ||
            cleanName.length > 100
        ) {
            newErrors.patientName =
                `El nombre del paciente debe contener entre 5 y 100 caracteres. Usted ingresó ${cleanName.length} caracteres.`;
        }

        if (!cleanDpi) {
            newErrors.patientDpi =
                "El DPI del paciente es obligatorio.";

        } else if (!/^\d{13}$/.test(cleanDpi)) {
            newErrors.patientDpi =
                "El DPI debe contener exactamente 13 dígitos numéricos.";
        }

        if (!branchId) {
            newErrors.branchId =
                "Debe seleccionar una sucursal.";
        }

        if (cleanNote.length > 500) {
            newErrors.emergencyNote =
                "La nota de emergencia no puede exceder los 500 caracteres.";
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

        try {
            setProcessing(true);

            const response =
                await registerEmergencyReception({
                    patientName:
                        patientName.trim(),

                    patientDpi:
                        patientDpi.trim(),

                    branchId:
                        Number(branchId),

                    emergencyNote:
                        emergencyNote.trim() || null
                });

            onRegistered(response.data);

        } catch (error) {
            console.error(error);

            setGeneralError(
                error.response?.data?.message ||
                "No fue posible registrar la emergencia."
            );

        } finally {
            setProcessing(false);
        }
    };

    return (
        <div
            className="emergency-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="emergency-title"
        >
            <div className="emergency-modal">
                <div className="emergency-modal-header">
                    <div>
                        <h2 id="emergency-title">
                            Registrar Emergencia
                        </h2>

                        <p>
                            Registre los datos básicos del
                            paciente para atención inmediata.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="emergency-close-button"
                        onClick={onClose}
                        disabled={processing}
                        aria-label="Cerrar"
                    >
                        ×
                    </button>
                </div>

                <div className="emergency-warning">
                    <strong>
                        Prioridad: EMERGENCIA
                    </strong>

                    <p>
                        El paciente debe pasar directamente
                        a toma de signos vitales después del registro.
                    </p>
                </div>

                {generalError && (
                    <div className="emergency-general-error">
                        {generalError}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="emergency-form-group">
                        <label htmlFor="emergency-patient-name">
                            Nombre del paciente
                        </label>

                        <input
                            id="emergency-patient-name"
                            type="text"
                            maxLength="100"
                            placeholder="Nombre completo"
                            value={patientName}
                            className={
                                errors.patientName
                                    ? "input-error"
                                    : ""
                            }
                            onChange={(event) => {
                                setPatientName(
                                    event.target.value
                                );

                                setErrors(previous => ({
                                    ...previous,
                                    patientName: ""
                                }));

                                setGeneralError("");
                            }}
                        />

                        <div className="field-counter">
                            {patientName.trim().length} / 100
                        </div>

                        {errors.patientName && (
                            <div className="error-message">
                                {errors.patientName}
                            </div>
                        )}
                    </div>

                    <div className="emergency-form-group">
                        <label htmlFor="emergency-patient-dpi">
                            DPI del paciente
                        </label>

                        <input
                            id="emergency-patient-dpi"
                            type="text"
                            inputMode="numeric"
                            maxLength="13"
                            placeholder="13 dígitos"
                            value={patientDpi}
                            className={
                                errors.patientDpi
                                    ? "input-error"
                                    : ""
                            }
                            onChange={(event) => {
                                const numericValue =
                                    event.target.value.replace(
                                        /\D/g,
                                        ""
                                    );

                                setPatientDpi(numericValue);

                                setErrors(previous => ({
                                    ...previous,
                                    patientDpi: ""
                                }));

                                setGeneralError("");
                            }}
                        />

                        <div className="field-counter">
                            {patientDpi.length} / 13
                        </div>

                        {errors.patientDpi && (
                            <div className="error-message">
                                {errors.patientDpi}
                            </div>
                        )}
                    </div>

                    <div className="emergency-form-group">
                        <label htmlFor="emergency-branch">
                            Sucursal
                        </label>

                        <select
                            id="emergency-branch"
                            value={branchId}
                            disabled={loadingBranches}
                            className={
                                errors.branchId
                                    ? "input-error"
                                    : ""
                            }
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
                        >
                            <option value="">
                                {loadingBranches
                                    ? "Cargando sucursales..."
                                    : "Seleccione una sucursal"}
                            </option>

                            {branches.map(branch => (
                                <option
                                    key={branch.id}
                                    value={branch.id}
                                >
                                    {branch.name}
                                </option>
                            ))}
                        </select>

                        {errors.branchId && (
                            <div className="error-message">
                                {errors.branchId}
                            </div>
                        )}
                    </div>

                    <div className="emergency-form-group">
                        <label htmlFor="emergency-note">
                            Nota de emergencia, opcional
                        </label>

                        <textarea
                            id="emergency-note"
                            rows="4"
                            maxLength="500"
                            placeholder="Describa brevemente la emergencia..."
                            value={emergencyNote}
                            className={
                                errors.emergencyNote
                                    ? "input-error"
                                    : ""
                            }
                            onChange={(event) => {
                                setEmergencyNote(
                                    event.target.value
                                );

                                setErrors(previous => ({
                                    ...previous,
                                    emergencyNote: ""
                                }));
                            }}
                        />

                        <div className="field-counter">
                            {emergencyNote.length} / 500
                        </div>

                        {errors.emergencyNote && (
                            <div className="error-message">
                                {errors.emergencyNote}
                            </div>
                        )}
                    </div>

                    <div className="emergency-modal-actions">
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
                            className="emergency-submit-button"
                            disabled={
                                processing ||
                                loadingBranches
                            }
                        >
                            {processing
                                ? "Registrando emergencia..."
                                : "Registrar Emergencia"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default EmergencyReceptionModal;