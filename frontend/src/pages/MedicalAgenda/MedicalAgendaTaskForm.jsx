import {
    useEffect,
    useMemo,
    useState
} from "react";

import {
    createMedicalAgendaTask,
    updateMedicalAgendaTask
} from "../../api/medicalAgendaTaskApi";

const PRIORITIES = [
    {
        value: "BAJA",
        label: "Baja"
    },
    {
        value: "NORMAL",
        label: "Normal"
    },
    {
        value: "ALTA",
        label: "Alta"
    }
];

/**
 * Formulario para crear o editar una tarea
 * personal del médico.
 */
function MedicalAgendaTaskForm({
    selectedDate,
    initialData,
    onSaved,
    onCancel
}) {
    const editing =
        Boolean(
            initialData?.taskId
        );

    const [
        title,
        setTitle
    ] = useState("");

    const [
        description,
        setDescription
    ] = useState("");

    const [
        priority,
        setPriority
    ] = useState("NORMAL");

    const [
        dueDate,
        setDueDate
    ] = useState("");

    const [
        dueTime,
        setDueTime
    ] = useState("08:00");

    const [
        errors,
        setErrors
    ] = useState({});

    const [
        saving,
        setSaving
    ] = useState(false);

    const formTitle =
        useMemo(
            () =>
                editing
                    ? "Editar Tarea"
                    : "Nueva Tarea",
            [editing]
        );

    /**
     * Inicializar el formulario.
     */
    useEffect(() => {
        const parsedDueDate =
            parseLocalDateTime(
                initialData?.dueDateTime
            );

        setTitle(
            initialData?.title ||
            ""
        );

        setDescription(
            initialData?.description ||
            ""
        );

        setPriority(
            initialData?.priority ||
            "NORMAL"
        );

        setDueDate(
            parsedDueDate.date ||
            selectedDate ||
            getTodayDate()
        );

        setDueTime(
            parsedDueDate.time ||
            "08:00"
        );

        setErrors({});
    }, [
        initialData,
        selectedDate
    ]);

    /**
     * Validar formulario.
     */
    const validateForm = () => {
        const newErrors = {};

        const cleanTitle =
            title.trim();

        const cleanDescription =
            description.trim();

        if (!cleanTitle) {
            newErrors.title =
                "El título de la tarea es obligatorio.";

        } else if (
            cleanTitle.length > 150
        ) {
            newErrors.title =
                "El título no puede exceder los 150 caracteres.";
        }

        if (
            cleanDescription.length > 1000
        ) {
            newErrors.description =
                "La descripción no puede exceder los 1000 caracteres.";
        }

        if (!priority) {
            newErrors.priority =
                "Debe seleccionar una prioridad.";
        }

        if (!dueDate) {
            newErrors.dueDate =
                "La fecha límite es obligatoria.";
        }

        if (!dueTime) {
            newErrors.dueTime =
                "La hora límite es obligatoria.";
        }

        if (
            editing &&
            (
                initialData?.version === null ||
                initialData?.version === undefined
            )
        ) {
            newErrors.version =
                "No fue posible determinar la versión actual de la tarea.";
        }

        setErrors(
            newErrors
        );

        return (
            Object.keys(
                newErrors
            ).length === 0
        );
    };

    /**
     * Guardar tarea.
     */
    const handleSubmit =
        async event => {
            event.preventDefault();

            if (!validateForm()) {
                return;
            }

            const payload = {
                title:
                    title.trim(),
                description:
                    description.trim(),
                priority,
                dueDateTime:
                    `${dueDate}T${dueTime}:00`,
                version:
                    initialData?.version
            };

            try {
                setSaving(true);

                const response =
                    editing
                        ? await updateMedicalAgendaTask(
                            initialData.taskId,
                            payload
                        )
                        : await createMedicalAgendaTask(
                            payload
                        );

                alert(
                    response.data
                        ?.message ||
                    (
                        editing
                            ? "Tarea actualizada correctamente."
                            : "Tarea creada correctamente."
                    )
                );

                if (onSaved) {
                    onSaved(
                        response.data
                    );
                }

            } catch (error) {
                console.error(
                    "Error al guardar tarea:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible guardar la tarea."
                );

            } finally {
                setSaving(false);
            }
        };

    return (
        <div
            className="medical-agenda-modal-backdrop"
            role="presentation"
        >
            <div
                className="medical-agenda-modal medical-agenda-task-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="task-form-title"
            >
                <div className="medical-agenda-modal-header">
                    <div>
                        <h2
                            id="task-form-title"
                            className="section-title"
                        >
                            {formTitle}
                        </h2>

                        <p>
                            Registre actividades y recordatorios personales.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="medical-agenda-close-button"
                        onClick={
                            onCancel
                        }
                        disabled={saving}
                        aria-label="Cerrar formulario"
                    >
                        ×
                    </button>
                </div>

                <form
                    onSubmit={
                        handleSubmit
                    }
                >
                    <div className="form-grid">
                        <div className="form-group">
                            <label>
                                Título
                            </label>

                            <input
                                type="text"
                                maxLength={150}
                                placeholder="Ejemplo: Revisar resultados"
                                value={title}
                                disabled={saving}
                                className={
                                    errors.title
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setTitle(
                                            event.target
                                                .value
                                        )
                                }
                            />

                            {errors.title && (
                                <div className="error-message">
                                    {errors.title}
                                </div>
                            )}

                            <small>
                                {title.length}/150
                            </small>
                        </div>

                        <div className="form-group">
                            <label>
                                Prioridad
                            </label>

                            <select
                                value={
                                    priority
                                }
                                disabled={saving}
                                className={
                                    errors.priority
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setPriority(
                                            event.target
                                                .value
                                        )
                                }
                            >
                                {PRIORITIES.map(
                                    item => (
                                        <option
                                            key={
                                                item.value
                                            }
                                            value={
                                                item.value
                                            }
                                        >
                                            {
                                                item.label
                                            }
                                        </option>
                                    )
                                )}
                            </select>

                            {errors.priority && (
                                <div className="error-message">
                                    {errors.priority}
                                </div>
                            )}
                        </div>

                        <div className="form-group">
                            <label>
                                Fecha límite
                            </label>

                            <input
                                type="date"
                                value={
                                    dueDate
                                }
                                disabled={saving}
                                className={
                                    errors.dueDate
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setDueDate(
                                            event.target
                                                .value
                                        )
                                }
                            />

                            {errors.dueDate && (
                                <div className="error-message">
                                    {errors.dueDate}
                                </div>
                            )}
                        </div>

                        <div className="form-group">
                            <label>
                                Hora límite
                            </label>

                            <input
                                type="time"
                                step="1800"
                                value={
                                    dueTime
                                }
                                disabled={saving}
                                className={
                                    errors.dueTime
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setDueTime(
                                            event.target
                                                .value
                                        )
                                }
                            />

                            {errors.dueTime && (
                                <div className="error-message">
                                    {errors.dueTime}
                                </div>
                            )}
                        </div>

                        <div className="form-group medical-agenda-full-width">
                            <label>
                                Descripción
                            </label>

                            <textarea
                                rows={6}
                                maxLength={1000}
                                placeholder="Descripción opcional"
                                value={
                                    description
                                }
                                disabled={saving}
                                className={
                                    errors.description
                                        ? "input-error"
                                        : ""
                                }
                                onChange={
                                    event =>
                                        setDescription(
                                            event.target
                                                .value
                                        )
                                }
                            />

                            {errors.description && (
                                <div className="error-message">
                                    {
                                        errors.description
                                    }
                                </div>
                            )}

                            <small>
                                {description.length}/1000
                            </small>
                        </div>
                    </div>

                    {errors.version && (
                        <div className="error-message">
                            {errors.version}
                        </div>
                    )}

                    <div className="button-row">
                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={saving}
                        >
                            {saving
                                ? "Guardando..."
                                : (
                                    editing
                                        ? "Actualizar tarea"
                                        : "Crear tarea"
                                )}
                        </button>

                        <button
                            type="button"
                            className="btn btn-secondary"
                            disabled={saving}
                            onClick={
                                onCancel
                            }
                        >
                            Cancelar
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

function parseLocalDateTime(
    value
) {
    if (!value) {
        return {
            date: "",
            time: ""
        };
    }

    const parts =
        String(value)
            .split("T");

    return {
        date:
            parts[0] || "",
        time:
            parts[1]
                ? parts[1]
                    .substring(
                        0,
                        5
                    )
                : ""
    };
}

function getTodayDate() {
    const date =
        new Date();

    const year =
        date.getFullYear();

    const month =
        String(
            date.getMonth() + 1
        ).padStart(
            2,
            "0"
        );

    const day =
        String(
            date.getDate()
        ).padStart(
            2,
            "0"
        );

    return `${year}-${month}-${day}`;
}

export default MedicalAgendaTaskForm;