import { useEffect, useState } from "react";

import {
    getAvailableReceptionDoctors,
    reassignAppointmentDoctor
} from "../../api/receptionApi";

import "./ReassignDoctorModal.css";

function ReassignDoctorModal({
    appointment,
    onClose,
    onReassigned
}) {
    const [doctors, setDoctors] =
        useState([]);

    const [newDoctorId, setNewDoctorId] =
        useState("");

    const [note, setNote] =
        useState("");

    const [errors, setErrors] =
        useState({});

    const [loading, setLoading] =
        useState(true);

    const [processing, setProcessing] =
        useState(false);

    const [generalError, setGeneralError] =
        useState("");

    useEffect(() => {
        loadDoctors();
    }, [appointment.appointmentId]);

    const loadDoctors = async () => {
        try {
            setLoading(true);
            setGeneralError("");

            const response =
                await getAvailableReceptionDoctors(
                    appointment.appointmentId
                );

            setDoctors(response.data);

        } catch (error) {
            console.error(error);

            setGeneralError(
                error.response?.data?.message ||
                "No fue posible cargar los médicos disponibles."
            );

        } finally {
            setLoading(false);
        }
    };

    const validateForm = () => {
        const newErrors = {};

        if (!newDoctorId) {
            newErrors.newDoctorId =
                "Debe seleccionar el nuevo médico.";
        }

        const cleanNote =
            note.trim();

        if (cleanNote.length > 500) {
            newErrors.note =
                "La nota de reasignación no puede exceder los 500 caracteres.";
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
                await reassignAppointmentDoctor(
                    appointment.appointmentId,
                    {
                        newDoctorId:
                            Number(newDoctorId),

                        note:
                            note.trim() || null
                    }
                );

            onReassigned(response.data);

        } catch (error) {
            console.error(error);

            setGeneralError(
                error.response?.data?.message ||
                "No fue posible reasignar el médico."
            );

        } finally {
            setProcessing(false);
        }
    };

    return (
        <div
            className="reassign-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="reassign-title"
        >
            <div className="reassign-modal">
                <div className="reassign-header">
                    <div>
                        <h2 id="reassign-title">
                            Reasignar Médico
                        </h2>

                        <p>
                            Cita #{appointment.appointmentId}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="reassign-close"
                        onClick={onClose}
                        disabled={processing}
                        aria-label="Cerrar"
                    >
                        ×
                    </button>
                </div>

                <div className="reassign-summary">
                    <div>
                        <span>Paciente</span>

                        <strong>
                            {appointment.patientName}
                        </strong>
                    </div>

                    <div>
                        <span>Médico actual</span>

                        <strong>
                            {appointment.doctorName}
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
                </div>

                {generalError && (
                    <div className="reassign-error">
                        {generalError}
                    </div>
                )}

                {loading ? (
                    <p>
                        Cargando médicos disponibles...
                    </p>

                ) : doctors.length === 0 ? (
                    <div className="reassign-empty">
                        <p>
                            No hay otros médicos disponibles
                            para la fecha y hora de esta cita.
                        </p>

                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={onClose}
                        >
                            Cerrar
                        </button>
                    </div>

                ) : (
                    <form onSubmit={handleSubmit}>
                        <div className="reassign-form-group">
                            <label htmlFor="new-doctor">
                                Nuevo médico
                            </label>

                            <select
                                id="new-doctor"
                                value={newDoctorId}
                                className={
                                    errors.newDoctorId
                                        ? "input-error"
                                        : ""
                                }
                                onChange={(event) => {
                                    setNewDoctorId(
                                        event.target.value
                                    );

                                    setErrors(previous => ({
                                        ...previous,
                                        newDoctorId: ""
                                    }));

                                    setGeneralError("");
                                }}
                            >
                                <option value="">
                                    Seleccione un médico
                                </option>

                                {doctors.map(doctor => (
                                    <option
                                        key={doctor.id}
                                        value={doctor.id}
                                    >
                                        {doctor.fullName}
                                    </option>
                                ))}
                            </select>

                            {errors.newDoctorId && (
                                <div className="error-message">
                                    {errors.newDoctorId}
                                </div>
                            )}
                        </div>

                        <div className="reassign-form-group">
                            <label htmlFor="reassignment-note">
                                Motivo de reasignación, opcional
                            </label>

                            <textarea
                                id="reassignment-note"
                                rows="4"
                                maxLength="500"
                                value={note}
                                className={
                                    errors.note
                                        ? "input-error"
                                        : ""
                                }
                                onChange={(event) => {
                                    setNote(
                                        event.target.value
                                    );

                                    setErrors(previous => ({
                                        ...previous,
                                        note: ""
                                    }));
                                }}
                            />

                            <small>
                                {note.length} / 500 caracteres
                            </small>

                            {errors.note && (
                                <div className="error-message">
                                    {errors.note}
                                </div>
                            )}
                        </div>

                        <div className="reassign-actions">
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
                                    !newDoctorId
                                }
                            >
                                {processing
                                    ? "Reasignando..."
                                    : "Confirmar Reasignación"}
                            </button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}

export default ReassignDoctorModal;