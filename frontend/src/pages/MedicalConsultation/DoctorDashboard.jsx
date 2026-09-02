import {
    useCallback,
    useEffect,
    useState
} from "react";

import MainLayout
    from "../../layouts/MainLayout";

import MedicalConsultationModal
    from "./MedicalConsultationModal";

import {
    finishPatientCare,
    getDoctorQueue,
    markAppointmentNoShow,
    startMedicalConsultation
} from "../../api/medicalConsultationApi";

import "./DoctorDashboard.css";

function DoctorDashboard() {
    const [queue, setQueue] =
        useState([]);

    const [
        selectedAppointmentId,
        setSelectedAppointmentId
    ] = useState(null);

    const [processingId, setProcessingId] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [errorMessage, setErrorMessage] =
        useState("");

    const [successMessage, setSuccessMessage] =
        useState("");

    const loadQueue = useCallback(
        async (showLoading = true) => {
            try {
                if (showLoading) {
                    setLoading(true);
                }

                const response =
                    await getDoctorQueue();

                setQueue(
                    Array.isArray(response.data)
                        ? response.data
                        : []
                );

                setErrorMessage("");

            } catch (error) {
                console.error(
                    "Error cargando panel médico:",
                    error
                );

                setErrorMessage(
                    getBackendMessage(
                        error,
                        "No fue posible cargar las citas del médico."
                    )
                );

            } finally {
                if (showLoading) {
                    setLoading(false);
                }
            }
        },
        []
    );

    //Actualiza la cola cada 30 segundos y limpia la cola de anuncios de voz al desmontar el componente
    useEffect(() => {
        loadQueue(true);

        const intervalId =
            setInterval(() => {
                loadQueue(false);
            }, 30000);

        return () => {
            clearInterval(intervalId);

            if (
                "speechSynthesis" in window
            ) {
                window.speechSynthesis.cancel();
            }
        };
    }, [loadQueue]);

    const waitingAppointments =
        queue.filter(
            item =>
                item.appointmentStatus ===
                "SIGNOS_REGISTRADOS"
        );

    const activeConsultations =
        queue.filter(
            item =>
                item.appointmentStatus ===
                "CONSULTA_MEDICA"
        );

    const evaluatedAppointments =
        queue.filter(
            item =>
                item.appointmentStatus ===
                "CONSULTA_EVALUADA"
        );

    const announcePatient = (
        appointment
    ) => {
        if (
            !("speechSynthesis" in window)
        ) {
            return;
        }

        window.speechSynthesis.cancel();

        const message =
            `Turno número ${appointment.appointmentId}. ` +
            `Paciente ${appointment.patientName}, ` +
            "favor pasar a consulta médica.";

        const utterance =
            new SpeechSynthesisUtterance(
                message
            );

        utterance.lang = "es-GT";
        utterance.rate = 0.9;
        utterance.pitch = 1;
        utterance.volume = 1;

        window.speechSynthesis.speak(
            utterance
        );
    };

    const handleStartConsultation =
        async (appointment) => {
            if (processingId !== null) {
                return;
            }

            try {
                setProcessingId(
                    appointment.appointmentId
                );

                setErrorMessage("");
                setSuccessMessage("");

                const response =
                    await startMedicalConsultation(
                        appointment.appointmentId
                    );

                announcePatient(
                    appointment
                );

                setSuccessMessage(
                    response.data.message
                );

                await loadQueue(false);

                setSelectedAppointmentId(
                    appointment.appointmentId
                );

            } catch (error) {
                setErrorMessage(
                    getBackendMessage(
                        error,
                        "No fue posible iniciar la consulta."
                    )
                );

            } finally {
                setProcessingId(null);
            }
        };

    const handleNoShow =
        async (appointment) => {
            if (processingId !== null) {
                return;
            }

            const accepted =
                window.confirm(
                    `¿Desea marcar la cita #${appointment.appointmentId} como No Asistió?`
                );

            if (!accepted) {
                return;
            }

            try {
                setProcessingId(
                    appointment.appointmentId
                );

                setErrorMessage("");
                setSuccessMessage("");

                await markAppointmentNoShow(
                    appointment.appointmentId
                );

                setSuccessMessage(
                    `Cita #${appointment.appointmentId} marcada como No Asistió.`
                );

                await loadQueue(false);

            } catch (error) {
                setErrorMessage(
                    getBackendMessage(
                        error,
                        "No fue posible marcar la inasistencia."
                    )
                );

            } finally {
                setProcessingId(null);
            }
        };

    const handleFinishCare =
        async (appointment) => {
            if (processingId !== null) {
                return;
            }

            const accepted =
                window.confirm(
                    `¿Desea finalizar la atención de la cita #${appointment.appointmentId}?`
                );

            if (!accepted) {
                return;
            }

            try {
                setProcessingId(
                    appointment.appointmentId
                );

                setErrorMessage("");
                setSuccessMessage("");

                await finishPatientCare(
                    appointment.appointmentId
                );

                setSuccessMessage(
                    `Atención finalizada para cita #${appointment.appointmentId}.`
                );

                await loadQueue(false);

            } catch (error) {
                setErrorMessage(
                    getBackendMessage(
                        error,
                        "No fue posible finalizar la atención."
                    )
                );

            } finally {
                setProcessingId(null);
            }
        };

    const handleConsultationSaved =
        async (response) => {
            setSelectedAppointmentId(null);

            setSuccessMessage(
                response.message
            );

            setErrorMessage("");

            await loadQueue(false);
        };

    return (
        <MainLayout>
            <div className="doctor-page">
                <header className="doctor-header">
                    <div>
                        <h1>Panel Médico</h1>

                        <p>
                            Gestione las consultas
                            asignadas y revise los signos
                            vitales del paciente.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() =>
                            loadQueue(true)
                        }
                        disabled={loading}
                    >
                        {loading
                            ? "Actualizando..."
                            : "Actualizar panel"}
                    </button>
                </header>

                {errorMessage && (
                    <div className="doctor-message error">
                        {errorMessage}
                    </div>
                )}

                {successMessage && (
                    <div className="doctor-message success">
                        {successMessage}
                    </div>
                )}

                {loading ? (
                    <div className="doctor-loading">
                        Cargando citas asignadas...
                    </div>
                ) : (
                    <div className="doctor-sections">
                        <DoctorSection
                            title="En Espera de Consulta"
                            appointments={
                                waitingAppointments
                            }
                            emptyMessage={
                                "No hay pacientes esperando consulta."
                            }
                            processingId={
                                processingId
                            }
                            onStart={
                                handleStartConsultation
                            }
                            onOpen={() => { }}
                            onFinish={() => { }}
                            onNoShow={
                                handleNoShow
                            }
                        />

                        <DoctorSection
                            title="En Consulta Médica"
                            appointments={
                                activeConsultations
                            }
                            emptyMessage={
                                "No hay consultas médicas en curso."
                            }
                            processingId={
                                processingId
                            }
                            onStart={() => { }}
                            onOpen={(appointment) =>
                                setSelectedAppointmentId(
                                    appointment
                                        .appointmentId
                                )
                            }
                            onFinish={() => { }}
                            onNoShow={() => { }}
                        />

                        <DoctorSection
                            title="Evaluados, Pendiente de Cierre"
                            appointments={
                                evaluatedAppointments
                            }
                            emptyMessage={
                                "No hay pacientes pendientes de cierre."
                            }
                            processingId={
                                processingId
                            }
                            onStart={() => { }}
                            onOpen={(appointment) =>
                                setSelectedAppointmentId(
                                    appointment
                                        .appointmentId
                                )
                            }
                            onFinish={
                                handleFinishCare
                            }
                            onNoShow={() => { }}
                        />
                    </div>
                )}

                {selectedAppointmentId && (
                    <MedicalConsultationModal
                        appointmentId={
                            selectedAppointmentId
                        }
                        onClose={() =>
                            setSelectedAppointmentId(
                                null
                            )
                        }
                        onSaved={
                            handleConsultationSaved
                        }
                    />
                )}
            </div>
        </MainLayout>
    );
}

function DoctorSection({
    title,
    appointments,
    emptyMessage,
    processingId,
    onStart,
    onOpen,
    onFinish,
    onNoShow
}) {
    return (
        <section className="doctor-section">
            <div className="doctor-section-header">
                <h2>{title}</h2>

                <span>
                    {appointments.length}
                </span>
            </div>

            {appointments.length === 0 ? (
                <div className="doctor-empty">
                    {emptyMessage}
                </div>
            ) : (
                <div className="doctor-grid">
                    {appointments.map(
                        appointment => {
                            const processing =
                                processingId ===
                                appointment.appointmentId;

                            return (
                                <article
                                    key={
                                        appointment
                                            .appointmentId
                                    }
                                    className={
                                        appointment
                                            .emergency
                                            ? "doctor-card emergency"
                                            : "doctor-card"
                                    }
                                >
                                    <div className="doctor-card-header">
                                        <div>
                                            <h3>
                                                {
                                                    appointment
                                                        .patientName
                                                }
                                            </h3>

                                            <p>
                                                Cita #
                                                {
                                                    appointment
                                                        .appointmentId
                                                }
                                            </p>
                                        </div>

                                        <span
                                            className={
                                                appointment
                                                    .emergency
                                                    ? "doctor-priority emergency"
                                                    : "doctor-priority normal"
                                            }
                                        >
                                            {
                                                appointment
                                                    .priority
                                            }
                                        </span>
                                    </div>

                                    <div className="doctor-card-details">
                                        <p>
                                            <span>
                                                Especialidad
                                            </span>
                                            <strong>
                                                {
                                                    appointment
                                                        .specialty
                                                }
                                            </strong>
                                        </p>

                                        <p>
                                            <span>
                                                Sucursal
                                            </span>
                                            <strong>
                                                {
                                                    appointment
                                                        .branch
                                                }
                                            </strong>
                                        </p>

                                        <p>
                                            <span>
                                                Estado
                                            </span>
                                            <strong>
                                                {
                                                    appointment
                                                        .appointmentStatus
                                                        .replaceAll(
                                                            "_",
                                                            " "
                                                        )
                                                }
                                            </strong>
                                        </p>

                                        <p>
                                            <span>
                                                Fecha
                                            </span>
                                            <strong>
                                                {formatDateTime(
                                                    appointment
                                                        .appointmentDateTime
                                                )}
                                            </strong>
                                        </p>
                                    </div>

                                    <div className="doctor-card-actions">
                                        {appointment
                                            .canStartConsultation && (
                                                <button
                                                    type="button"
                                                    className={
                                                        appointment
                                                            .emergency
                                                            ? "doctor-primary-action urgent"
                                                            : "doctor-primary-action"
                                                    }
                                                    disabled={
                                                        processing
                                                    }
                                                    onClick={() =>
                                                        onStart(
                                                            appointment
                                                        )
                                                    }
                                                >
                                                    {processing
                                                        ? "Iniciando..."
                                                        : "Iniciar Consulta"}
                                                </button>
                                            )}

                                        {appointment
                                            .canOpenConsultation && (
                                                <button
                                                    type="button"
                                                    className={
                                                        "doctor-primary-action"
                                                    }
                                                    onClick={() =>
                                                        onOpen(
                                                            appointment
                                                        )
                                                    }
                                                >
                                                    Ver / Completar
                                                    Consulta
                                                </button>
                                            )}

                                        {appointment
                                            .canFinishCare && (
                                                <button
                                                    type="button"
                                                    className={
                                                        "doctor-finish-action"
                                                    }
                                                    disabled={
                                                        processing
                                                    }
                                                    onClick={() =>
                                                        onFinish(
                                                            appointment
                                                        )
                                                    }
                                                >
                                                    {processing
                                                        ? "Finalizando..."
                                                        : "Finalizar Atención"}
                                                </button>
                                            )}

                                        {appointment
                                            .canMarkNoShow && (
                                                <button
                                                    type="button"
                                                    className={
                                                        "doctor-secondary-action"
                                                    }
                                                    disabled={
                                                        processing
                                                    }
                                                    onClick={() =>
                                                        onNoShow(
                                                            appointment
                                                        )
                                                    }
                                                >
                                                    No Asistió
                                                </button>
                                            )}
                                    </div>
                                </article>
                            );
                        }
                    )}
                </div>
            )}
        </section>
    );
}

function formatDateTime(value) {
    if (!value) {
        return "No disponible";
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

export default DoctorDashboard;