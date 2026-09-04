import {
    useCallback,
    useEffect,
    useState
} from "react";

import {
    getLaboratoryOrder,
    publishLaboratoryResult
} from "../../api/laboratoryApi";

import LaboratoryResultModal
    from "./LaboratoryResultModal";

import "./LaboratoryOrderDetail.css";

function LaboratoryOrderDetail({
    orderId,
    onClose,
    onOrderUpdated
}) {
    const [order, setOrder] =
        useState(null);

    const [
        selectedItem,
        setSelectedItem
    ] = useState(null);

    const [publishingItemId, setPublishingItemId] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [errorMessage, setErrorMessage] =
        useState("");

    const [successMessage, setSuccessMessage] =
        useState("");

    const loadOrder = useCallback(
        async () => {
            try {
                setLoading(true);
                setErrorMessage("");

                const response =
                    await getLaboratoryOrder(
                        orderId
                    );

                setOrder(response.data);

            } catch (error) {
                console.error(
                    "Error cargando orden:",
                    error
                );

                setErrorMessage(
                    getBackendMessage(
                        error,
                        "No fue posible cargar la orden."
                    )
                );

            } finally {
                setLoading(false);
            }
        },
        [orderId]
    );

    useEffect(() => {
        loadOrder();
    }, [loadOrder]);

    const handleResultSaved =
        async () => {
            setSelectedItem(null);

            setSuccessMessage(
                "Resultado guardado correctamente."
            );

            await loadOrder();

            if (onOrderUpdated) {
                onOrderUpdated();
            }
        };

    const handlePublish =
        async (item) => {
            if (publishingItemId !== null) {
                return;
            }

            const accepted =
                window.confirm(
                    `¿Desea publicar el resultado de ${item.testName}? Después de publicarlo no podrá modificarlo.`
                );

            if (!accepted) {
                return;
            }

            try {
                setPublishingItemId(
                    item.itemId
                );

                setErrorMessage("");
                setSuccessMessage("");

                const response =
                    await publishLaboratoryResult(
                        orderId,
                        item.itemId
                    );

                setOrder(response.data);

                setSuccessMessage(
                    response.data.message
                );

                if (onOrderUpdated) {
                    onOrderUpdated();
                }

            } catch (error) {
                console.error(
                    "Error publicando resultado:",
                    error
                );

                setErrorMessage(
                    getBackendMessage(
                        error,
                        "No fue posible publicar el resultado."
                    )
                );

            } finally {
                setPublishingItemId(null);
            }
        };

    if (loading) {
        return (
            <div className="lab-detail-overlay">
                <div className="lab-detail-modal">
                    Cargando orden...
                </div>
            </div>
        );
    }

    return (
        <div
            className="lab-detail-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="lab-detail-title"
        >
            <div className="lab-detail-modal">
                <header className="lab-detail-header">
                    <div>
                        <h2 id="lab-detail-title">
                            Detalle de Orden
                        </h2>

                        <p>
                            {order?.orderNumber}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="lab-detail-close"
                        onClick={onClose}
                        aria-label="Cerrar"
                    >
                        ×
                    </button>
                </header>

                {errorMessage && (
                    <div className="lab-detail-message error">
                        {errorMessage}
                    </div>
                )}

                {successMessage && (
                    <div className="lab-detail-message success">
                        {successMessage}
                    </div>
                )}

                {order && (
                    <>
                        <section className="lab-order-context">
                            <ContextField
                                label="Paciente"
                                value={
                                    order.patientName
                                }
                            />

                            <ContextField
                                label="Médico"
                                value={
                                    order.doctorName
                                }
                            />

                            <ContextField
                                label="Sucursal"
                                value={
                                    order.branch
                                }
                            />

                            <ContextField
                                label="Estado"
                                value={
                                    formatStatus(
                                        order.status
                                    )
                                }
                            />

                            <ContextField
                                label="Monto"
                                value={
                                    formatAmount(
                                        order.totalAmount
                                    )
                                }
                            />

                            <ContextField
                                label="Fecha de orden"
                                value={
                                    formatDateTime(
                                        order.createdAt
                                    )
                                }
                            />
                        </section>

                        {order.externalOrder && (
                            <div className="lab-external-warning">
                                Esta orden es externa. Los
                                resultados serán presentados
                                directamente al médico.
                            </div>
                        )}

                        {order.status ===
                            "PENDIENTE_DE_PAGO" && (
                                <div className="lab-payment-warning">
                                    La orden está pendiente de
                                    pago. No se pueden registrar
                                    resultados todavía.
                                </div>
                            )}

                        <section className="lab-progress">
                            <div>
                                <strong>
                                    Progreso de resultados
                                </strong>

                                <span>
                                    {
                                        order.publishedTests
                                    }{" "}
                                    de {order.totalTests}
                                    {" "}publicados
                                </span>
                            </div>

                            <div className="lab-progress-track">
                                <div
                                    className="lab-progress-value"
                                    style={{
                                        width:
                                            `${getProgress(
                                                order
                                            )}%`
                                    }}
                                />
                            </div>
                        </section>

                        {order.notes && (
                            <section className="lab-order-notes">
                                <strong>
                                    Observaciones de la orden
                                </strong>

                                <p>{order.notes}</p>
                            </section>
                        )}

                        <section className="lab-items-section">
                            <h3>
                                Exámenes solicitados
                            </h3>

                            <div className="lab-items-list">
                                {order.items?.map(
                                    item => (
                                        <LaboratoryItemCard
                                            key={
                                                item.itemId
                                            }
                                            item={item}
                                            publishing={
                                                publishingItemId ===
                                                item.itemId
                                            }
                                            onEdit={() =>
                                                setSelectedItem(
                                                    item
                                                )
                                            }
                                            onPublish={() =>
                                                handlePublish(
                                                    item
                                                )
                                            }
                                        />
                                    )
                                )}
                            </div>
                        </section>
                    </>
                )}

                <div className="lab-detail-actions">
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={onClose}
                    >
                        Cerrar
                    </button>
                </div>

                {selectedItem && (
                    <LaboratoryResultModal
                        orderId={orderId}
                        item={selectedItem}
                        onClose={() =>
                            setSelectedItem(
                                null
                            )
                        }
                        onSaved={
                            handleResultSaved
                        }
                    />
                )}
            </div>
        </div>
    );
}

function LaboratoryItemCard({
    item,
    publishing,
    onEdit,
    onPublish
}) {
    return (
        <article
            className={
                item.outOfRange
                    ? "lab-item-card out-of-range"
                    : "lab-item-card"
            }
        >
            <header className="lab-item-header">
                <div>
                    <h4>
                        {item.testName}
                    </h4>

                    <p>
                        {item.testCode}
                    </p>
                </div>

                <div className="lab-item-badges">
                    {item.outOfRange && (
                        <span className="lab-range-badge">
                            Fuera de rango
                        </span>
                    )}

                    <span
                        className={
                            `lab-item-status ${item.status
                                .toLowerCase()
                                .replaceAll(
                                    "_",
                                    "-"
                                )}`
                        }
                    >
                        {formatStatus(
                            item.status
                        )}
                    </span>
                </div>
            </header>

            <div className="lab-item-data">
                <p>
                    <span>
                        Rango de referencia
                    </span>

                    <strong>
                        {item.referenceRange ||
                            "No configurado"}
                    </strong>
                </p>

                <p>
                    <span>Resultado</span>

                    <strong>
                        {item.resultValue
                            ? `${item.resultValue} ${item.resultUnit || ""}`
                            : "Pendiente"}
                    </strong>
                </p>

                {item.resultDate && (
                    <p>
                        <span>
                            Fecha del resultado
                        </span>

                        <strong>
                            {formatDateTime(
                                item.resultDate
                            )}
                        </strong>
                    </p>
                )}

                {item.resultNotes && (
                    <p>
                        <span>
                            Observaciones
                        </span>

                        <strong>
                            {item.resultNotes}
                        </strong>
                    </p>
                )}
            </div>

            <div className="lab-item-actions">
                {item.canSaveResult && (
                    <button
                        type="button"
                        className="lab-item-edit-button"
                        onClick={onEdit}
                    >
                        {item.resultValue
                            ? "Editar Resultado"
                            : "Registrar Resultado"}
                    </button>
                )}

                {item.canPublishResult && (
                    <button
                        type="button"
                        className="lab-item-publish-button"
                        disabled={publishing}
                        onClick={onPublish}
                    >
                        {publishing
                            ? "Publicando..."
                            : "Publicar Resultado"}
                    </button>
                )}

                {item.published && (
                    <div className="lab-published-label">
                        Resultado publicado
                    </div>
                )}
            </div>
        </article>
    );
}

function ContextField({
    label,
    value
}) {
    return (
        <div>
            <span>{label}</span>
            <strong>{value}</strong>
        </div>
    );
}

function getProgress(order) {
    if (!order?.totalTests) {
        return 0;
    }

    return Math.round(
        (
            Number(
                order.publishedTests
            ) /
            Number(order.totalTests)
        ) * 100
    );
}

function formatStatus(status) {
    if (!status) {
        return "No disponible";
    }

    return status
        .replaceAll("_", " ");
}

function formatAmount(value) {
    return `Q${Number(
        value || 0
    ).toFixed(2)}`;
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

export default LaboratoryOrderDetail;