import {
    useEffect,
    useMemo,
    useState
} from "react";

import {
    Link,
    useNavigate,
    useParams
} from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import {
    createFollowUpAppointment,
    getFollowUpAvailability,
    getFollowUpContext,
    getFollowUpsByConsultation
} from "../../api/followUpAppointmentApi";

import "./FollowUpAppointmentWizard.css";

/**
 * Wizard para programar una cita de seguimiento.
 *
 * El consultationId se recibe desde la URL:
 *
 * /doctor/follow-up/:consultationId
 */
function FollowUpAppointmentWizard() {
    const navigate =
        useNavigate();

    const {
        consultationId
    } = useParams();

    /*
     * Paso actual del wizard:
     *
     * 1: contexto y tipo.
     * 2: fecha y horario.
     * 3: motivo, prioridad y confirmación.
     * 4: resultado exitoso.
     */
    const [currentStep, setCurrentStep] =
        useState(1);

    /*
     * Datos que el backend obtiene desde
     * la consulta original.
     */
    const [context, setContext] =
        useState(null);

    /*
     * Seguimientos ya registrados para la consulta.
     */
    const [existingFollowUps, setExistingFollowUps] =
        useState([]);

    const [followUpType, setFollowUpType] =
        useState("");

    const [selectedDate, setSelectedDate] =
        useState("");

    const [availableSlots, setAvailableSlots] =
        useState([]);

    const [
        selectedAppointmentDateTime,
        setSelectedAppointmentDateTime
    ] = useState("");

    const [reason, setReason] =
        useState("");

    const [priority, setPriority] =
        useState("NORMAL");

    /*
     * Se genera una UUID por intento de creación.
     */
    const [
        idempotencyKey,
        setIdempotencyKey
    ] = useState(
        () => crypto.randomUUID()
    );

    const [createdAppointment, setCreatedAppointment] =
        useState(null);

    const [errors, setErrors] =
        useState({});

    const [generalError, setGeneralError] =
        useState("");

    const [loadingContext, setLoadingContext] =
        useState(true);

    const [loadingSlots, setLoadingSlots] =
        useState(false);

    const [processing, setProcessing] =
        useState(false);

    /**
     * Carga el contexto y el historial cuando
     * se abre el wizard.
     */
    useEffect(() => {
        loadInitialData();
    }, [consultationId]);

    /**
     * Obtiene el contexto validado por Spring Boot
     * y los seguimientos previamente registrados.
     */
    const loadInitialData = async () => {
        const numericConsultationId =
            Number(consultationId);

        if (
            !Number.isInteger(
                numericConsultationId
            ) ||
            numericConsultationId <= 0
        ) {
            setGeneralError(
                "El identificador de la consulta no es válido."
            );

            setLoadingContext(false);
            return;
        }

        try {
            setLoadingContext(true);
            setGeneralError("");

            const [
                contextResponse,
                followUpsResponse
            ] = await Promise.all([
                getFollowUpContext(
                    numericConsultationId
                ),

                getFollowUpsByConsultation(
                    numericConsultationId
                )
            ]);

            setContext(
                contextResponse.data
            );

            setExistingFollowUps(
                Array.isArray(
                    followUpsResponse.data
                )
                    ? followUpsResponse.data
                    : []
            );

        } catch (error) {
            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible cargar el contexto de seguimiento."
                )
            );
        } finally {
            setLoadingContext(false);
        }
    };

    /**
     * Tipos de seguimiento que ya tienen
     * una cita activa.
     */
    const activeFollowUpTypes =
        useMemo(() => {
            return existingFollowUps
                .filter(appointment =>
                    appointment.active &&
                    ![
                        "CANCELADA",
                        "EXPIRADA",
                        "NO_ASISTIO"
                    ].includes(
                        appointment.status
                    )
                )
                .map(appointment =>
                    appointment.followUpType
                );
        }, [existingFollowUps]);

    /**
     * Devuelve la fecha mínima permitida.
     *
     * El día de hoy puede mostrarse, pero el backend
     * solo aceptará una hora futura y válida.
     */
    const minimumDate =
        useMemo(() => {
            return new Date()
                .toISOString()
                .slice(0, 10);
        }, []);

    /**
     * Valida el primer paso.
     */
    const handleContinueToAvailability = () => {
        const newErrors = {};

        if (!followUpType) {
            newErrors.followUpType =
                "Debe seleccionar el tipo de seguimiento.";
        }

        if (
            activeFollowUpTypes.includes(
                followUpType
            )
        ) {
            newErrors.followUpType =
                "La consulta ya cuenta con una cita activa de este tipo.";
        }

        setErrors(newErrors);

        if (
            Object.keys(newErrors).length > 0
        ) {
            return;
        }

        setCurrentStep(2);
        setGeneralError("");
    };

    /**
     * Consulta la disponibilidad para una fecha.
     */
    const handleLoadAvailability = async () => {
        if (!selectedDate) {
            setErrors(previous => ({
                ...previous,
                selectedDate:
                    "Debe seleccionar una fecha."
            }));

            return;
        }

        try {
            setLoadingSlots(true);
            setGeneralError("");
            setAvailableSlots([]);
            setSelectedAppointmentDateTime("");

            const response =
                await getFollowUpAvailability(
                    Number(consultationId),
                    selectedDate
                );

            const slots =
                Array.isArray(response.data)
                    ? response.data
                    : [];

            setAvailableSlots(slots);

            if (
                !slots.some(slot =>
                    Boolean(slot.available)
                )
            ) {
                setGeneralError(
                    "No existen horarios disponibles para la fecha seleccionada."
                );
            }

        } catch (error) {
            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible consultar la disponibilidad."
                )
            );
        } finally {
            setLoadingSlots(false);
        }
    };

    /**
     * Valida el horario y avanza a la confirmación.
     */
    const handleContinueToConfirmation = () => {
        if (
            !selectedAppointmentDateTime
        ) {
            setErrors(previous => ({
                ...previous,
                selectedSlot:
                    "Debe seleccionar un horario disponible."
            }));

            return;
        }

        setErrors(previous => ({
            ...previous,
            selectedSlot: ""
        }));

        setCurrentStep(3);
    };

    /**
     * Valida motivo y prioridad.
     */
    const validateConfirmation = () => {
        const newErrors = {};

        const cleanReason =
            reason.trim();

        if (!cleanReason) {
            newErrors.reason =
                "El motivo del seguimiento es obligatorio.";

        } else if (
            cleanReason.length < 10
        ) {
            newErrors.reason =
                "El motivo debe contener al menos 10 caracteres.";

        } else if (
            cleanReason.length > 2000
        ) {
            newErrors.reason =
                "El motivo no puede exceder los 2000 caracteres.";
        }

        if (
            ![
                "NORMAL",
                "EMERGENCIA"
            ].includes(priority)
        ) {
            newErrors.priority =
                "La prioridad seleccionada no es válida.";
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors).length === 0
        );
    };

    /**
     * Confirma la cita.
     *
     * Spring Boot vuelve a validar disponibilidad
     * dentro de la transacción.
     */
    const handleCreateFollowUp = async (
        event
    ) => {
        event.preventDefault();

        if (processing) {
            return;
        }

        setGeneralError("");

        if (!validateConfirmation()) {
            return;
        }

        const request = {
            parentConsultationId:
                Number(consultationId),

            followUpType,

            appointmentDateTime:
                selectedAppointmentDateTime,

            reason:
                reason.trim(),

            priority,

            idempotencyKey
        };

        try {
            setProcessing(true);

            const response =
                await createFollowUpAppointment(
                    request
                );

            setCreatedAppointment(
                response.data
            );

            setCurrentStep(4);

        } catch (error) {
            const message =
                getBackendMessage(
                    error,
                    "No fue posible crear la cita de seguimiento."
                );

            setGeneralError(message);

            /*
             * Flujo alterno:
             * otro usuario tomó el horario entre
             * la selección y la confirmación.
             */
            if (
                message.includes(
                    "horario seleccionado ya no está disponible"
                )
            ) {
                setSelectedAppointmentDateTime("");
                setAvailableSlots([]);
                setCurrentStep(2);
                setIdempotencyKey(
                    crypto.randomUUID()
                );

                /*
                 * Volver a consultar la fecha seleccionada.
                 */
                if (selectedDate) {
                    try {
                        const response =
                            await getFollowUpAvailability(
                                Number(
                                    consultationId
                                ),
                                selectedDate
                            );

                        setAvailableSlots(
                            Array.isArray(
                                response.data
                            )
                                ? response.data
                                : []
                        );
                    } catch {
                        /*
                         * Se mantiene el mensaje original
                         * de horario ocupado.
                         */
                    }
                }
            }

        } finally {
            setProcessing(false);
        }
    };

    /**
     * Cancela el wizard y regresa a la vista anterior,
     * sin guardar información parcial.
     */
    const handleCancel = () => {
        navigate(-1);
    };

    if (loadingContext) {
        return (
            <MainLayout>
                <div className="follow-up-loading">
                    Cargando información de seguimiento...
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="follow-up-page">
                <header className="follow-up-header">
                    <div>
                        <h1>
                            Agendar Cita de Seguimiento
                        </h1>

                        <p>
                            Programe una cita posterior para
                            revisar el tratamiento o los
                            resultados de laboratorio.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="follow-up-back-button"
                        onClick={handleCancel}
                    >
                        Volver
                    </button>
                </header>

                {generalError && (
                    <div
                        className="follow-up-message error"
                        role="alert"
                    >
                        {generalError}
                    </div>
                )}

                {context && (
                    <section className="follow-up-context">
                        <ContextItem
                            label="Paciente"
                            value={context.patientName}
                        />

                        <ContextItem
                            label="Médico"
                            value={context.doctorName}
                        />

                        <ContextItem
                            label="Especialidad"
                            value={context.specialtyName}
                        />

                        <ContextItem
                            label="Sucursal"
                            value={context.branchName}
                        />

                        <ContextItem
                            label="Diagnóstico"
                            value={
                                context.diagnosis ||
                                "No registrado"
                            }
                        />

                        <ContextItem
                            label="Plan de tratamiento"
                            value={
                                context.treatmentPlan ||
                                "No registrado"
                            }
                        />
                    </section>
                )}

                <nav className="follow-up-steps">
                    <Step
                        number={1}
                        label="Tipo"
                        active={
                            currentStep === 1
                        }
                        completed={
                            currentStep > 1
                        }
                    />

                    <Step
                        number={2}
                        label="Horario"
                        active={
                            currentStep === 2
                        }
                        completed={
                            currentStep > 2
                        }
                    />

                    <Step
                        number={3}
                        label="Confirmación"
                        active={
                            currentStep === 3
                        }
                        completed={
                            currentStep > 3
                        }
                    />

                    <Step
                        number={4}
                        label="Resultado"
                        active={
                            currentStep === 4
                        }
                        completed={false}
                    />
                </nav>

                {currentStep === 1 && (
                    <section className="follow-up-card">
                        <h2>
                            Tipo de seguimiento
                        </h2>

                        <div className="follow-up-types">
                            <FollowUpTypeOption
                                value="MONITOREO_TRATAMIENTO"
                                title="Monitoreo de tratamiento"
                                description={
                                    "Evaluar la respuesta clínica al tratamiento indicado."
                                }
                                checked={
                                    followUpType ===
                                    "MONITOREO_TRATAMIENTO"
                                }
                                disabled={
                                    activeFollowUpTypes
                                        .includes(
                                            "MONITOREO_TRATAMIENTO"
                                        )
                                }
                                onChange={
                                    setFollowUpType
                                }
                            />

                            <FollowUpTypeOption
                                value="REVISION_RESULTADOS_LABORATORIO"
                                title="Revisión de resultados de laboratorio"
                                description={
                                    "Revisar resultados de exámenes solicitados durante la consulta."
                                }
                                checked={
                                    followUpType ===
                                    "REVISION_RESULTADOS_LABORATORIO"
                                }
                                disabled={
                                    activeFollowUpTypes
                                        .includes(
                                            "REVISION_RESULTADOS_LABORATORIO"
                                        )
                                }
                                onChange={
                                    setFollowUpType
                                }
                            />
                        </div>

                        {errors.followUpType && (
                            <div className="follow-up-field-error">
                                {errors.followUpType}
                            </div>
                        )}

                        <div className="follow-up-actions">
                            <button
                                type="button"
                                className="secondary"
                                onClick={handleCancel}
                            >
                                Cancelar
                            </button>

                            <button
                                type="button"
                                className="primary"
                                onClick={
                                    handleContinueToAvailability
                                }
                            >
                                Continuar
                            </button>
                        </div>
                    </section>
                )}

                {currentStep === 2 && (
                    <section className="follow-up-card">
                        <h2>
                            Seleccionar fecha y horario
                        </h2>

                        <div className="follow-up-date-search">
                            <div>
                                <label htmlFor="follow-up-date">
                                    Fecha
                                </label>

                                <input
                                    id="follow-up-date"
                                    type="date"
                                    min={minimumDate}
                                    value={selectedDate}
                                    onChange={(event) => {
                                        setSelectedDate(
                                            event.target.value
                                        );

                                        setAvailableSlots([]);
                                        setSelectedAppointmentDateTime("");

                                        setErrors(previous => ({
                                            ...previous,
                                            selectedDate: "",
                                            selectedSlot: ""
                                        }));

                                        setGeneralError("");
                                    }}
                                />

                                {errors.selectedDate && (
                                    <div className="follow-up-field-error">
                                        {errors.selectedDate}
                                    </div>
                                )}
                            </div>

                            <button
                                type="button"
                                className="primary"
                                onClick={
                                    handleLoadAvailability
                                }
                                disabled={
                                    loadingSlots ||
                                    !selectedDate
                                }
                            >
                                {loadingSlots
                                    ? "Consultando..."
                                    : "Consultar horarios"}
                            </button>
                        </div>

                        {availableSlots.length > 0 && (
                            <div className="follow-up-slots">
                                {availableSlots.map(
                                    slot => (
                                        <button
                                            key={
                                                slot.dateTime
                                            }
                                            type="button"
                                            className={
                                                selectedAppointmentDateTime ===
                                                    slot.dateTime
                                                    ? "selected"
                                                    : ""
                                            }
                                            disabled={
                                                !slot.available
                                            }
                                            onClick={() => {
                                                setSelectedAppointmentDateTime(
                                                    slot.dateTime
                                                );

                                                setErrors(previous => ({
                                                    ...previous,
                                                    selectedSlot: ""
                                                }));
                                            }}
                                        >
                                            {slot.time}

                                            {!slot.available && (
                                                <small>
                                                    No disponible
                                                </small>
                                            )}
                                        </button>
                                    )
                                )}
                            </div>
                        )}

                        {errors.selectedSlot && (
                            <div className="follow-up-field-error">
                                {errors.selectedSlot}
                            </div>
                        )}

                        <div className="follow-up-actions">
                            <button
                                type="button"
                                className="secondary"
                                onClick={() =>
                                    setCurrentStep(1)
                                }
                            >
                                Anterior
                            </button>

                            <button
                                type="button"
                                className="primary"
                                onClick={
                                    handleContinueToConfirmation
                                }
                                disabled={
                                    !selectedAppointmentDateTime
                                }
                            >
                                Continuar
                            </button>
                        </div>
                    </section>
                )}

                {currentStep === 3 && (
                    <section className="follow-up-card">
                        <h2>
                            Confirmar seguimiento
                        </h2>

                        <form
                            onSubmit={
                                handleCreateFollowUp
                            }
                        >
                            <div className="follow-up-confirmation-summary">
                                <ContextItem
                                    label="Tipo"
                                    value={formatStatus(
                                        followUpType
                                    )}
                                />

                                <ContextItem
                                    label="Fecha y hora"
                                    value={formatDateTime(
                                        selectedAppointmentDateTime
                                    )}
                                />
                            </div>

                            <div className="follow-up-form-group">
                                <label htmlFor="follow-up-reason">
                                    Motivo del seguimiento
                                </label>

                                <textarea
                                    id="follow-up-reason"
                                    rows={5}
                                    maxLength={2000}
                                    value={reason}
                                    onChange={(event) => {
                                        setReason(
                                            event.target.value
                                        );

                                        setErrors(previous => ({
                                            ...previous,
                                            reason: ""
                                        }));
                                    }}
                                    disabled={processing}
                                />

                                <small>
                                    {reason.length}/2000
                                </small>

                                {errors.reason && (
                                    <div className="follow-up-field-error">
                                        {errors.reason}
                                    </div>
                                )}
                            </div>

                            <div className="follow-up-form-group">
                                <label htmlFor="follow-up-priority">
                                    Prioridad
                                </label>

                                <select
                                    id="follow-up-priority"
                                    value={priority}
                                    onChange={(event) =>
                                        setPriority(
                                            event.target.value
                                        )
                                    }
                                    disabled={processing}
                                >
                                    <option value="NORMAL">
                                        Normal
                                    </option>

                                    <option value="EMERGENCIA">
                                        Emergencia
                                    </option>
                                </select>
                            </div>

                            <div className="follow-up-actions">
                                <button
                                    type="button"
                                    className="secondary"
                                    onClick={() =>
                                        setCurrentStep(2)
                                    }
                                    disabled={processing}
                                >
                                    Anterior
                                </button>

                                <button
                                    type="submit"
                                    className="primary"
                                    disabled={processing}
                                >
                                    {processing
                                        ? "Creando cita..."
                                        : "Confirmar cita de seguimiento"}
                                </button>
                            </div>
                        </form>
                    </section>
                )}

                {currentStep === 4 &&
                    createdAppointment && (
                        <section className="follow-up-result">
                            <div className="follow-up-result-icon">
                                ✓
                            </div>

                            <h2>
                                Cita de seguimiento creada
                            </h2>

                            <p>
                                {createdAppointment.message}
                            </p>

                            <div className="follow-up-result-details">
                                <ContextItem
                                    label="Cita"
                                    value={
                                        createdAppointment
                                            .appointmentId
                                    }
                                />

                                <ContextItem
                                    label="Paciente"
                                    value={
                                        createdAppointment
                                            .patientName
                                    }
                                />

                                <ContextItem
                                    label="Tipo"
                                    value={formatStatus(
                                        createdAppointment
                                            .followUpType
                                    )}
                                />

                                <ContextItem
                                    label="Fecha"
                                    value={formatDateTime(
                                        createdAppointment
                                            .appointmentDateTime
                                    )}
                                />

                                <ContextItem
                                    label="Estado"
                                    value={formatStatus(
                                        createdAppointment
                                            .status
                                    )}
                                />

                                <ContextItem
                                    label="Reserva vence"
                                    value={formatDateTime(
                                        createdAppointment
                                            .reservationExpiresAt
                                    )}
                                />
                            </div>

                            <div className="follow-up-actions center">
                                <Link
                                    to="/doctor"
                                    className="primary link-button"
                                >
                                    Volver al panel médico
                                </Link>
                            </div>
                        </section>
                    )}
            </div>
        </MainLayout>
    );
}

function ContextItem({
    label,
    value
}) {
    return (
        <div>
            <span>{label}</span>

            <strong>
                {value || "No disponible"}
            </strong>
        </div>
    );
}

function Step({
    number,
    label,
    active,
    completed
}) {
    return (
        <div
            className={[
                "follow-up-step",
                active ? "active" : "",
                completed ? "completed" : ""
            ]
                .filter(Boolean)
                .join(" ")}
        >
            <b>{number}</b>
            <span>{label}</span>
        </div>
    );
}

function FollowUpTypeOption({
    value,
    title,
    description,
    checked,
    disabled,
    onChange
}) {
    return (
        <label
            className={[
                "follow-up-type-option",
                checked ? "selected" : "",
                disabled ? "disabled" : ""
            ]
                .filter(Boolean)
                .join(" ")}
        >
            <input
                type="radio"
                name="followUpType"
                value={value}
                checked={checked}
                disabled={disabled}
                onChange={() =>
                    onChange(value)
                }
            />

            <div>
                <strong>{title}</strong>
                <p>{description}</p>

                {disabled && (
                    <small>
                        Ya existe un seguimiento activo
                        de este tipo.
                    </small>
                )}
            </div>
        </label>
    );
}

function formatStatus(value) {
    if (!value) {
        return "No disponible";
    }

    return String(value)
        .replaceAll("_", " ");
}

function formatDateTime(value) {
    if (!value) {
        return "No disponible";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString(
        "es-GT",
        {
            year: "numeric",
            month: "long",
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

export default FollowUpAppointmentWizard;