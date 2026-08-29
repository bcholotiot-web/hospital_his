import {
    useCallback,
    useEffect,
    useState
} from "react";

import MainLayout
    from "../../layouts/MainLayout";

import VitalSignsModal
    from "./VitalSignsModal";

import {
    callAppointmentPatient,
    callEmergencyPatient,
    getNursingQueue
} from "../../api/nursingApi";

import "./NursingDashboard.css";

function NursingDashboard() {
    const [queue, setQueue] =
        useState([]);

    const [selectedPatient, setSelectedPatient] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [callingId, setCallingId] =
        useState(null);

    const [errorMessage, setErrorMessage] =
        useState("");

    const [successMessage, setSuccessMessage] =
        useState("");

    const loadQueue = useCallback(async () => {
        try {
            setLoading(true);
            setErrorMessage("");

            const response =
                await getNursingQueue();

            setQueue(
                Array.isArray(response.data)
                    ? response.data
                    : []
            );

        } catch (error) {
            console.error(
                "Error cargando cola:",
                error
            );

            setErrorMessage(
                error.response?.data?.message ||
                "No fue posible cargar la cola de enfermería."
            );

        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadQueue();
    }, [loadQueue]);

    const waitingPatients =
        queue.filter(patient =>
            patient.canCallPatient
        );

    const calledPatients =
        queue.filter(patient =>
            patient.canRegisterVitalSigns
        );

    const announcePatient = (
        patient
    ) => {
        if (
            !("speechSynthesis" in window)
        ) {
            return;
        }

        window.speechSynthesis.cancel();

        const turnNumber =
            patient.sourceId;

        const message =
            `Turno número ${turnNumber}. ` +
            `Paciente ${patient.patientName}, ` +
            "favor pasar a toma de signos vitales.";

        const utterance =
            new SpeechSynthesisUtterance(
                message
            );

        utterance.lang = "es-GT";
        utterance.rate = 0.9;
        utterance.pitch = 1;

        window.speechSynthesis.speak(
            utterance
        );
    };

    const handleCallPatient =
        async (patient) => {
            if (callingId !== null) {
                return;
            }

            try {
                setCallingId(
                    `${patient.sourceType}-${patient.sourceId}`
                );

                setErrorMessage("");
                setSuccessMessage("");

                let response;

                if (
                    patient.sourceType ===
                    "EMERGENCY_RECEPTION"
                ) {
                    response =
                        await callEmergencyPatient(
                            patient.emergencyReceptionId
                        );

                } else {
                    response =
                        await callAppointmentPatient(
                            patient.appointmentId
                        );
                }

                announcePatient(
                    response.data
                );

                setSuccessMessage(
                    `Paciente ${response.data.patientName} llamado para toma de signos vitales.`
                );

                await loadQueue();

            } catch (error) {
                console.error(
                    "Error llamando paciente:",
                    error
                );

                setErrorMessage(
                    error.response?.data?.message ||
                    "No fue posible llamar al paciente."
                );

            } finally {
                setCallingId(null);
            }
        };

    const handleVitalSignsRegistered =
        async (response) => {
            setSelectedPatient(null);

            setSuccessMessage(
                response.message
            );

            setErrorMessage("");

            await loadQueue();
        };

    return (
        <MainLayout>
            <div className="nursing-page">
                <header className="nursing-header">
                    <div>
                        <h1>
                            Panel de Enfermería
                        </h1>

                        <p>
                            Gestione el llamado y registro
                            de signos vitales.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={loadQueue}
                        disabled={loading}
                    >
                        {loading
                            ? "Actualizando..."
                            : "Actualizar cola"}
                    </button>
                </header>

                {errorMessage && (
                    <div className="nursing-message error">
                        {errorMessage}
                    </div>
                )}

                {successMessage && (
                    <div className="nursing-message success">
                        {successMessage}
                    </div>
                )}

                {loading ? (
                    <div className="nursing-loading">
                        Cargando cola de enfermería...
                    </div>

                ) : (
                    <div className="nursing-sections">
                        <QueueSection
                            title="Pacientes esperando"
                            description={
                                "Pacientes presentes pendientes de llamado."
                            }
                            patients={waitingPatients}
                            emptyMessage={
                                "No hay pacientes esperando."
                            }
                            actionLabel={
                                "Llamar y Tomar Signos"
                            }
                            callingId={callingId}
                            onAction={
                                handleCallPatient
                            }
                            actionType="CALL"
                        />

                        <QueueSection
                            title="En toma de signos"
                            description={
                                "Pacientes que ya fueron llamados."
                            }
                            patients={calledPatients}
                            emptyMessage={
                                "No hay pacientes en toma de signos."
                            }
                            actionLabel={
                                "Registrar Signos Vitales"
                            }
                            callingId={callingId}
                            onAction={
                                setSelectedPatient
                            }
                            actionType="REGISTER"
                        />
                    </div>
                )}

                {selectedPatient && (
                    <VitalSignsModal
                        patient={selectedPatient}
                        onClose={() =>
                            setSelectedPatient(null)
                        }
                        onRegistered={
                            handleVitalSignsRegistered
                        }
                    />
                )}
            </div>
        </MainLayout>
    );
}

function QueueSection({
    title,
    description,
    patients,
    emptyMessage,
    actionLabel,
    callingId,
    onAction,
    actionType
}) {
    return (
        <section className="nursing-section">
            <div className="nursing-section-header">
                <div>
                    <h2>{title}</h2>
                    <p>{description}</p>
                </div>

                <span className="queue-count">
                    {patients.length}
                </span>
            </div>

            {patients.length === 0 ? (
                <div className="nursing-empty">
                    {emptyMessage}
                </div>

            ) : (
                <div className="nursing-grid">
                    {patients.map(patient => {
                        const patientKey =
                            `${patient.sourceType}-${patient.sourceId}`;

                        const isCalling =
                            callingId === patientKey;

                        return (
                            <article
                                key={patientKey}
                                className={
                                    patient.emergency
                                        ? "nursing-card emergency"
                                        : "nursing-card"
                                }
                            >
                                <div className="nursing-card-header">
                                    <div>
                                        <h3>
                                            {patient.patientName}
                                        </h3>

                                        <p>
                                            {patient.sourceType ===
                                                "APPOINTMENT"
                                                ? `Cita #${patient.appointmentId}`
                                                : `Emergencia #${patient.emergencyReceptionId}`}
                                        </p>
                                    </div>

                                    <span
                                        className={
                                            patient.emergency
                                                ? "nursing-priority emergency"
                                                : "nursing-priority normal"
                                        }
                                    >
                                        {patient.priority}
                                    </span>
                                </div>

                                <div className="nursing-card-details">
                                    <p>
                                        <span>Estado</span>
                                        <strong>
                                            {patient.status
                                                .replaceAll(
                                                    "_",
                                                    " "
                                                )}
                                        </strong>
                                    </p>

                                    <p>
                                        <span>Sucursal</span>
                                        <strong>
                                            {patient.branch}
                                        </strong>
                                    </p>

                                    {patient.specialty && (
                                        <p>
                                            <span>
                                                Especialidad
                                            </span>

                                            <strong>
                                                {patient.specialty}
                                            </strong>
                                        </p>
                                    )}

                                    {patient.doctorName && (
                                        <p>
                                            <span>Médico</span>
                                            <strong>
                                                {patient.doctorName}
                                            </strong>
                                        </p>
                                    )}

                                    <p>
                                        <span>
                                            Hora de llegada
                                        </span>

                                        <strong>
                                            {formatDateTime(
                                                patient.arrivalTime
                                            )}
                                        </strong>
                                    </p>
                                </div>

                                <button
                                    type="button"
                                    className={
                                        patient.emergency
                                            ? "nursing-action urgent"
                                            : "nursing-action"
                                    }
                                    disabled={
                                        isCalling
                                    }
                                    onClick={() =>
                                        onAction(patient)
                                    }
                                >
                                    {isCalling &&
                                        actionType === "CALL"
                                        ? "Llamando..."
                                        : actionLabel}
                                </button>
                            </article>
                        );
                    })}
                </div>
            )}
        </section>
    );
}

function formatDateTime(value) {
    if (!value) {
        return "No registrada";
    }

    return new Date(value)
        .toLocaleString(
            "es-GT",
            {
                year: "numeric",
                month: "short",
                day: "numeric",
                hour: "2-digit",
                minute: "2-digit"
            }
        );
}

export default NursingDashboard;