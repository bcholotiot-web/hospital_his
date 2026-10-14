import {
    useCallback,
    useEffect,
    useMemo,
    useState
} from "react";

import {
    changeMedicalAgendaTaskCompletion,
    changeMedicalAgendaTaskStatus,
    getMedicalAgendaTasks
} from "../../api/medicalAgendaTaskApi";

import MedicalAgendaTaskForm
    from "./MedicalAgendaTaskForm";

const TASK_FILTERS = [
    {
        value: "PENDING",
        label: "Pendientes"
    },
    {
        value: "COMPLETED",
        label: "Completadas"
    },
    {
        value: "ALL",
        label: "Todas"
    }
];

/**
 * Panel lateral de tareas de la agenda médica.
 */
function TaskPanel({
    selectedDate,
    canCreate,
    canUpdate,
    canDelete
}) {
    const [
        tasks,
        setTasks
    ] = useState([]);

    const [
        statusFilter,
        setStatusFilter
    ] = useState("PENDING");

    const [
        loading,
        setLoading
    ] = useState(false);

    const [
        processingTaskId,
        setProcessingTaskId
    ] = useState(null);

    const [
        errorMessage,
        setErrorMessage
    ] = useState("");

    const [
        taskFormData,
        setTaskFormData
    ] = useState(null);

    /**
     * Fecha visible para el usuario.
     */
    const formattedSelectedDate =
        useMemo(() => {
            return formatDate(
                selectedDate
            );
        }, [selectedDate]);

    /**
     * Cargar tareas.
     */
    const loadTasks =
        useCallback(async () => {
            if (!selectedDate) {
                setTasks([]);
                return;
            }

            try {
                setLoading(true);
                setErrorMessage("");

                const response =
                    await getMedicalAgendaTasks({
                        date:
                            selectedDate,
                        status:
                            statusFilter
                    });

                setTasks(
                    Array.isArray(
                        response.data
                    )
                        ? response.data
                        : []
                );

            } catch (error) {
                console.error(
                    "Error al cargar tareas:",
                    error
                );

                setTasks([]);

                setErrorMessage(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar las tareas."
                );

            } finally {
                setLoading(false);
            }
        }, [
            selectedDate,
            statusFilter
        ]);

    useEffect(() => {
        loadTasks();
    }, [loadTasks]);

    /**
     * Abrir formulario de creación.
     */
    const handleCreateTask = () => {
        if (!canCreate) {
            return;
        }

        setTaskFormData({
            dueDateTime:
                `${selectedDate}T08:00:00`,
            priority:
                "NORMAL"
        });
    };

    /**
     * Abrir edición.
     */
    const handleEditTask =
        task => {
            if (!canUpdate) {
                return;
            }

            setTaskFormData(
                task
            );
        };

    /**
     * Cerrar formulario.
     */
    const closeTaskForm = () => {
        setTaskFormData(null);
    };

    /**
     * Recargar después de guardar.
     */
    const handleTaskSaved = () => {
        setTaskFormData(null);
        loadTasks();
    };

    /**
     * Completar o reabrir.
     */
    const handleCompletionChange =
        async task => {
            if (!canUpdate) {
                return;
            }

            const newCompleted =
                !Boolean(
                    task.completed
                );

            try {
                setProcessingTaskId(
                    task.taskId
                );

                await changeMedicalAgendaTaskCompletion(
                    task.taskId,
                    newCompleted,
                    task.version
                );

                await loadTasks();

            } catch (error) {
                console.error(
                    "Error al cambiar tarea:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cambiar el estado de la tarea."
                );

                await loadTasks();

            } finally {
                setProcessingTaskId(
                    null
                );
            }
        };

    /**
     * Eliminar lógicamente.
     */
    const handleDeleteTask =
        async task => {
            if (!canDelete) {
                return;
            }

            const confirmed =
                window.confirm(
                    `¿Desea eliminar la tarea "${task.title}"?`
                );

            if (!confirmed) {
                return;
            }

            try {
                setProcessingTaskId(
                    task.taskId
                );

                const response =
                    await changeMedicalAgendaTaskStatus(
                        task.taskId,
                        false,
                        task.version
                    );

                alert(
                    response.data
                        ?.message ||
                    "Tarea eliminada correctamente."
                );

                await loadTasks();

            } catch (error) {
                console.error(
                    "Error al eliminar tarea:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible eliminar la tarea."
                );

                await loadTasks();

            } finally {
                setProcessingTaskId(
                    null
                );
            }
        };

    return (
        <>
            <aside className="medical-agenda-task-panel">
                <div className="medical-agenda-task-header">
                    <div>
                        <h2>
                            Tareas
                        </h2>

                        <p>
                            {formattedSelectedDate}
                        </p>
                    </div>

                    {canCreate && (
                        <button
                            type="button"
                            className="btn btn-primary"
                            onClick={
                                handleCreateTask
                            }
                        >
                            Nueva
                        </button>
                    )}
                </div>

                <div
                    className="medical-agenda-task-filters"
                    role="group"
                    aria-label="Filtros de tareas"
                >
                    {TASK_FILTERS.map(
                        filter => (
                            <button
                                key={
                                    filter.value
                                }
                                type="button"
                                className={
                                    statusFilter ===
                                        filter.value
                                        ? "medical-agenda-task-filter active"
                                        : "medical-agenda-task-filter"
                                }
                                onClick={() =>
                                    setStatusFilter(
                                        filter.value
                                    )
                                }
                                disabled={loading}
                            >
                                {filter.label}
                            </button>
                        )
                    )}
                </div>

                {errorMessage && (
                    <div className="medical-agenda-task-error">
                        {errorMessage}
                    </div>
                )}

                {loading ? (
                    <p className="medical-agenda-task-message">
                        Cargando tareas...
                    </p>

                ) : tasks.length === 0 ? (
                    <div className="medical-agenda-task-empty">
                        <strong>
                            Sin tareas
                        </strong>

                        <p>
                            No hay tareas para la fecha y filtro seleccionados.
                        </p>
                    </div>

                ) : (
                    <div className="medical-agenda-task-list">
                        {tasks.map(
                            task => (
                                <TaskItem
                                    key={
                                        task.taskId
                                    }
                                    task={task}
                                    canUpdate={
                                        canUpdate
                                    }
                                    canDelete={
                                        canDelete
                                    }
                                    processing={
                                        processingTaskId ===
                                        task.taskId
                                    }
                                    onEdit={
                                        handleEditTask
                                    }
                                    onCompletionChange={
                                        handleCompletionChange
                                    }
                                    onDelete={
                                        handleDeleteTask
                                    }
                                />
                            )
                        )}
                    </div>
                )}
            </aside>

            {taskFormData && (
                <MedicalAgendaTaskForm
                    selectedDate={
                        selectedDate
                    }
                    initialData={
                        taskFormData
                    }
                    onSaved={
                        handleTaskSaved
                    }
                    onCancel={
                        closeTaskForm
                    }
                />
            )}
        </>
    );
}

function TaskItem({
    task,
    canUpdate,
    canDelete,
    processing,
    onEdit,
    onCompletionChange,
    onDelete
}) {
    const priorityColor =
        safeColor(
            task.priorityColor,
            getPriorityColor(
                task.priority
            )
        );

    return (
        <article
            className={
                task.completed
                    ? "medical-agenda-task-item completed"
                    : (
                        task.overdue
                            ? "medical-agenda-task-item overdue"
                            : "medical-agenda-task-item"
                    )
            }
        >
            <div className="medical-agenda-task-item-top">
                <span
                    className="medical-agenda-task-priority"
                    style={{
                        borderColor:
                            priorityColor,
                        color:
                            priorityColor
                    }}
                >
                    {task.priorityLabel ||
                        formatTechnicalValue(
                            task.priority
                        )}
                </span>

                {task.overdue &&
                    !task.completed && (
                        <span className="medical-agenda-task-overdue">
                            Vencida
                        </span>
                    )}
            </div>

            <h3>
                {task.title}
            </h3>

            {task.description && (
                <p className="medical-agenda-task-description">
                    {task.description}
                </p>
            )}

            <div className="medical-agenda-task-due">
                <span>
                    Fecha límite
                </span>

                <strong>
                    {formatDateTime(
                        task.dueDateTime
                    )}
                </strong>
            </div>

            {task.completed &&
                task.completedAt && (
                    <div className="medical-agenda-task-completed-at">
                        Completada:
                        {" "}
                        {formatDateTime(
                            task.completedAt
                        )}
                    </div>
                )}

            <div className="medical-agenda-task-actions">
                {canUpdate && (
                    <button
                        type="button"
                        className={
                            task.completed
                                ? "btn btn-secondary"
                                : "btn btn-primary"
                        }
                        disabled={processing}
                        onClick={() =>
                            onCompletionChange(
                                task
                            )
                        }
                    >
                        {task.completed
                            ? "Reabrir"
                            : "Completar"}
                    </button>
                )}

                {canUpdate && (
                    <button
                        type="button"
                        className="btn btn-secondary"
                        disabled={processing}
                        onClick={() =>
                            onEdit(
                                task
                            )
                        }
                    >
                        Editar
                    </button>
                )}

                {canDelete && (
                    <button
                        type="button"
                        className="btn btn-danger-outline"
                        disabled={processing}
                        onClick={() =>
                            onDelete(
                                task
                            )
                        }
                    >
                        Eliminar
                    </button>
                )}
            </div>
        </article>
    );
}

function formatDate(
    value
) {
    if (!value) {
        return "Fecha no seleccionada";
    }

    const date =
        parseDateOnly(
            value
        );

    if (!date) {
        return value;
    }

    return date.toLocaleDateString(
        "es-GT",
        {
            weekday: "long",
            day: "2-digit",
            month: "long",
            year: "numeric"
        }
    );
}

function formatDateTime(
    value
) {
    if (!value) {
        return "Sin información";
    }

    const date =
        new Date(value);

    if (
        Number.isNaN(
            date.getTime()
        )
    ) {
        return value;
    }

    return date.toLocaleString(
        "es-GT",
        {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}

function parseDateOnly(
    value
) {
    const [
        year,
        month,
        day
    ] = String(value)
        .split("-")
        .map(Number);

    if (
        !year ||
        !month ||
        !day
    ) {
        return null;
    }

    return new Date(
        year,
        month - 1,
        day
    );
}

function getPriorityColor(
    priority
) {
    const colors = {
        BAJA:
            "#3B82F6",
        NORMAL:
            "#F59E0B",
        ALTA:
            "#EF4444"
    };

    return colors[priority] ||
        "#64748B";
}

function safeColor(
    value,
    fallback
) {
    return /^#[0-9A-Fa-f]{6}$/
        .test(value || "")
        ? value
        : fallback;
}

function formatTechnicalValue(
    value
) {
    if (!value) {
        return "Sin información";
    }

    const text =
        String(value)
            .toLowerCase()
            .replace(
                /_/g,
                " "
            );

    return (
        text.charAt(0)
            .toUpperCase() +
        text.slice(1)
    );
}

export default TaskPanel;